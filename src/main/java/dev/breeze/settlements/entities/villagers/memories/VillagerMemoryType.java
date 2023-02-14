package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public class VillagerMemoryType {

    private static final String NBT_TAG_NAME = "settlements_memories";

    public static final String REGISTRY_KEY_FENCE_GATE_TO_CLOSE = "settlements_villager_fence_gates_to_close_memory";
    public static MemoryModuleType<Set<GlobalPos>> FENCE_GATE_TO_CLOSE;

    public static final String REGISTRY_KEY_OWNED_DOG = "settlements_villager_owned_dogs_memory";
    public static MemoryModuleType<UUID> OWNED_DOG;

    public static final String REGISTRY_KEY_WALK_DOG_TARGET = "settlements_villager_walk_dog_target_memory";
    public static MemoryModuleType<VillagerWolf> WALK_DOG_TARGET;


    /**
     * Export important memories to NBT
     * - only certain memories are persistent
     * - other are deleted upon unloading
     */
    public static void save(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = new CompoundTag();

        if (brain.hasMemoryValue(OWNED_DOG)) {
            memories.put("owned_dog", StringTag.valueOf(brain.getMemory(OWNED_DOG).get().toString()));
        }
        
        // Write to NBT tag
        nbt.put(NBT_TAG_NAME, memories);
    }

    /**
     * Attempts to load the custom memories to the villager brain
     */
    public static void load(CompoundTag nbt, BaseVillager villager) {
        // Safety check
        if (!nbt.contains(NBT_TAG_NAME))
            return;

        // Load memories to brain
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = nbt.getCompound(NBT_TAG_NAME);
        if (memories.contains("owned_dog")) {
            brain.setMemory(OWNED_DOG, UUID.fromString(memories.getString("owned_dog")));
        }
    }

}
