package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.animal.Wolf;

public final class WolfSitBehaviorController {

    private static final int DETECT_COOLDOWN = TimeUtil.seconds(10);

    public static OneShot<Wolf> sit() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(ServerLevel world, Wolf self, long time) {
                if (--this.cooldown > 0 || self.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                self.setOrderedToSit(true);
                return true;
            }
        };
    }

    public static OneShot<Wolf> stand() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(ServerLevel world, Wolf self, long time) {
                if (--this.cooldown > 0 || !self.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                self.setOrderedToSit(false);
                return true;
            }
        };
    }


}
