<?php

class KafkaHelper
{
    private RdKafka\Conf $kafka_config;
    private ?RdKafka\Producer $producer = null;
    private string $topic;

    /**
     * @param string $brokers Allows to override the broker list value specified in `$conn_params`.
     */
    public function __construct(KafkaConnectionParams $conn_params, ?string $brokers = null, bool $use_sasl_auth = false)
    {
        $this->kafka_config = new RdKafka\Conf();
        $this->kafka_config->set('metadata.broker.list', $brokers ?? $conn_params->brokers);

        if ($use_sasl_auth) {
            $this->kafka_config->set('security.protocol', "SASL_SSL");
            $this->kafka_config->set('sasl.mechanism', "PLAIN");
            $this->kafka_config->set('sasl.username', $conn_params->username);
            $this->kafka_config->set('sasl.password', $conn_params->password);

            $ssl_auth_files = self::generateKafkaCertificateFiles("/config");

            $this->kafka_config->set('ssl.ca.location', $ssl_auth_files["KAFKA_CA"]);
            $this->kafka_config->set('ssl.certificate.location', $ssl_auth_files["KAFKA_CLIENT_CERTIFICATE"]);
            $this->kafka_config->set('ssl.key.location', $ssl_auth_files["KAFKA_CLIENT_KEY"]);
        }

        $this->topic = $conn_params->topic;
        if ($this->topic === "") {
            Log::getInstance()->warning("Kafka topic environment value is empty.");
        }

        Log::getInstance()->info("Kafka helper configured to write to topic '{$this->topic}' for brokers [" . ($brokers ?? $conn_params->brokers) . "]");
    }

    public function __destruct()
    {
        // Deliver any remaining buffered messages
        if ($this->producer !== null) {
            for ($flushRetries = 0; $flushRetries < 3; $flushRetries++) {
                $result = $this->producer->flush(10_000);
                if ($result === RD_KAFKA_RESP_ERR_NO_ERROR) {
                    return;
                }
            }
            if ($result !== RD_KAFKA_RESP_ERR_NO_ERROR) {
                Log::getInstance()->severe("Kafka producer flush failed - messages might be lost.");
            }
        }
    }

    /**
     * @param ?string $topic Allows to override the currently targeted topic.
     * @throws RuntimeException
     */
    public function uploadMessage(string $message, ?string $topic = null): void
    {
        if ($this->producer === null) {
            // Set up callbacks
            // $this->kafka_config->setDrMsgCb(function (RdKafka\Producer $kafka, RdKafka\Message $message) {
            //     if ($message->err) {
            //         Log::getInstance()->severe("Delivery failed: " . rd_kafka_err2str($message->err));
            //     } else {
            //         Log::getInstance()->debug("Delivered to {$message->topic_name} [{$message->partition}] @ offset {$message->offset}");
            //     }
            // });

            // Initialize producer
            $this->producer = new RdKafka\Producer($this->kafka_config);
        }

        $target_topic = $topic ?? $this->topic;
        if ($topic !== null) {
            // Only notify when the target topic is overriden by the json config
            Log::getInstance()->info("Writing to topic '$target_topic'.");
        }

        // Get handle on existing topic or create a new one
        $topic = $this->producer->newTopic($target_topic);

        // Produce message and poll to allow internal processing and callbacks execution
        // Log::getInstance()->debug("Sending message: $message");
        $topic->produce(RD_KAFKA_PARTITION_UA, 0, $message);
        $this->producer->poll(100);
    }

    /**
     * @throws EnvironmentException
     */
    public static function crexdataKafkaCredentials(): KafkaConnectionParams
    {
        $brokers = getenv("KAFKA_BROKER_LIST_NO_AUTH");
        $username = "not_used"; //getenv("KAFKA_AUTH_USERNAME");
        $password = "not_used"; //getenv("KAFKA_AUTH_PASSWORD");
        $topic = getenv("KAFKA_TOPIC");

        if ($brokers === false || $username === false || $password === false || $topic === false) {
            throw new Exception("Environment variables for Kafka connection not set.");
        }

        return new KafkaConnectionParams(
            $brokers,
            $username,
            $password,
            $topic,
        );
    }

    private static function generateKafkaCertificateFiles(string $output_dir): array
    {
        $certificate_files = [];
        $certificate_env_vars = [
            "KAFKA_CA",
            "KAFKA_CLIENT_CERTIFICATE",
            "KAFKA_CLIENT_KEY",
        ];

        if (!is_dir($output_dir)) {
            mkdir($output_dir, 0600, recursive: true);
        }

        foreach ($certificate_env_vars as $env_variable) {
            $content = getenv($env_variable);
            if ($content === false) {
                throw new Exception("Failed to read env variable '$env_variable'");
            }

            $file = tempnam($output_dir, $env_variable);
            if ($file === false || !file_put_contents($file, $content)) {
                throw new Exception("Failed to store contents of env variable '$env_variable' into a file");
            }

            $certificate_files[$env_variable] = $file;
        }

        return $certificate_files;
    }
}
