package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.Wolf;

import java.util.HashMap;

public class TestWolfBehavior extends Behavior<Wolf> {

    private int cooldown;
    private final String name;

    public TestWolfBehavior(String name) {
        super(new HashMap<>(), 5 * 20);
        this.cooldown = 5 * 20;
        this.name = name;
    }

    @Override
    protected final boolean checkExtraStartConditions(ServerLevel level, Wolf self) {
        // Check if we are still in cooldown
        if (--this.cooldown > 0) {
            if (this.cooldown % 40 == 0)
                this.action("Cooling down action %s - %d", this.name, this.cooldown);
            return false;
        }

        if (this.cooldown > -5 * 20)
            return true;

        this.cooldown = 5 * 20;
        return false;
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, Wolf self, long gameTime) {
        return this.checkExtraStartConditions(level, self);
    }

    @Override
    protected void start(ServerLevel level, Wolf self, long gameTime) {
    }

    @Override
    protected final void tick(ServerLevel level, Wolf self, long gameTime) {
        if (this.cooldown % 40 == 0)
            this.action("Performing action %s - %d", this.name, this.cooldown);
    }

    @Override
    protected void stop(ServerLevel level, Wolf self, long gameTime) {
    }

    private void action(String format, Object... args) {
        MessageUtil.broadcast(format, args);
    }

}

