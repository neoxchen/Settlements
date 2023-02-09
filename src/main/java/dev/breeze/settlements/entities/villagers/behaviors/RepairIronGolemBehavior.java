package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RepairIronGolemBehavior extends InteractAtEntityBehavior {

    private static final ItemStack IRON_INGOT = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_INGOT).build());

    /**
     * The amount of HP to heal per repair action
     * - 1-5: novice, apprentice, journeyman, expert, master
     */
    private static final Map<Integer, Float> REPAIR_AMOUNT_MAP = Map.of(
            1, 5F,
            2, 7F,
            3, 9F,
            4, 12F,
            5, 15F
    );

    /**
     * Golems above this HP% will not be considered as a repair target
     */
    private static final double REPAIR_WHEN_BELOW_HP_PERCENTAGE = 0.999;

    @Nullable
    private IronGolem targetGolem;

    public RepairIronGolemBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // There should be iron golems detected recently to run this behavior
                        MemoryModuleType.GOLEM_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(20), Math.pow(30, 2),
                TimeUtil.minutes(2), Math.pow(2, 2),
                5, TimeUtil.seconds(2),
                TimeUtil.seconds(20), TimeUtil.seconds(10));

        this.targetGolem = null;
    }

    /**
     * Scans for nearby golems to repair
     */
    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetGolem == null) {
            // Check for nearby iron golems
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestGolem = target.stream().filter(e -> e.getType() == EntityType.IRON_GOLEM && this.needHealing(e)).findFirst();

            // If no nearby iron golems, ignore
            if (nearestGolem.isEmpty())
                return false;

            IronGolem golem = (IronGolem) nearestGolem.get();

            // If golem is too far away, ignore
            if (self.distanceToSqr(golem) > this.getScanRangeSquared())
                return false;

            this.targetGolem = golem;
            self.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, nearestGolem.get());
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        if (this.targetGolem == null || this.targetGolem.isDeadOrDying())
            return false;
        return this.needHealing(this.targetGolem);
    }

    @Override
    protected void start(ServerLevel level, Villager self, long gameTime) {
        // TODO: do we need anything here?
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        self.setItemSlot(EquipmentSlot.MAINHAND, IRON_INGOT);
        self.setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.targetGolem == null)
            return;

        // Walk to the target golem
        self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetGolem, true));
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetGolem, 0.5F, 1));
        self.getLookControl().setLookAt(this.targetGolem);
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.targetGolem == null)
            return;

        // TODO: have the golem look at the villager
//        this.targetGolem.lookAt(self, 30f, 30f);
//        this.targetGolem.getLookControl().setLookAt(self);

        // Heal golem
        this.targetGolem.heal(REPAIR_AMOUNT_MAP.getOrDefault(self.getVillagerData().getLevel(), 5F), EntityRegainHealthEvent.RegainReason.CUSTOM);

        // TODO: random chance to offer flower?
//        if (RandomUtil.RANDOM.nextDouble() < 0.2)
//            this.targetGolem.offerFlower(true);

        // Display effects
        Location golemLocation = new Location(level.getWorld(), this.targetGolem.getX(), this.targetGolem.getY() + 1.2,
                this.targetGolem.getZ());
        ParticleUtil.globalParticle(golemLocation, Particle.WAX_OFF, 25, 0.4, 0.6, 0.4, 1);
        SoundUtil.playSoundPublic(golemLocation, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 0.7f);
    }

    @Override
    protected void stop(ServerLevel level, Villager self, long gameTime) {
        // Reset held item
        self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Remove golem from interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.cooldown = this.getInteractCooldownTicks();

        this.ticksSpentNavigating = 0;
        this.ticksSpentInteracting = 0;

        this.targetGolem = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetGolem != null;
    }

    @Override
    protected boolean isTargetReachable(Villager self) {
        return this.targetGolem != null && self.distanceToSqr(this.targetGolem) < this.getInteractRangeSquared();
    }

    private boolean needHealing(@Nonnull LivingEntity entity) {
        return entity.getHealth() < entity.getMaxHealth() * REPAIR_WHEN_BELOW_HP_PERCENTAGE;
    }

}
