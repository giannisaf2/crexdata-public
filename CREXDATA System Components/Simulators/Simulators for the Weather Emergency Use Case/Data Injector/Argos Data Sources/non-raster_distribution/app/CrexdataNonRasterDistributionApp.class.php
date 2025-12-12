<?php

declare(strict_types=1);

class CrexdataNonRasterDistributionApp extends Application
{
    /**
     * @throws EnvironmentException|Exception
     */
    public function run(): void
    {
        $info_file = $this->config->general->distribution_info_folder . DIRECTORY_SEPARATOR . basename($this->config->general->log_file, ".log") . ".json";

        if (!is_dir($this->config->general->distribution_info_folder)) {
            mkdir($this->config->general->distribution_info_folder, 0755, true);
        }

        // Info of previously distributed files
        $info = [];
        if (is_file($info_file)) {
            $info = json_decode(file_get_contents($info_file), true);
        }

        // Initialize helpers
        $kafka_helper = new KafkaHelper(
            KafkaHelper::crexdataKafkaCredentials(),
            use_sasl_auth: false,
        );

        foreach ($this->config->input_products as $input_product) {
            Log::getInstance()->info("Distributing product ($input_product->variable, $input_product->type, $input_product->supplier)");
            // Retrieve additional information from DB
            $product_details = ArgosDB::getInstance()->getProductDetails(
                $input_product->variable,
                $input_product->type,
                $input_product->supplier
            );

            if ($product_details === null) {
                Log::getInstance()->severe("\tProduct information not found in DB, skipping...");
                continue;
            }

            if ($product_details->is_raster) {
                Log::getInstance()->severe("\tProduct is raster! Skipping...");
                continue;
            }

            if ($product_details->max_age < 86400) {
                Log::getInstance()->severe("\tMax age is smaller than 1 day, some data will be lost in the serialization process!");
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
            } else if ($product_details->data_type == ProductDataType::OBSERVATION) {
                $last_available = $product_details->last_observation;
            } else {
                throw new Exception("CrexdataNonRasterDistributionApp not implemented for OBSERVATION_AND_FORECAST products.");
            }

            if ($last_available === null) {
                Log::getInstance()->warning("\tProduct data not found, skipping...");
                continue;
            }

            $last_shared = null;
            if (isset($info[$product_details->id_product])) {
                $last_shared = (isset($input_product->kafka_topic))
                    ? $info[$product_details->id_product][$input_product->kafka_topic]
                    : $info[$product_details->id_product];
            }

            if ($last_shared === $last_available) {
                Log::getInstance()->info("Already distributed. Nothing to be done.");
                continue;
            }

            // Getting records
            if ($product_details->data_type == ProductDataType::FORECAST) {
                // It is assumed that the program executes with enough frequency that no simulations will be missed between the
                // last_shared and last_available.
                $records = ArgosDB::getInstance()->getRecords($product_details->id_product, $last_available);
            } else {
                $from = $last_shared ?? $last_available;
                $from = ((int)($from / 86400)) * 86400;

                $records = ArgosDB::getInstance()->getRecordsByDate(
                    $product_details->id_product,
                    from: $from,
                    to: $last_available,
                );
            }

            if (empty($records)) {
                continue;
            }

            $records_by_element = [];
            if ($product_details->data_type == ProductDataType::FORECAST) {
                foreach ($records as $record) {
                    if (array_key_exists($record->id_element, $elements)) {
                        $records_by_element[$record->id_element][] = $record;
                    }
                }
            } else {
                foreach ($records as $record) {
                    if (array_key_exists($record->id_element, $elements)) {
                        $day_timestamp = ((int)($record->date / 86400)) * 86400;
                        $records_by_element[$record->id_element][$day_timestamp][] = $record;
                    }
                }
            }

            // Serialize records and upload them to kafka
            Log::getInstance()->info("Serializing records...");

            if (isset($this->config->general->is_warning) && $this->config->general->is_warning) {
                // NOTE: Element geometry is not included.
                foreach ($records_by_element as $id_element => $records) {
                    $element = $elements[$id_element];

                    $serialized_records = RecordSerializer::serializeWarnings(
                        $records,
                        $element,
                        $product_details,
                        description: (isset($input_product->description))
                            ? $input_product->description
                            : "",
                    );

                    try {
                        $kafka_helper->uploadMessage(
                            json_encode($serialized_records),
                            isset($input_product->kafka_topic) ? $input_product->kafka_topic : null
                        );
                    } catch (Exception $e) {
                        Log::getInstance()->severe("Failed to upload records to kafka. Exception: " . $e->getMessage());
                    }
                }
            } else {
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
                        foreach ($elem_records as $day_timestamp => $day_records) {
                            $serialized_records = RecordSerializer::serialize(
                                $day_records,
                                $element,
                                $product_details,
                                description: (isset($input_product->description))
                                    ? $input_product->description
                                    : "",
                            );

                            try {
                                $kafka_helper->uploadMessage(
                                    json_encode($serialized_records),
                                    isset($input_product->kafka_topic) ? $input_product->kafka_topic : null,
                                );
                            } catch (Exception $e) {
                                Log::getInstance()->severe("Failed to upload records to kafka. Exception: " . $e->getMessage());
                            }
                        }
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

                        try {
                            $kafka_helper->uploadMessage(
                                json_encode($serialized_records),
                                isset($input_product->kafka_topic) ? $input_product->kafka_topic : null,
                            );
                        } catch (Exception $e) {
                            Log::getInstance()->severe("Failed to upload records to kafka. Exception: " . $e->getMessage());
                        }
                    }
                }
            }

            // CREXDATA distribution
            if (isset($input_product->kafka_topic)) {
                $info[$product_details->id_product][$input_product->kafka_topic] = $last_available;
            } else {
                $info[$product_details->id_product] = $last_available;
            }
        }

        if (!file_put_contents($info_file, json_encode($info))) {
            Log::getInstance()->severe("Failed storing last processed date into " . basename($info_file));
        }
    }
}
