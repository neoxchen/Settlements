package dev.breeze.settlements.entities.behaviors;

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
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class ShearSheepBehavior extends InteractAtEntityBehavior {

    private static final int SCAN_INTERVAL_TICKS = TimeUtil.seconds(20);
    private static final double SCAN_RANGE_SQUARED = Math.pow(30, 2);
    private static final double INTERACT_RANGE_SQUARED = Math.pow(2, 2);

    private static final int MAX_NAVIGATION_TICKS = TimeUtil.seconds(20);
    private static final int NAVIGATION_COOLDOWN_TICKS = 5;
    private int navigationCooldown;

    private static final int COOLDOWN_TICKS = 20 * 20; // TODO: 5 minutes

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
                ), MAX_NAVIGATION_TICKS + 1,
                SCAN_INTERVAL_TICKS, SCAN_RANGE_SQUARED, INTERACT_RANGE_SQUARED,
                COOLDOWN_TICKS, MAX_NAVIGATION_TICKS, 1);

        this.navigationCooldown = 0;

        this.sheepSheared = 0;
        this.targetSheep = null;
    }

    /**
     * Scans for nearby golems to repair
     */
    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        // If profession is not shepherd, ignore
        VillagerProfession profession = self.getVillagerData().getProfession();
        if (profession != VillagerProfession.SHEPHERD)
            return false;

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
        if (this.sheepSheared >= MAX_SHEAR_COUNT_MAP.get(self.getVillagerData().getLevel()))
            return false;

        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager self, long gameTime) {
        // Do nothing
    }


    @Override
    protected void tick(ServerLevel level, Villager self, long gameTime) {
        // End behavior immediately if target is null
        if (this.targetSheep == null) {
            this.setTicksSpentNavigating(this.getMaxNavigationTicks());
            this.setTicksSpentInteracting(this.getMaxInteractionTicks());
            return;
        }

        self.setItemSlot(EquipmentSlot.MAINHAND, SHEARS);
        self.setDropChance(EquipmentSlot.MAINHAND, 0f);

        // Walk to the target
        if (--this.navigationCooldown < 0) {
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetSheep, true));
            self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetSheep, 0.5F, 1));
            self.getLookControl().setLookAt(this.targetSheep);

            this.navigationCooldown = NAVIGATION_COOLDOWN_TICKS;
        }

        // Check distance to target
        if (self.distanceToSqr(this.targetSheep) > this.getInteractRangeSquared()) {
            this.setTicksSpentNavigating(this.getTicksSpentNavigating() + 1);
            return;
        }

        // We are at the sheep, now shear it
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
        this.setTicksSpentNavigating(0);
        this.setTicksSpentInteracting(0);
        this.setCooldown(this.getMaxCooldownTicks());

        this.sheepSheared = 0;
        this.targetSheep = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetSheep != null;
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
