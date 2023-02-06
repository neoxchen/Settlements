package dev.breeze.settlements.entities.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

@Getter
@Setter
public abstract class InteractAtEntityBehavior extends Behavior<Villager> {

    /**
     * The interval between searching nearby entities for the target
     */
    private final int scanIntervalTicks;
    /**
     * Entities outside of this range are ignored
     */
    private final double scanRangeSquared;
    /**
     * Only interact with entities within this range
     * - aka the "reach" of the villager
     */
    private final double interactRangeSquared;

    /**
     * Maximum number of ticks that can be spent in navigation
     * - terminates behavior if exceeds
     */
    private final int maxNavigationTicks;
    /**
     * Maximum number of ticks that can be spent interacting
     * - terminates behavior if exceeds
     */
    private final int maxInteractionTicks;

    /**
     * How long to wait after a successful interaction before scanning again
     */
    private final int maxCooldownTicks;

    /*
     * Dynamic variables
     */
    private int cooldown;
    private int ticksSpentNavigating;
    private int ticksSpentInteracting;

    public InteractAtEntityBehavior(Map<MemoryModuleType<?>, MemoryStatus> preconditions, int duration,
                                    int scanIntervalTicks, double scanRangeSquared, double interactRangeSquared,
                                    int maxCooldownTicks, int maxNavigationTicks, int maxInteractionTicks) {
        super(preconditions, duration);

        this.scanIntervalTicks = scanIntervalTicks;
        this.scanRangeSquared = scanRangeSquared;
        this.interactRangeSquared = interactRangeSquared;

        this.maxCooldownTicks = maxCooldownTicks;
        this.maxNavigationTicks = maxNavigationTicks;
        this.maxInteractionTicks = maxInteractionTicks;

        this.ticksSpentNavigating = 0;
        this.ticksSpentInteracting = 0;
        // Initial cooldown = max cooldown to prevent spamming
        this.cooldown = this.maxCooldownTicks;
    }

    @Override
    protected final boolean checkExtraStartConditions(ServerLevel level, Villager self) {
        // TODO: remove
        for (Player p : Bukkit.getOnlinePlayers())
            MessageUtil.sendActionbar(p, "&aCooldown: %d - target? " + this.hasTarget(), this.cooldown);
        // Check if we are still in cooldown
        if (--this.cooldown > 0)
            return false;

        // Check if we should scan or wait
        if (!this.hasTarget() && this.cooldown < 0) {
            this.cooldown = this.getScanIntervalTicks();
            return false;
        }

        // Only reached when (scan) cooldown == 0
        return this.scan(level, self);
    }

    /**
     * Scans for nearby entities
     * - note: put expensive operations in this method
     */
    protected abstract boolean scan(ServerLevel level, Villager self);

    @Override
    protected final boolean canStillUse(ServerLevel level, Villager self, long gameTime) {
        // Terminate if we no longer have target
        if (!this.hasTarget())
            return false;

        // Check time limit
        if (this.ticksSpentNavigating > this.getMaxNavigationTicks() || this.ticksSpentInteracting > this.getMaxInteractionTicks())
            return false;

        // Check extra conditions
        if (!this.checkExtraCanStillUseConditions(level, self, gameTime))
            return false;

        return this.checkExtraStartConditions(level, self);
    }

    protected abstract boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime);

    @Override
    protected abstract void start(ServerLevel level, Villager self, long gameTime);

    @Override
    protected abstract void tick(ServerLevel level, Villager self, long gameTime);

    @Override
    protected abstract void stop(ServerLevel level, Villager self, long gameTime);

    protected abstract boolean hasTarget();

}
