<?php

declare(strict_types=1);

class GazeboSimulationApp extends Application
{
    public function run(): void
    {
        $kafka_consumer = new KafkaConsumer(
            EnvironmentHelper::crexdataKafkaCredentials(no_auth: true),
            enable_auto_commit: true,
        );

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
                $message_dto = GazeboSimulationMessage::fromJson($message->payload);
            } catch (Exception $ex) {
                Log::getInstance()->severe("Exception raised while parsing message payload. | Reason: " . $ex->getMessage());
                continue;
            }

            // var_dump($message_dto);
            // print_r($message_dto);
            // print(PHP_EOL);
            // continue;

            // New message received and succesfully decoded, so we process it
            $this->processFlights($message_dto->flights, $message_dto->criterion);
        }
    }

    private function processFlights(array $flights, string $criterion): void
    {
        foreach ($this->config->products as $prod_conf) {
            // Product details
            $product = ArgosDB::getInstance()->getProductDetails(
                $prod_conf->variable,
                $prod_conf->type,
                $prod_conf->supplier,
            );
            if ($product === null) {
                Log::getInstance()->warning("Failed to obtain information for product ({$prod_conf->variable}), skipping.");
                continue;
            }

            // Flights
            $routes = [];

            foreach ($flights as $flight_info) {
                $waypoint_objs = self::extractWaypoints($flight_info["Waypoints"]);
                if (!empty($waypoint_objs)) {
                    $routes[] = $waypoint_objs;
                }
            }

            if (empty($routes)) {
                return;
            }

            $domain = $this->config->general->domain;
            Log::getInstance()->info("Removing previous routes in '$domain'...");
            ArgosDB::getInstance()->deleteRoutesByDomain($domain, $product->id_product);

            Log::getInstance()->info("Inserting new routes...");
            foreach ($routes as $rank => $waypoints) {
                $id_route = ArgosDB::getInstance()->createNewRouteWithWaypoints($domain, $product->id_product, $waypoints);
                ArgosDB::getInstance()->addRouteRanking($id_route, $criterion, $rank);
            }
        }
    }

    private static function extractWaypoints(array $waypoints): array
    {
        $waypoint_objs = [];

        foreach ($waypoints as $wp_index => $waypoint) {
            $obj = new stdClass();
            $obj->waypoint_index = (int)$wp_index;
            $obj->tag = $waypoint["event"];
            $obj->longitude = (float)$waypoint["longitude"];
            $obj->latitude = (float)$waypoint["latitude"];
            $obj->altitude = (float)$waypoint["altitude"];
            $obj->time_instant = round((float)$waypoint["timestamp"], 2);
            $obj->battery = (float)$waypoint["battery"];

            $waypoint_objs[] = $obj;
        }

        return $waypoint_objs;
    }
}
