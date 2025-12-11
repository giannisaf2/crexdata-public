<?php

declare(strict_types=1);

class FloodWaiveApp extends Application
{
    use RasterFileTrait;
    use TimeSeriesTrait;

    public function run(): void
    {
        $kafka_consumer = new KafkaConsumer(
            EnvironmentHelper::crexdataKafkaCredentials(no_auth: true),
        );

        if (isset($this->config->kafka->topic) && $this->config->kafka->topic !== "") {
            $kafka_consumer->setTopic($this->config->kafka->topic);
        }

        while (true) {
            // Consume messages with a timeout
            Log::getInstance()->debug("Waiting for messages...");
            $message = $kafka_consumer->waitMessage(timeout_ms: 30_000);
            if ($message === null) {
                continue;
            }

            try {
                $obj = self::decodeJson($message->payload);
            } catch (Exception $ex) {
                Log::getInstance()->severe("Failed to decode kafka message payload. Exception: " . $ex->getMessage());
                continue;
            }

            if (!isset($obj->simulation_id)) {
                Log::getInstance()->warning("Received message does not have the expected format, ignoring.");
                $kafka_consumer->commitMessage($message);
                continue;
            }

            // New message received and succesfully decoded, so we process it
            $simulation_id = $obj->simulation_id;
            var_dump($obj);
            continue;

            $this->process($simulation_id);

            // Mark message as consumed
            $kafka_consumer->commitMessage($message);
        }
    }

    private static function decodeJson(string $json_string, bool $associative = false)
    {
        $content = json_decode($json_string, associative: $associative);

        switch (json_last_error()) {
            case JSON_ERROR_NONE:
                break;
            case JSON_ERROR_DEPTH:
                throw new JSONFileException('ERROR decoding json: Maximum stack depth exceeded');
            case JSON_ERROR_STATE_MISMATCH:
                throw new JSONFileException('ERROR decoding json: State mismatch (invalid or malformed JSON)');
            case JSON_ERROR_CTRL_CHAR:
                throw new JSONFileException('ERROR decoding json: Control character error, possibly incorrectly encoded');
            case JSON_ERROR_SYNTAX:
                throw new JSONFileException('ERROR decoding json: Syntax error');
            case JSON_ERROR_UTF8:
                throw new JSONFileException('ERROR decoding json: Malformed UTF-8 characters, possibly incorrectly encoded');
        }

        return $content;
    }

    /**
     * @throws Exception
     */
    public function process(string $simulation_id): void
    {
        // Init helpers
        $repository_helper = new RepositoryHelper(EnvironmentHelper::dataRepositoryPath());
        $fw_client = new FloodWaiveApiClient(EnvironmentHelper::floodWaiveApiKey());

        // Get simulation details
        $fw_simulation = $fw_client->getSimulation($simulation_id);
        if ($fw_simulation === null) {
            return;
        }

        $simulation_t0 = $fw_simulation->getCreationTime()->getTimestamp();
        $simulation_t0 = ((int)round($simulation_t0 / 300)) * 300;

        $available_timesteps = $fw_simulation->getTimesteps();
        if (empty($available_timesteps)) {
            Log::getInstance()->warning("Simulation '$simulation_id' does not have any timesteps available!");
            return;
        }

        $output_dir = FileSystemUtils::createTempDir(static::class . "_" . $fw_simulation->getID());

        Log::getInstance()->info("Downloading simulation...");
        $success = $fw_client->downloadSimulationTimeseries($fw_simulation, $output_dir);
        if (!$success) {
            Log::getInstance()->severe("Failed to download simulation timeseries!");
            return;
        }

        try {
            // Process
            foreach ($this->config->products as $prod_conf) {
                Log::getInstance()->info("Processing ($prod_conf->variable) ...");
                $product_details = $this->loadProductDetails($prod_conf);
                if ($product_details === null) {
                    continue;
                }

                // if ($simulation_t0 <= $product_details->last_simulation) {
                //     Log::getInstance()->info("\tAlready processed");
                //     continue;
                // }

                $simulation_files = glob($output_dir . "/*.tif");
                if (empty($simulation_files)) {
                    Log::getInstance()->warning("No data found.");
                    continue;
                }
                foreach ($simulation_files as $tif_file) {
                    $this->reprojectTo3857($tif_file);
                }

                // Store in repository and generate raster records
                Log::getInstance()->info("\tStoring result in repository...");
                $date_extraction_pattern = $this->config->inputs->date_extraction_pattern;
                $records = $repository_helper->store(
                    $output_dir,
                    $date_extraction_pattern,
                    $simulation_t0,
                    $product_details,
                );
                if (empty($records)) {
                    Log::getInstance()->severe("\tNo files stored in repository.");
                    continue;
                }

                // Update raster records in DB
                Log::getInstance()->info("Updating raster records...");
                $this->updateRasterRecords($records, $product_details);

                $prod_vector_tile_info = ArgosDB::getInstance()->getVectorTileInfo($product_details->id_product);
                if ($prod_vector_tile_info === null) {
                    Log::getInstance()->warning("\t* Vector Tiles info not found, skiping tile generation.");
                } else if ($prod_vector_tile_info->debug) {
                    Log::getInstance()->info("\t* Product has debug enabled, skiping tile generation.");
                } else {
                    Log::getInstance()->info("\t* Requesting Vector Tiles generation...");
                    if (!MessagePublisher::requestRasterRecords2VectorTiles($records, $prod_vector_tile_info->generation_priority)) {
                        Log::getInstance()->severe("\t* Request failed. Vector Tiles will not be generated!");
                    }
                }

                // Update product last simulation
                Log::getInstance()->info("Updating product last simulation");
                ArgosDB::getInstance()->updateLastSimulation($product_details->id_product, $simulation_t0);
            }
        } finally {
            FileSystemUtils::removeDirectoryRecursive($output_dir);
        }

        Log::getInstance()->info("Finished.");
        // if ($success) {
        //     Log::getInstance()->info("Updating data availability...");
        //     $data_availability_entry = $this->config->monitoring->data_availability_name ?? "";
        //     DataAvailabilityHelper::updateLastInstant($data_availability_entry, $simulation_t0);
        // }
    }

    /**
     * Reprojects a GeoTIFF form EPSG:25832 to EPSG:3857 overwriting the original file.
     */
    private function reprojectTo3857(string $tif_file): void
    {
        $tmp_file = $tif_file . ".reproj.tif";

        $cmd = sprintf(
            "gdalwarp -t_srs EPSG:3857 -r bilinear %s %s",
            escapeshellarg($tif_file),
            escapeshellarg($tmp_file)
        );

        $result = ExternalProcessHelper::execute($cmd);
        if ($result["status_code"] !== 0) {
            Log::getInstance()->warning("Reprojection failed for [$tif_file]: " . $result["output"]);
            return;
        }

        rename($tmp_file, $tif_file);
        Log::getInstance()->info("\tReprojected $tif_file to EPSG:3857");
    }
}
