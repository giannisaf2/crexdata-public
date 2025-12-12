<?php

declare(strict_types=1);

class CrexdataNonRasterPackedDistributionApp extends Application
{
    use TimeSeriesTrait;

    /**
     * @throws EnvironmentException|Exception
     */
    public function run(): void
    {
        // Initialize helpers
        $kafka_helper = new KafkaHelper(
            KafkaHelper::crexdataKafkaCredentials(),
            use_sasl_auth: false,
        );

        $products = $this->loadProducts($this->config->input_products);

        $message_data = [];
        foreach ($products as $input_product) {
            Log::getInstance()->info("Serializing product ($input_product->variable, $input_product->type, $input_product->supplier)");
            $product_details = $input_product->details;

            if ($product_details->is_raster) {
                Log::getInstance()->severe("\tProduct is raster! Skipping...");
                continue;
            }

            $elements = ArgosDB::getInstance()->getElementsByIdProductWithoutGeometry($product_details->id_product, aggregation_key: "id_element");
            if (isset($this->config->element_codes)) {
                $elements = array_filter(
                    $elements,
                    fn(object $e) => in_array($e->code, $this->config->element_codes),
                );
            }
            if (empty($elements)) {
                Log::getInstance()->severe("\tProduct has no elements, skipping.");
            }

            $last_available = null;
            if ($product_details->data_type == ProductDataType::FORECAST) {
                $last_available = $product_details->last_simulation;
            } else {
                throw new Exception("Not implemented");
            }

            if ($last_available === null) {
                Log::getInstance()->warning("\tProduct data not found, skipping...");
                continue;
            }

            // Getting records
            if ($product_details->data_type == ProductDataType::FORECAST) {
                // It is assumed that the program executes with enough frequency that no simulations will be missed between the
                // last_shared and last_available.
                $records = ArgosDB::getInstance()->getRecords($product_details->id_product, $last_available);
            }

            $records_by_element = [];
            if ($product_details->data_type == ProductDataType::FORECAST) {
                foreach ($records as $record) {
                    if (array_key_exists($record->id_element, $elements)) {
                        $records_by_element[$record->id_element][] = $record;
                    }
                }
            }

            if (empty($records_by_element)) {
                Log::getInstance()->warning("No records found, skipping.");
                continue;
            } else {
                Log::getInstance()->info("Found " . self::countLeaves($records_by_element) . " records to distribute.");
            }

            // Serialize records
            Log::getInstance()->info("Serializing records...");

            $element_coords_by_id = ArgosDB::getInstance()->getElementsByIdProductOnlyLocation(
                $product_details->id_product,
                srid: 4326,
                aggregation_key: "id_element",
            );

            foreach ($records_by_element as $id_element => $elem_records) {
                $element = $elements[$id_element];
                $element->lat = $element_coords_by_id[$id_element]->y;
                $element->lon = $element_coords_by_id[$id_element]->x;

                if ($product_details->data_type == ProductDataType::OBSERVATION) {
                    throw new Exception("Unimplemented");
                } else {
                    $serialized_records = RecordSerializer::serialize(
                        $elem_records,
                        $element,
                        $product_details,
                        simulation: $last_available,
                        description: (isset($input_product->description))
                            ? $input_product->description
                            : "",
                    );
                    $message_data[$product_details->variable] = $serialized_records;
                }
            }

            // CREXDATA distribution
            if (isset($input_product->kafka_topic)) {
                $info[$product_details->id_product][$input_product->kafka_topic] = $last_available;
            } else {
                $info[$product_details->id_product] = $last_available;
            }
        }

        try {
            $kafka_helper->uploadMessage(
                json_encode($message_data),
                isset($input_product->kafka_topic) ? $input_product->kafka_topic : null,
            );
            Log::getInstance()->info("Succesfully uploaded message to Kafka.");
        } catch (Exception $e) {
            Log::getInstance()->severe("Failed to upload message to Kafka. Exception: " . $e->getMessage());
            return;
        }
    }

    private static function countLeaves(array $array): int
    {
        $count = 0;

        foreach ($array as $value) {
            if (is_array($value)) {
                $count += self::countLeaves($value);
            } else {
                $count++;
            }
        }

        return $count;
    }
}
