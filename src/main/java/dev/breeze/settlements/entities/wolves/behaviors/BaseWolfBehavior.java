package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nonnull;
import java.util.Map;

public class BaseWolfBehavior extends Behavior<Wolf> {

    public BaseWolfBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        super(requiredMemoryState, runTime);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Wolf self, long gameTime) {
        MessageUtil.broadcast("&a[Debug] Wolf behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Wolf self, long gameTime) {
        MessageUtil.broadcast("&c[Debug] Wolf behavior " + this.getClass().getSimpleName() + " has stopped");
    }

}
