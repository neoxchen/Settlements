package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class InteractAtEntityBehavior extends BaseVillagerBehavior {

    /**
     * Entities outside of this range are ignored
     */
    @Getter
    private final double scanRangeSquared;

    /**
     * How long to wait after a successful interaction before scanning again
     */
    @Getter
    private final int interactCooldownTicks;
    /**
     * Only interact with entities within this range
     * - aka the "reach" of the villager
     */
    @Getter
    private final double interactRangeSquared;

    /**
     * The delay before navigateToTarget() is called in ticks
     */
    @Getter
    private final int maxNavigationIntervalTicks;

    /**
     * The delay before interactWithTarget() is called in ticks
     */
    @Getter
    private final int maxInteractionIntervalTicks;

    /**
     * Maximum number of ticks that can be spent in navigation
     * - terminates behavior if exceeds
     */
    @Getter
    private final int maxNavigationTicks;
    /**
     * Maximum number of ticks that can be spent interacting
     * - terminates behavior if exceeds
     */
    @Getter
    private final int maxInteractionTicks;

    /*
     * Dynamic variables
     */
    /**
     * Cooldown before the behavior can be run again
     * - can be set after a successful interaction (interactCooldownTicks)
     * - or after scanning and detecting no targets (scanCooldownTicks)
     */
    protected int cooldown;

    protected int navigationIntervalTicksLeft;
    protected int ticksSpentNavigating;

    protected int interactionIntervalTicksLeft;
    protected int ticksSpentInteracting;

    public InteractAtEntityBehavior(Map<MemoryModuleType<?>, MemoryStatus> preconditions,
                                    int scanCooldownTicks, double scanRangeSquared,
                                    int interactCooldownTicks, double interactRangeSquared,
                                    int maxNavigationIntervalTicks, int maxInteractionIntervalTicks,
                                    int maxNavigationTicks, int maxInteractionTicks) {
        // Max run time is calculated by the sum of navigation & interaction time
        super(preconditions, maxNavigationTicks + maxInteractionTicks, scanCooldownTicks);

        // Final variables
        this.scanRangeSquared = scanRangeSquared;

        this.interactCooldownTicks = interactCooldownTicks;
        this.interactRangeSquared = interactRangeSquared;
        if (this.interactRangeSquared < 1.5) {
            LogUtil.warning("%s's interact range squared (%.2f) is less than recommended value of 1.5", this.getClass().getSimpleName(),
                    this.interactRangeSquared);
        }

        this.maxNavigationIntervalTicks = maxNavigationIntervalTicks;
        this.maxInteractionIntervalTicks = maxInteractionIntervalTicks;

        this.maxNavigationTicks = maxNavigationTicks;
        this.maxInteractionTicks = maxInteractionTicks;

        // Dynamic variables
        // Initial cooldown = random up to max cooldown to prevent spamming
        this.cooldown = RandomUtil.RANDOM.nextInt(this.interactCooldownTicks) + TimeUtil.seconds(10);
        this.cooldown = 80;

        this.navigationIntervalTicksLeft = 0;
        this.ticksSpentNavigating = 0;

        this.interactionIntervalTicksLeft = 0;
        this.ticksSpentInteracting = 0;
    }

    @Override
    protected final boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Villager self) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        if (this.cooldown > 0)
            return false;
        return this.scan(level, self);
    }

    /**
     * Scans for nearby entities
     * - note: put expensive operations in this method
     */
    protected abstract boolean scan(ServerLevel level, Villager self);

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.start(level, self, gameTime);

        // Disable default walk target setting
        if (self instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(true);
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, Villager self, long gameTime) {
        // Terminate if we no longer have target
        if (!this.hasTarget())
            return false;

        // Check time limit
        if (this.ticksSpentNavigating > this.maxNavigationTicks || this.ticksSpentInteracting > this.maxInteractionTicks)
            return false;

        // Check extra conditions
        return this.checkExtraCanStillUseConditions(level, self, gameTime);
    }

    /**
     * Implemented by subclasses to add custom conditions
     */
    protected abstract boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime);

    @Override
    protected final void tick(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        // Interact with the target if we can reach it
        if (this.isTargetReachable(self)) {
            // Increment interaction duration
            this.ticksSpentInteracting++;

            // Check if we are ready for another interaction
            if (--this.interactionIntervalTicksLeft <= 0) {
                this.interactWithTarget(level, self, gameTime);
                // Reset cooldown
                this.interactionIntervalTicksLeft = this.maxInteractionIntervalTicks;
            }
        } else {
            // We are still far away from the target
            this.ticksSpentNavigating++;

            // Check if we are ready for another navigation call
            if (--this.navigationIntervalTicksLeft <= 0) {
                this.navigateToTarget(level, self, gameTime);
                // Reset cooldown
                this.navigationIntervalTicksLeft = this.maxNavigationIntervalTicks;
            }
        }

        // Execute extra logic
        this.tickExtra(level, self, gameTime);
    }

    /**
     * Extra logic that can be handled per tick
     * - called by tick() regardless of the returns of isTargetReachable()
     */
    protected abstract void tickExtra(ServerLevel level, Villager self, long gameTime);

    /**
     * Navigates to the target
     * - only called by tick() when isTargetReachable() returns false
     * - implemented by subclasses
     */
    protected abstract void navigateToTarget(ServerLevel level, Villager self, long gameTime);

    /**
     * Interacts with the target
     * - only called by tick() when isTargetReachable() returns true
     * - implemented by subclasses
     */
    protected abstract void interactWithTarget(ServerLevel level, Villager self, long gameTime);

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.stop(level, self, gameTime);

        // Enable default walk target setting
        if (self instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(false);

        // Reset variables
        this.cooldown = this.getInteractCooldownTicks();

        this.navigationIntervalTicksLeft = 0;
        this.ticksSpentNavigating = 0;

        this.interactionIntervalTicksLeft = 0;
        this.ticksSpentInteracting = 0;
    }

    /**
     * Determines whether this behavior has a valid target or not
     * - implemented by subclasses
     */
    protected abstract boolean hasTarget();

    /**
     * Determines whether the target is within interaction range
     * - implemented by subclasses
     */
    protected abstract boolean isTargetReachable(Villager self);

}
