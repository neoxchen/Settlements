package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nullable;
import java.util.Map;

public final class WolfPlayWithEntityBehavior extends BaseWolfBehavior {

    /**
     * What speed will the wolf move at when playing
     */
    private static final float SPEED_MODIFIER = 0.6F;

    /**
     * The chance that the wolf will jump while navigating
     */
    private static final double NAVIGATE_JUMP_CHANCE = 0.08;

    /**
     * How close will the wolf stay to the play target
     * - in blocks (squared)
     */
    private static final double INTERACT_DISTANCE_SQUARED = Math.pow(2.5, 2);

    /**
     * How long will the wolf "rest" after playing
     */
    private static final int MAX_PLAY_COOLDOWN = TimeUtil.seconds(20);

    /**
     * How long will the wolf play for
     */
    private static final int MAX_PLAY_DURATION = TimeUtil.seconds(15);

    private int cooldown;

    public WolfPlayWithEntityBehavior() {
        super(Map.of(
                // TODO: any?
        ), 5 * 20);

        // No initial cooldown
        this.cooldown = 0;
    }

    @Override
    protected final boolean checkExtraStartConditions(ServerLevel level, Wolf self) {
        // Check if we are still in cooldown
        if (--this.cooldown > 0)
            return false;

        // Check if the wolf is sitting
        if (self.isOrderedToSit())
            return false;

        // Reset cooldown if owner is null
        BaseVillager owner = this.getOwner(self);
        if (owner == null && this.cooldown < 0) {
            this.cooldown = MAX_PLAY_COOLDOWN;
            return false;
        }

        return owner != null;
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, Wolf self, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_PLAY_DURATION) {
            return false;
        }

        return this.checkExtraStartConditions(level, self);
    }

    @Override
    protected void start(ServerLevel level, Wolf self, long gameTime) {
        super.start(level, self, gameTime);
    }

    @Override
    protected final void tick(ServerLevel level, Wolf self, long gameTime) {
        BaseVillager owner = this.getOwner(self);

        // Safety null check
        if (owner == null)
            return;

        // Check if we are too far away from the owner
        if (self.distanceToSqr(owner) > INTERACT_DISTANCE_SQUARED) {
            // We are too far from the owner, walk to it
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(owner, true));
            self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, SPEED_MODIFIER, 1));
            self.getLookControl().setLookAt(owner);

            // Randomly jump
            if (RandomUtil.RANDOM.nextDouble() < NAVIGATE_JUMP_CHANCE)
                self.getJumpControl().jump();
        } else {
            // We are close enough to the owner to interact
            self.getJumpControl().jump();
        }
    }

    @Override
    protected void stop(ServerLevel level, Wolf self, long gameTime) {
        super.stop(level, self, gameTime);
    }

    /**
     * Gets the interaction target entity (can be null)
     */
    @Nullable
    private BaseVillager getOwner(Wolf wolf) {
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return null;
        return villagerWolf.getOwner();
    }

}

