package dev.breeze.settlements.entities.villagers.sensors;

import net.minecraft.world.entity.ai.sensing.SensorType;

public class VillagerSensorType {

    /**
     * Sensor for scanning nearby water areas
     */
    public static final String REGISTRY_KEY_NEAREST_WATER_AREA = "settlements_villager_nearest_water_area_sensor";
    public static SensorType<VillagerNearbyWaterAreaSensor> NEAREST_WATER_AREA;

}
