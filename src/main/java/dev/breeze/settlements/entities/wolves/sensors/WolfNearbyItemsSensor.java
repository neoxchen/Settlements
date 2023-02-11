package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.behaviors.WolfFetchItemBehavior;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WolfNearbyItemsSensor extends Sensor<Wolf> {

    public static final String REGISTRY_KEY_NEARBY_ITEMS_SENSOR = "settlements_nearby_items_sensor";
    public static SensorType<WolfNearbyItemsSensor> NEARBY_ITEMS_SENSOR;

    /**
     * How far away to scan for items horizontally
     */
    private static final double RANGE_HORIZONTAL = 20.0D;

    /**
     * How far away to scan for items vertically
     */
    private static final double RANGE_VERTICAL = 4.5D;

    /**
     * How often will the wolf scans for nearby items
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(7);

    public WolfNearbyItemsSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(ServerLevel world, Wolf entity) {
        // Scan for nearby dropped items
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL,
                RANGE_HORIZONTAL), (itemEntity -> itemEntity != null && !itemEntity.isPassenger()));

        // Set or erase memory
        Brain<?> brain = entity.getBrain();
        if (list.isEmpty())
            brain.eraseMemory(WolfFetchItemBehavior.NEARBY_ITEMS_MEMORY);
        else
            brain.setMemory(WolfFetchItemBehavior.NEARBY_ITEMS_MEMORY, Optional.of(list));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfFetchItemBehavior.NEARBY_ITEMS_MEMORY);
    }

}
