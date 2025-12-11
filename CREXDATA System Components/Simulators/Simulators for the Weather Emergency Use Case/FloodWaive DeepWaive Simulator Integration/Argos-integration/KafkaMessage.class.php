<?php

abstract class KafkaMessage
{
    abstract protected static function fromArray(array $data): static;

    public static function fromJson(string $json_str): static
    {
        $content = json_decode($json_str, true, flags: JSON_THROW_ON_ERROR);
        return static::fromArray($content);
    }

    /**
     * Helper function to enforce required keys for all subclasses.
     * @throws InvalidArgumentException When the given key is not present in the array.
     */
    protected static function requireKey(array $data, string $key): mixed
    {
        if (!array_key_exists($key, $data)) {
            throw new InvalidArgumentException("Missing required key '$key' on construction of class '" . static::class . "'");
        }
        return $data[$key];
    }
}
