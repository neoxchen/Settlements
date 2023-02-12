package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

/**
 * Sensor for detecting nearby living entities
 * - used in dog-walking related behaviors
 */
public class WolfSniffableEntitiesSensor extends Sensor<Wolf> {

    private static final double RANGE_HORIZONTAL = 15.0;
    private static final double RANGE_VERTICAL = 3.0;
    private static final double MIN_DISTANCE_SQUARED = Math.pow(4, 2);

    /**
     * How often does the sensor get triggered
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(7);

    /**
     * Actual cooldown: SNIFF_COOLDOWN * SENSE_COOLDOWN
     * - since doTick is called every SENSE_COOLDOWN seconds
     * - and cooldown is decremented every time doTick is called
     */
    private static final int SNIFF_COOLDOWN = 10;

    private final Map<UUID, Integer> sniffCooldownMap;

    public WolfSniffableEntitiesSensor() {
        super(SENSE_COOLDOWN);
        this.sniffCooldownMap = new HashMap<>();
    }

    @Override
    protected void doTick(ServerLevel world, Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf self))
            return;

        // Check activity == PLAY
        Brain<Wolf> brain = self.getBrain();
        if (brain.getSchedule().getActivityAt((int) world.getWorld().getTime()) != Activity.PLAY)
            return;

        // Criteria for selecting a sniffable entity
        Predicate<LivingEntity> criteria = (nearby) -> {
            // Check basic requirements
            if (nearby == null || nearby == self || nearby == self.getOwner() || !nearby.isAlive())
                return false;
            // Check minimum distance
            if (nearby.distanceToSqr(self) < MIN_DISTANCE_SQUARED)
                return false;
            // Check if we've sniffed it before
            UUID uuid = nearby.getUUID();
            if (this.sniffCooldownMap.containsKey(uuid))
                return false;
            this.sniffCooldownMap.put(uuid, SNIFF_COOLDOWN);
            return true;
        };

        // Create result list
        List<LivingEntity> sniffable = new ArrayList<>(1);

        // Try to get nearby entities fitting the criteria
        // - limit to 1 for efficiency
        AABB nearbyBoundingBox = self.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL, RANGE_HORIZONTAL);
        world.getEntities(EntityTypeTest.forClass(LivingEntity.class), nearbyBoundingBox, criteria, sniffable, 1);

        // Set or erase memory
        if (sniffable.isEmpty())
            brain.eraseMemory(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES);
        else
            brain.setMemory(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES, Optional.of(sniffable));

        // Clean up map
        this.updateCooldowns();
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES);
    }

    /**
     * Updates the cooldown map by subtracting 1 in all cooldowns
     * - entry is removed if the cooldown is <= 0
     */
    private void updateCooldowns() {
        Iterator<Map.Entry<UUID, Integer>> iterator = this.sniffCooldownMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> current = iterator.next();
            int newCooldown = current.getValue() - 1;
            if (newCooldown <= 0)
                iterator.remove();
            else
                this.sniffCooldownMap.put(current.getKey(), newCooldown);
        }
    }

}
