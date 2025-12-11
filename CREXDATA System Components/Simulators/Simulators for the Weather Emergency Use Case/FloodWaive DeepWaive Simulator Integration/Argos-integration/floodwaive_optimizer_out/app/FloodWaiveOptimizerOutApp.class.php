<?php

declare(strict_types=1);

class FloodWaiveOptimizerOutApp extends Application
{
    use RasterFileTrait;
    use TimeSeriesTrait;

    public function run(): void
    {
        $kafka_consumer = new KafkaConsumer(
            EnvironmentHelper::crexdataKafkaCredentials(no_auth: true),
            enable_auto_commit: true,
        );

        if (isset($this->config->kafka->topic) && $this->config->kafka->topic !== "") {
            $kafka_consumer->setTopic($this->config->kafka->topic);
        }

        while (true) {
            // Consume messages with a timeout
            Log::getInstance()->debug("Waiting for messages...");
            try {
                $message = $kafka_consumer->waitMessage(60_000);
            } catch (Exception $ex) {
                Log::getInstance()->severe($ex->getMessage());
                continue;
            }
            if ($message === null) {
                continue;
            }

            try {
                $json = FloodWaiveOptimizerMessage::fromJson($message->payload);
            } catch (Exception $ex) {
                Log::getInstance()->severe("Failed to decode kafka message payload. Exception: " . $ex->getMessage());
                continue;
            }

            // New message received and succesfully decoded, so we process it
            $this->process(
                $json->optimizer_request_id,
                $json->simulation_id,
                $json->barriers,
            );
        }
    }

    /**
     * @throws Exception
     */
    public function process(string $simulation_id, string $optimizer_id, array $barriers): void
    {
        // Init helpers
        $repository_helper = new RepositoryHelper(EnvironmentHelper::dataRepositoryPath());
        $fw_client = new FloodWaiveApiClient(EnvironmentHelper::floodWaiveApiKey());

        // Get simulation details
        $fw_simulation = $fw_client->getSimulation($simulation_id);
        if ($fw_simulation === null) {
            Log::getInstance()->warning("Simulation (FloodWaive API) does not exist.");
            return;
        }

        $simulation_t0 = $fw_simulation->getCreationTime()->getTimestamp();
        $simulation_t0 = ((int)round($simulation_t0 / 300)) * 300;

        $available_timesteps = $fw_simulation->getTimesteps();
        if (empty($available_timesteps)) {
            Log::getInstance()->warning("Simulation '$simulation_id' does not have any timesteps available!");
            return;
        }

        // Process barriers
        if (empty($barriers)) {
            Log::getInstance()->warning("No barriers received, exporting simulation without optimal heights.");
        } else {
            $barriers_height = [];
            foreach ($barriers as $barrier) {
                $barriers_height[$barrier["name"]] = $barrier["height"];
            }

            ArgosDB::getInstance()->deleteBarriers($optimizer_id);
            ArgosDB::getInstance()->updateBarriersHeight(
                $optimizer_id,
                $simulation_id,
                $simulation_t0,
                $barriers_height,
            );
        }

        // Download simulation
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
