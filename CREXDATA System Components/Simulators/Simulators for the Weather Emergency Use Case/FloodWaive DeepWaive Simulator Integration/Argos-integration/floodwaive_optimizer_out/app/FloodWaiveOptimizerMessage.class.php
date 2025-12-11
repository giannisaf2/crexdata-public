<?php

class FloodWaiveOptimizerMessage extends KafkaMessage
{
    public string $optimizer_request_id;
    public string $simulation_id;
    public array $barriers;

    protected static function fromArray(array $data): static
    {
        $msg = new static();

        $msg->optimizer_request_id = static::requireKey($data, "optimizerRequestID");
        $msg->simulation_id = static::requireKey($data, "floodwaiveSimulationID");
        $msg->optimizer_request_id = $data["optimizedBarrierHeights"] ?? [];

        return $msg;
    }
}
