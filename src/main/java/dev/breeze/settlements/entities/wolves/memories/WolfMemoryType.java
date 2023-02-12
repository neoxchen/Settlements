package dev.breeze.settlements.entities.wolves.memories;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public class WolfMemoryType {

    /**
     * Nearby dropped items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_wolf_nearby_items_memory";
    public static MemoryModuleType<List<ItemEntity>> NEARBY_ITEMS;

    /**
     * Nearby living entities
     */
    public static final String REGISTRY_KEY_SNIFFABLE_ENTITIES = "settlements_wolf_sniffable_entities_memory";
    public static MemoryModuleType<List<LivingEntity>> NEARBY_SNIFFABLE_ENTITIES;

}
