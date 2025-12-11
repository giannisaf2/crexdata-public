<?php

class FloodWaiveSimulation
{
    private string $simulation_id;
    private string $area_id;
    private string $created_at;
    private array $timesteps;

    public function __construct(array $api_response_data)
    {
        $this->simulation_id = $api_response_data["simulation_id"];
        $this->area_id = $api_response_data["area_id"];
        $this->timesteps = $api_response_data["progress"]["available_timesteps"];
        $this->created_at = $api_response_data["created_at"];

    }

    public function getID(): string
    {
        return $this->simulation_id;
    }

    public function getAreaID(): string
    {
        return $this->area_id;
    }

    public function getTimesteps(): array
    {
        return $this->timesteps;
    }

    public function getCreationTime(): DateTime
    {
        return new DateTime($this->created_at, new DateTimeZone("UTC"));
    }
}
