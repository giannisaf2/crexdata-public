<?php

use GuzzleHttp\RequestOptions;

class FloodWaiveApiClient extends ApiClient
{
    private const string BASE_URL = "https://api.floodwaive.de/v1";

    public function __construct(
        private readonly string $api_key
    ) {
        parent::__construct();

        $this->addDefaultRetryPolicy(ApiClient::DEFAULT_MAX_RETRIES);
    }

    public function getSimulation(string $simulation_id): ?FloodWaiveSimulation
    {
        $endpoint_uri = self::BASE_URL . "/simulations/$simulation_id";

        $request_options = $this->sharedRequestOptions();

        try {
            $response = $this->makeRequest($endpoint_uri, "GET", $request_options);
            return new FloodWaiveSimulation(
                $this->formatJsonResponseData($response["body"], as_associative: true)
            );
        } catch (ApiClientException $ex) {
            Log::getInstance()->severe($ex->getMessage());
        }

        return null;
    }

    /**
     * If succesful will return the export ID for keeping track of the operation progress.
     *
     * @param array|int $timesteps List of timestep indices to export. Use -1 for maximum water levels.
     */
    public function requestGeotiffExport(string $simulation_id, array $timesteps = [-1]): ?string
    {
        $endpoint_uri = self::BASE_URL . "/simulations/$simulation_id/export-geotiff";

        $body = [
            "timesteps" => $timesteps,
            "data_type" => "depth",
            "scope" => "full_area",
        ];
        $body = json_encode($body);

        $request_options = $this->sharedRequestOptions();
        $request_options[RequestOptions::BODY] = $body;

        try {
            $response = $this->makeRequest($endpoint_uri, "POST", $request_options);
            Log::getInstance()->debug("Succesfully requested geotiff export of simulation '$simulation_id'");
            $json = $this->formatJsonResponseData($response["body"], as_associative: true);
            return $json["export_id"];
        } catch (ApiClientException $ex) {
            Log::getInstance()->severe("Geotiff export request for simulation '$simulation_id' failed. Message: " . $ex->getMessage());
        }

        return null;
    }

    /**
     * @param array<int> $timesteps
     */
    public function downloadSimulationTimeseries(FloodWaiveSimulation $simulation_data, string $output_dir, int $timeout_seconds = 300): bool
    {
        $timer = new Timer();
        $timer->start();

        $simulation_id = $simulation_data->getID();
        $timesteps = $simulation_data->getTimesteps();

        // NOTE: Using the closest mulitple of 5 min timestamp as the simulation's first instant.
        $simulation_t0 = $simulation_data->getCreationTime()->getTimestamp();
        $simulation_t0 = round($simulation_t0 / 300) * 300;

        Log::getInstance()->debug("Requested timesteps [" . implode(", ", $timesteps) . "].");
        $export_id = $this->requestGeotiffExport($simulation_id, $timesteps);
        if ($export_id === null) {
            return false;
        }

        $endpoint_uri = self::BASE_URL . "/exports/$export_id";

        $request_options = $this->sharedRequestOptions();

        $finished = false;
        while (!$finished && ($timer->elapsed() < $timeout_seconds)) {
            try {
                $response = $this->makeRequest($endpoint_uri, "GET", $request_options);
                $json = $this->formatJsonResponseData($response["body"], as_associative: true);
                $status = $json["status"];

                if ($status === "completed") {
                    Log::getInstance()->info("Export job finished. Status: '$status'");
                    $finished = true;
                } else if ($status === "failed") {
                    Log::getInstance()->warning("Export job finished. Status: '$status'. Message: " . $json["error_message"]);
                    $finished = true;
                } else if ($json["status"] === "processing") {
                    Log::getInstance()->info("Export progress at " . $json["progress_percent"] . "%");
                    // Wait before polling for export progress again
                    sleep(3);
                } else {
                    Log::getInstance()->severe("Stopping because of unknown export status: $status");
                    print_r($json);
                    return false;
                }
            } catch (ApiClientException $ex) {
                Log::getInstance()->severe($ex->getMessage());
                return false;
            }
        }

        if (!$finished) {
            Log::getInstance()->warning("Operation timed out! [Sim: $simulation_id, Exp: $export_id]");
            return false;
        }

        $file_count = $json["file_count"];
        if ($file_count <= 0) {
            Log::getInstance()->warning("Not enough files available!");
            return false;
        }

        Log::getInstance()->info("Downloading $file_count files.");
        $src_dir = $json["filesystem_path"];

        foreach ($json["file_progress"] as $file_info) {
            if ($file_info["status"] === "completed") {
                $src_filename = $file_info["filename"];
                $src_path = "$src_dir/$src_filename";

                $file_timestamp = $simulation_t0 + $file_info["timestep"];
                $dst_filename = date("Ymd\THis", $file_timestamp);
                $dst_path = "$output_dir/$dst_filename." . pathinfo($src_filename, PATHINFO_EXTENSION);

                try {
                    $this->downloadFileFromOrganization($src_path, $dst_path);
                } catch (Exception $ex) {
                    Log::getInstance()->severe("Failed to download file $src_path. Ex: " . $ex->getMessage());
                    continue;
                }
            }
        }

        return true;
    }

    /**
     * @param string $src_path Absolute path to the file in Floodwaive's filesystem.
     * @param string $dst_path Absolute path to where the file should be downloaded.
     */
    public function downloadFileFromOrganization(string $src_path, string $dst_path)
    {
        $src_path = trim($src_path, "/");
        $endpoint_uri = self::BASE_URL . "/filesystem/download/$src_path";

        $request_options = $this->sharedRequestOptions();

        $response = $this->makeRequest($endpoint_uri, "GET", $request_options);
        $json = $this->formatJsonResponseData($response["body"], as_associative: true);
        $download_link = $json["download_url"];

        $this->makeRequest($download_link, "GET", [RequestOptions::SINK => $dst_path]);
    }

    public function getAreaSimulations(string $area_id): ?array
    {
        $endpoint_uri = self::BASE_URL . "/areas/$area_id/simulations";

        // Adding api key
        $request_options = $this->sharedRequestOptions();

        // Fetch data
        $response = $this->makeRequest($endpoint_uri, "GET", $request_options);
        return $this->formatJsonResponseData($response["body"], as_associative: true);
    }

    /**
     * Shared request options
     *
     * @return string[][]
     */
    private function sharedRequestOptions(): array
    {
        return [
            RequestOptions::HEADERS => [
                "Authorization" => "Bearer " . $this->api_key,
            ],
        ];
    }
}
