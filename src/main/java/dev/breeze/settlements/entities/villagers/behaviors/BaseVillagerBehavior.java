package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

public class BaseVillagerBehavior extends Behavior<Villager> {

    public BaseVillagerBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        super(requiredMemoryState, runTime);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        MessageUtil.broadcast("&a[Debug] Villager behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        MessageUtil.broadcast("&c[Debug] Villager behavior " + this.getClass().getSimpleName() + " has stopped");
    }

}
