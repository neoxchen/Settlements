package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class ShearSheepBehavior extends InteractAtEntityBehavior {

    private static final ItemStack SHEARS = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.SHEARS).build());

    /**
     * The number of sheep to shear for each action
     * - 1-5: novice, apprentice, journeyman, expert, master
     */
    private static final Map<Integer, Integer> MAX_SHEAR_COUNT_MAP = Map.of(
            1, 2,
            2, 3,
            3, 4,
            4, 5,
            5, 6
    );

    /**
     * The number of sheep sheared in this action
     */
    private int sheepSheared;

    @Nullable
    private Sheep targetSheep;

    public ShearSheepBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(20), Math.pow(30, 2),
                TimeUtil.minutes(2), Math.pow(2, 2),
                5, 1,
                TimeUtil.seconds(20), 1);

        this.sheepSheared = 0;
        this.targetSheep = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetSheep == null) {
            Sheep sheep = this.scanForSheep(self);

            // If there are no nearby sheep to shear, ignore
            if (sheep == null)
                return false;

            this.targetSheep = sheep;
            self.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, sheep);
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        if (this.targetSheep == null || this.targetSheep.isDeadOrDying())
            return false;

        // If we've reached the maximum number of sheep sheared, stop
        return this.sheepSheared < MAX_SHEAR_COUNT_MAP.get(self.getVillagerData().getLevel());
    }

    @Override
    protected void start(ServerLevel level, Villager self, long gameTime) {
        // Do nothing
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        self.setItemSlot(EquipmentSlot.MAINHAND, SHEARS);
        self.setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.targetSheep == null)
            return;

        self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetSheep, true));
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetSheep, 0.5F, 1));
        self.getLookControl().setLookAt(this.targetSheep);
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.targetSheep == null)
            return;

        this.targetSheep.shear(SoundSource.NEUTRAL);
        this.sheepSheared++;

        // Scan for another sheep to shear
        // - target sheep may be null here, and it will be picked up by the next checkExtraCanStillUseConditions call
        this.targetSheep = this.scanForSheep(self);
    }

    @Override
    protected void stop(ServerLevel level, Villager self, long gameTime) {
        // Reset held item
        self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Remove golem from interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.ticksSpentNavigating = 0;
        this.ticksSpentInteracting = 0;
        this.cooldown = this.getInteractCooldownTicks();

        this.sheepSheared = 0;
        this.targetSheep = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetSheep != null;
    }

    @Override
    protected boolean isTargetReachable(Villager self) {
        return this.targetSheep != null && self.distanceToSqr(this.targetSheep) < this.getInteractRangeSquared();
    }

    @Nullable
    private Sheep scanForSheep(Villager villager) {
        if (villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return null;

        // Check for a nearby sheep
        List<LivingEntity> nearbyEntities = villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();

        Sheep sheep = null;
        for (LivingEntity nearby : nearbyEntities) {
            if (!(nearby instanceof Sheep nearbySheep) || !nearbySheep.readyForShearing())
                continue;
            sheep = nearbySheep;
        }
        return sheep;
    }

}
