package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Set;

public class VillagerMemoryType {

    public static final String REGISTRY_KEY_FENCE_GATE_TO_CLOSE = "settlements_villager_fence_gates_to_close_memory";
    public static MemoryModuleType<Set<GlobalPos>> FENCE_GATE_TO_CLOSE;

    public static final String REGISTRY_KEY_WALK_DOG_TARGET = "settlements_villager_walk_dog_target_memory";
    public static MemoryModuleType<VillagerWolf> WALK_DOG_TARGET;

}
