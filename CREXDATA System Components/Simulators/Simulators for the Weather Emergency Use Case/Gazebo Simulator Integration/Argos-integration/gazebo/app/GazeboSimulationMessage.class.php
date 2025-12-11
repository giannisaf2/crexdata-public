<?php

class GazeboSimulationMessage extends KafkaMessage
{
    public string $criterion;
    public array $flights;

    protected static function fromArray(array $data): static
    {
        $msg = new static();

        $msg->criterion = trim((string)(static::requireKey($data, "optimizationCriterion")));
        $msg->flights = static::requireKey($data, "flights");

        return $msg;
    }
}
