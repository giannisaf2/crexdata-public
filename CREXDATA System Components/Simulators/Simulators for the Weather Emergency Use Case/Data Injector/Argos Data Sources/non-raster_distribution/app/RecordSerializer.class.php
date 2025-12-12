<?php

declare(strict_types=1);

class RecordSerializer
{
    public const string RECORD_DATE_FORMAT = DateTimeInterface::ATOM;

    /**
     * Todo documentation.
     */
    public static function serialize(
        array $records,
        object $element,
        ProductDetail $product,
        int $simulation = 0,
        string $description = "",
        bool $add_metadata = true,
        int $precision = 2,
    ): array {
        $values_array = [];
        foreach ($records as $record) {
            $dt = new DateTimeImmutable("@$record->date");
            $values_array[$dt->format(self::RECORD_DATE_FORMAT)] = round($record->value, $precision);
        }

        if ($add_metadata) {
            $content = [
                "metadata" => [
                    "location_name" => $element->name,
                    "location_code" => $element->code,
                    "location_type" => $element->type,
                    "latitude" => $element->lat,
                    "longitude" => $element->lon,
                    "unit" => self::mapUnits($product->unit),
                    "time_step_minutes" => $product->time_step / 60,
                    "update_frequency_minutes" => $product->update_frequency / 60,
                    "description" => $description,
                ],
                "values" => $values_array,
            ];
            if ($product->data_type == ProductDataType::FORECAST) {
                $content["metadata"]["simulation_timestamp"] = $simulation;
            }
        } else {
            $content = [
                "values" => $values_array,
            ];
        }

        return $content;
    }

    /**
     * Todo documentation.
     */
    public static function serializeWarnings(
        array $records,
        object $element,
        ProductDetail $product,
        string $description = "",
        bool $add_metadata = true,
    ): array {
        $values_array = [];
        foreach ($records as $record) {
            $dt = new DateTimeImmutable("@$record->date");
            $values_array[$dt->format(self::RECORD_DATE_FORMAT)] = $record->value;
        }

        if ($add_metadata) {
            $content = [
                "metadata" => [
                    "name" => $element->name,
                    "code" => $element->code,
                    "type" => $element->type,
                    "unit" => self::mapUnits($product->unit),
                    "time_step_minutes" => $product->time_step / 60,
                    "update_frequency_minutes" => $product->update_frequency / 60,
                    "description" => $description,
                ],
                "values" => $values_array,
            ];
        } else {
            $content = [
                "values" => $values_array,
            ];
        }

        return $content;
    }

    private static function mapUnits(string $unit): string
    {
        return match ($unit) {
            "º" => "degrees",
            default => $unit,
        };
    }
}
