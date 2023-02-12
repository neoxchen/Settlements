package dev.breeze.settlements.entities.wolves.sensors;

import net.minecraft.world.entity.ai.sensing.SensorType;

public class WolfSensorType {

    /**
     * Sensor for scanning nearby items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_wolf_nearby_items_sensor";
    public static SensorType<WolfNearbyItemsSensor> NEARBY_ITEMS;

    /**
     * Sensor for scanning nearby living entities
     */
    public static final String REGISTRY_KEY_NEARBY_SNIFFABLE_ENTITIES = "settlements_wolf_nearby_sniffable_entities_sensor";
    public static SensorType<WolfSniffableEntitiesSensor> NEARBY_SNIFFABLE_ENTITIES;

}
