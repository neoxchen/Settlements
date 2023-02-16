package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WolfWalkBehavior extends BaseWolfBehavior {

    private static final float WALK_SPEED_MODIFIER = 0.9F;
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
    public static final int MAX_WALK_DURATION = TimeUtil.minutes(1);
    private static final int MAX_WALK_COOLDOWN = TimeUtil.minutes(20);
    /**
     * Initial cooldown is random between [0, MAX_WALK_INITIAL_COOLDOWN)
     */
    private static final int MAX_WALK_INITIAL_COOLDOWN = TimeUtil.minutes(1);

    /**
     * The probability of sniffing an entity (as opposed to sniffing a block) while taking a walk
     */
    private static final double SNIFF_ENTITY_PROBABILITY = 0.7;

    /**
     * The probability of digging instead of sniffing something
     * - note: digging can only occur if the wolf is sniffing a block
     */
    private static final double DIG_PROBABILITY = 0.15;

    private static final int SNIFF_MIN_DURATION = TimeUtil.seconds(2);
    private static final int SNIFF_MAX_DURATION = TimeUtil.seconds(5);

    private int cooldown;
    private int sniffDuration;
    private WalkStatus status;

    @Nullable
    private Vec3 target;
    @Nullable
    private LivingEntity targetEntity;

    private boolean isDigging;

    public WolfWalkBehavior() {
        super(Map.of(
                // No preconditions
        ), MAX_WALK_DURATION);

        this.cooldown = RandomUtil.RANDOM.nextInt(MAX_WALK_INITIAL_COOLDOWN);

        this.sniffDuration = 0;
        this.status = WalkStatus.STANDBY;

        this.target = null;
        this.targetEntity = null;

        this.isDigging = false;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Wolf wolf) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        if (this.cooldown > 0)
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

        return self.getBrain().getSchedule().getActivityAt((int) level.getWorld().getTime()) == Activity.PLAY;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Wolf self, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_WALK_DURATION)
            return false;
        return this.checkExtraStartConditions(level, self);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Wolf self, long gameTime) {
        super.start(level, self, gameTime);

        this.status = WalkStatus.NOTIFYING_OWNER;
        if (self instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(true);
            villagerWolf.setLookLocked(true);
            villagerWolf.setMovementLocked(true);
        }
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
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

            // Randomly digs if we are sniffing a block
            if (this.target != null && this.targetEntity == null) {
                this.isDigging = RandomUtil.RANDOM.nextDouble() < DIG_PROBABILITY;
            }

            return;
        } else if (this.status == WalkStatus.SNIFFING && --this.sniffDuration > 0) {
            if (this.target != null) {
                self.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z);
                self.getNavigation().stop();
            } else if (this.targetEntity != null) {
                self.getLookControl().setLookAt(this.targetEntity);

                // Follow the entity
                if (self.getNavigation().isDone())
                    self.getNavigation().moveTo(this.targetEntity, WALK_SPEED_MODIFIER);
            }

            // Spawn dig effects if isDigging
            if (this.isDigging && this.sniffDuration % 5 == 0) {
                Location location = new Location(self.level.getWorld(), self.getX(), self.getY(), self.getZ());
                ParticleUtil.blockBreak(location, location.clone().add(0, -1, 0).getBlock().getType(), 5, 0.4, 0.3, 0.4, 0.1);
                SoundUtil.playSoundPublic(location, Sound.BLOCK_SAND_HIT, 0.2F, 1.4F);
            }

            return;
        }

        // Reset isDigging
        this.isDigging = false;

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

                // Set entity to recently sniffed
                if (!self.getBrain().hasMemoryValue(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES)) {
                    self.getBrain().setMemory(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES, new HashSet<>(Set.of(target)));
                } else {
                    self.getBrain().getMemory(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES).get().add(target);
                }

                // Set cache variables
                this.target = null;
                this.targetEntity = target;
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

            // Set cache variables
            this.target = new Vec3(target.x, target.y - 1, target.z);
            this.targetEntity = null;
        }

        this.status = WalkStatus.WALKING;
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Wolf self, long gameTime) {
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

            // Remove all entities recently sniffed
            villagerWolf.getBrain().eraseMemory(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES);
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

        // If we haven't sniffed any entities recently, return first
        if (!brain.hasMemoryValue(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES)) {
            return sniffable.get(0);
        }

        // Otherwise, don't sniff entities that are recently sniffed
        Set<LivingEntity> recentlySniffed = brain.getMemory(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES).get();
        return sniffable.stream().filter(nearby -> !recentlySniffed.contains(nearby)).findFirst().orElse(null);
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
         * Stopping to sniff or dig something
         */
        SNIFFING
    }

}

