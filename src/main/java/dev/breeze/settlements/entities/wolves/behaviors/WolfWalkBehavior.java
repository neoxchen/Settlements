package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class WolfWalkBehavior extends BaseWolfBehavior {

    private static final float WALK_SPEED_MODIFIER = 0.85F;
    private static final float NOTIFY_SPEED_MODIFIER = 1.1F;

    // The maximum range that the wolf can pick a new target to walk to in blocks
    private static final int MAX_HORIZONTAL_DELTA = 10;
    private static final int MAX_VERTICAL_DELTA = 7;
    private static final int MAX_FAIL_COUNT = 10;

    /**
     * How far away can the wolf notify the owner its intent to walk
     */
    private static final double NOTIFY_OWNER_DISTANCE_SQUARED = Math.pow(2, 2);

    /**
     * Also used in WalkDogBehavior for villagers
     */
    public static final int MAX_WALK_DURATION = TimeUtil.seconds(45);
    private static final int MAX_WALK_COOLDOWN = TimeUtil.minutes(20);
    /**
     * Initial cooldown is random between [0, MAX_WALK_INITIAL_COOLDOWN)
     */
    private static final int MAX_WALK_INITIAL_COOLDOWN = TimeUtil.minutes(1);

    /**
     * The probability of sniffing an entity (as opposed to sniffing a block) while taking a walk
     */
    private static final double SNIFF_ENTITY_PROBABILITY = 0.7;

    private static final int SNIFF_MIN_DURATION = TimeUtil.seconds(2);
    private static final int SNIFF_MAX_DURATION = TimeUtil.seconds(5);

    private int cooldown;
    private int sniffDuration;
    private WalkStatus status;

    @Nullable
    private Vec3 target;

    public WolfWalkBehavior() {
        super(Map.of(
                // No preconditions
        ), MAX_WALK_DURATION);

        this.cooldown = RandomUtil.RANDOM.nextInt(MAX_WALK_INITIAL_COOLDOWN);

        this.sniffDuration = 0;
        this.status = WalkStatus.STANDBY;

        this.target = null;
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Wolf wolf) {
        if (--this.cooldown > 0)
            return false;

        if (!(wolf instanceof VillagerWolf self))
            return false;

        // Check if the wolf has owner
        BaseVillager owner = self.getOwner();
        if (owner == null)
            return false;

        // Check if the wolf is sitting
        if (self.isOrderedToSit())
            return false;

        if (self.getBrain().getSchedule().getActivityAt((int) level.getWorld().getTime()) != Activity.PLAY)
            return false;

        return true;
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, Wolf self, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_WALK_DURATION)
            return false;
        return this.checkExtraStartConditions(level, self);
    }

    @Override
    protected void start(ServerLevel level, Wolf self, long gameTime) {
        super.start(level, self, gameTime);

        this.status = WalkStatus.NOTIFYING_OWNER;
        if (self instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(true);
            villagerWolf.setLookLocked(true);
            villagerWolf.setMovementLocked(true);
        }
    }

    @Override
    protected final void tick(ServerLevel level, Wolf wolf, long gameTime) {
        if (!(wolf instanceof VillagerWolf self))
            return;

        if (this.status == WalkStatus.NOTIFYING_OWNER) {
            if (self.distanceToSqr(self.getOwner()) > NOTIFY_OWNER_DISTANCE_SQUARED) {
                // Not close enough to the owner, walk to it
                self.getNavigation().moveTo(self.getOwner(), NOTIFY_SPEED_MODIFIER);
            } else {
                self.getOwner().getBrain().setMemory(VillagerMemoryType.WALK_DOG_TARGET, self);
                this.status = WalkStatus.SNIFFING;
            }
            return;
        } else if (this.status == WalkStatus.WALKING) {
            // Check if current path is done
            PathNavigation navigation = self.getNavigation();
            if (!navigation.isDone())
                return;

            // Navigation is done, change state to sniffing
            this.status = WalkStatus.SNIFFING;
            this.sniffDuration = RandomUtil.RANDOM.nextInt(SNIFF_MIN_DURATION, SNIFF_MAX_DURATION);
            return;
        } else if (this.status == WalkStatus.SNIFFING && --this.sniffDuration > 0) {
            self.getNavigation().stop();
            if (this.target != null)
                self.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z);
            return;
        }

        // Current path is done, randomize another target
        boolean sniffTargetFound = false;
        if (RandomUtil.RANDOM.nextDouble() < SNIFF_ENTITY_PROBABILITY) {
            // Sniff an entity next
            LivingEntity target = this.scanForNearbyEntities(self);
            if (target != null) {
                sniffTargetFound = true;
                // Navigate to the entity
                self.getNavigation().moveTo(target, WALK_SPEED_MODIFIER);
                self.getLookControl().setLookAt(target);
                this.target = null;
            }
        }

        // Sniff a block if
        // 1. RNG selected not to sniff an entity
        // 2. failed to find a sniffable entity nearby
        if (!sniffTargetFound) {
            int failCount = 0;
            Vec3 target = this.generateRandomLocation(self);
            while (target == null && failCount < MAX_FAIL_COUNT) {
                target = this.generateRandomLocation(self);
                failCount++;
            }

            if (target == null) {
                // We failed 10 times to find a spot, stop behavior early
                this.stop(level, wolf, gameTime);
                return;
            }

            // Navigate to the block
            self.getNavigation().moveTo(target.x, target.y, target.z, WALK_SPEED_MODIFIER);
            this.target = new Vec3(target.x, target.y - 1, target.z);
        }

        this.status = WalkStatus.WALKING;
    }

    @Override
    protected void stop(ServerLevel level, Wolf self, long gameTime) {
        super.stop(level, self, gameTime);

        this.cooldown = MAX_WALK_COOLDOWN;
        this.sniffDuration = 0;
        this.status = WalkStatus.STANDBY;

        if (self instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(false);
            villagerWolf.setLookLocked(false);
            villagerWolf.setMovementLocked(false);

            if (villagerWolf.getOwner() != null)
                villagerWolf.getOwner().getBrain().eraseMemory(VillagerMemoryType.WALK_DOG_TARGET);
        }
    }

    /**
     * Generates the next random location to stroll to
     */
    @Nullable
    private Vec3 generateRandomLocation(VillagerWolf wolf) {
        return DefaultRandomPos.getPos(wolf, MAX_HORIZONTAL_DELTA, MAX_VERTICAL_DELTA);
    }

    /**
     * Generates the next random location to stroll to
     */
    @Nullable
    private LivingEntity scanForNearbyEntities(VillagerWolf wolf) {
        Brain<Wolf> brain = wolf.getBrain();
        if (!brain.hasMemoryValue(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES))
            return null;

        // Get sniffable entities from memory
        List<LivingEntity> sniffable = brain.getMemory(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES).get();
        if (sniffable.isEmpty())
            return null;

        return sniffable.get(0);
    }

    private enum WalkStatus {
        /**
         * Not taking a walk
         */
        STANDBY,

        /**
         * Notifying the owning villager of the intent to walk
         */
        NOTIFYING_OWNER,

        /**
         * Walking towards a target
         */
        WALKING,

        /**
         * Stopping to sniff something
         */
        SNIFFING
    }

}

