<?php

enum KafkaAuthEnv: string
{
    case CA = "KAFKA_CA";
    case CLIENT_CERTIFICATE = "KAFKA_CLIENT_CERTIFICATE";
    case CLIENT_KEY = "KAFKA_CLIENT_KEY";
}

class KafkaConsumer
{
    private ?RdKafka\KafkaConsumer $consumer = null;
    private RdKafka\Conf $kafka_config;

    /**
     * @param string $brokers Allows to override the broker list value specified in `$conn_params`.
     */
    public function __construct(
        KafkaConnectionParams $conn_params,
        ?string $brokers = null,
        bool $use_sasl_auth = false,
        bool $enable_auto_commit = true,
    ) {
        $this->kafka_config = new RdKafka\Conf();
        $this->kafka_config->set('metadata.broker.list', $brokers ?? $conn_params->brokers);

        if ($use_sasl_auth) {
            $this->kafka_config->set('security.protocol', "SASL_SSL");
            $this->kafka_config->set('sasl.mechanism', "PLAIN");
            $this->kafka_config->set('sasl.username', $conn_params->username);
            $this->kafka_config->set('sasl.password', $conn_params->password);

            $ssl_auth_files = self::generateKafkaCertificateFiles("/config");

            $this->kafka_config->set('ssl.ca.location', $ssl_auth_files[KafkaAuthEnv::CA]);
            $this->kafka_config->set('ssl.certificate.location', $ssl_auth_files[KafkaAuthEnv::CLIENT_CERTIFICATE]);
            $this->kafka_config->set('ssl.key.location', $ssl_auth_files[KafkaAuthEnv::CLIENT_KEY]);
        }

        $this->kafka_config->set('group.id', $conn_params->consumer_group);
        $this->kafka_config->set('enable.partition.eof', 'true');
        $this->kafka_config->set('enable.auto.commit', $enable_auto_commit ? 'true' : 'false');
        $this->kafka_config->set('auto.offset.reset', 'earliest');

        if ($conn_params->topic === "") {
            Log::getInstance()->warning("Kafka topic environment value is empty.");
        }

        Log::getInstance()->info("Kafka consumer configured to read from topic '{$conn_params->topic}' for brokers [" . ($brokers ?? $conn_params->brokers) . "]");
        Log::getInstance()->info("Using group ID: {$conn_params->consumer_group}");
        $this->consumer = new RdKafka\KafkaConsumer($this->kafka_config);
        $this->consumer->subscribe([$conn_params->topic]);
    }

    public function setTopic(string $topic): void
    {
        $this->consumer->subscribe([$topic]);
        Log::getInstance()->info("Subscribed to topic '$topic'.");
    }

    /**
     * If no messages are received after the specified `$timeout_ms` the function returns `null`.
     */
    public function waitMessage(int $timeout_ms = 30_000): ?\RdKafka\Message
    {
        $message = $this->consumer->consume($timeout_ms);

        if ($message->err === RD_KAFKA_RESP_ERR__PARTITION_EOF) {
            Log::getInstance()->debug("Reached end of partition, no more messages.");
            return null;
        }
        if ($message->err === RD_KAFKA_RESP_ERR__TIMED_OUT) {
            Log::getInstance()->debug("Timed out waiting for messages.");
            return null;
        }
        if ($message->err !== RD_KAFKA_RESP_ERR_NO_ERROR) {
            throw new Exception($message->errstr(), $message->err);
        }

        Log::getInstance()->debug("New message received at {$message->timestamp}");
        return $message;
    }

    public function commitMessage(\RdKafka\Message $message): void
    {
        $this->consumer->commit($message);
    }

    private static function generateKafkaCertificateFiles(string $output_dir): array
    {
        $certificate_files = [];

        if (!is_dir($output_dir)) {
            mkdir($output_dir, 0600, recursive: true);
        }

        foreach (KafkaAuthEnv::cases() as $env_variable) {
            $variable_name = $env_variable->value;
            $content = getenv($variable_name);
            if ($content === false) {
                throw new Exception("Failed to read env variable '$variable_name'");
            }

            $file = tempnam($output_dir, $variable_name);
            if ($file === false || !file_put_contents($file, $content)) {
                throw new Exception("Failed to store contents of env variable '$variable_name' into a file");
            }

            $certificate_files[$env_variable] = $file;
        }

        return $certificate_files;
    }
}
