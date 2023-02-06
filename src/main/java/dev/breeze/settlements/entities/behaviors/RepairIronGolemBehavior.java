package dev.breeze.settlements.entities.behaviors;

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
import net.minecraft.world.entity.npc.VillagerProfession;
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

    /**
     * How long before the villager searches around for golems to heal
     */
    private static final int SCAN_INTERVAL_TICKS = TimeUtil.seconds(20);

    /**
     * Golems that are further away from this distance will be ignored
     */
    private static final double SCAN_RANGE_SQUARED = Math.pow(30, 2);
    /**
     * Only engage in repairing mode within this distance from the golem
     */
    private static final double INTERACT_RANGE_SQUARED = Math.pow(2, 2);

    /**
     * Maximum number of ticks that can be spent in navigation
     * - terminates behavior if exceeds
     */
    private static final int MAX_NAVIGATION_TICKS = TimeUtil.seconds(20);
    /**
     * Maximum number of ticks that can be spent in repairing
     * - terminates behavior if exceeds
     */
    private static final int MAX_INTERACTION_TICKS = TimeUtil.seconds(10);

    /**
     * How long before the villager can repair another golem
     */
    private static final int COOLDOWN_TICKS = TimeUtil.minutes(2);

    /**
     * The repair speed of the villager, aka the interval of healing the golem
     */
    private static final int REPAIR_INTERVAL_TICKS = TimeUtil.seconds(2);
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

    private int ticksBeforeNextRepair;

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
                ), MAX_NAVIGATION_TICKS + MAX_INTERACTION_TICKS,
                SCAN_INTERVAL_TICKS, SCAN_RANGE_SQUARED, INTERACT_RANGE_SQUARED,
                COOLDOWN_TICKS, MAX_NAVIGATION_TICKS, MAX_INTERACTION_TICKS);

        this.ticksBeforeNextRepair = 0;
        this.targetGolem = null;
    }

    /**
     * Scans for nearby golems to repair
     */
    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        // If profession is not armorer, tool smith, or weapon smith, ignore
        VillagerProfession profession = self.getVillagerData().getProfession();
        if (profession != VillagerProfession.ARMORER && profession != VillagerProfession.TOOLSMITH && profession != VillagerProfession.WEAPONSMITH)
            return false;

        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetGolem == null) {
            // Check for nearby iron golems
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestGolem =
                    target.stream().filter(e -> e.getType() == EntityType.IRON_GOLEM && this.needHealing(e)).findFirst();

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

        // If golem does not need healing, stop
        if (!this.needHealing(this.targetGolem))
            return false;

        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager self, long gameTime) {
        // TODO: do we need anything here?
    }

    @Override
    protected void tick(ServerLevel level, Villager self, long gameTime) {
        // End behavior immediately if golem is null by setting time limit to exceeded
        if (this.targetGolem == null) {
            this.setTicksSpentNavigating(this.getMaxNavigationTicks());
            this.setTicksSpentInteracting(this.getMaxInteractionTicks());
            return;
        }

        self.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_INGOT).build()));
        self.setDropChance(EquipmentSlot.MAINHAND, 0f);

        // Walk to the target golem
        self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetGolem, true));
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetGolem, 0.5F, 1));
        self.getLookControl().setLookAt(this.targetGolem);

        // If not close to the golem, don't repair
        if (self.distanceToSqr(this.targetGolem) > this.getInteractRangeSquared()) {
            this.setTicksSpentNavigating(this.getTicksSpentNavigating() + 1);
            return;
        }

        // We are at the golem
        this.setTicksSpentInteracting(this.getTicksSpentInteracting() + 1);

        // TODO: have the golem look at the villager
//        this.targetGolem.lookAt(self, 30f, 30f);
//        this.targetGolem.getLookControl().setLookAt(self);

        // Check if we should repair now or wait
        if (--this.ticksBeforeNextRepair > 0)
            return;

        // Heal golem
        this.targetGolem.heal(REPAIR_AMOUNT_MAP.getOrDefault(self.getVillagerData().getLevel(), 5F),
                EntityRegainHealthEvent.RegainReason.CUSTOM);

        // TODO: random chance to offer flower?
//        if (RandomUtil.RANDOM.nextDouble() < 0.2)
//            this.targetGolem.offerFlower(true);

        // Display effects
        Location golemLocation = new Location(level.getWorld(), this.targetGolem.getX(), this.targetGolem.getY() + 1.2,
                this.targetGolem.getZ());
        ParticleUtil.globalParticle(golemLocation, Particle.WAX_OFF, 25, 0.4, 0.6, 0.4, 1);
        SoundUtil.playSoundPublic(golemLocation, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 0.7f);

        // Set repair cooldown
        this.ticksBeforeNextRepair = REPAIR_INTERVAL_TICKS;
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

        this.ticksBeforeNextRepair = 0;
        this.targetGolem = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetGolem != null;
    }

    private boolean needHealing(@Nonnull LivingEntity entity) {
        return entity.getHealth() < entity.getMaxHealth() * REPAIR_WHEN_BELOW_HP_PERCENTAGE;
    }

}
