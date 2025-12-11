<?php

class KafkaConnectionParams
{
    public function __construct(
        public string $brokers,
        public string $username,
        public string $password,
        public string $ssl_dir,
        public string $topic,
        public string $consumer_group,
    ) {}
}
