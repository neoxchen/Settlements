package org.bukkit.craftbukkit.v1_19_R2.block.data.type;

import org.bukkit.block.data.type.SculkShrieker;
import org.bukkit.craftbukkit.v1_19_R2.block.data.CraftBlockData;

public abstract class CraftSculkShrieker extends CraftBlockData implements SculkShrieker {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty CAN_SUMMON = getBoolean("can_summon");
    private static final net.minecraft.world.level.block.state.properties.BooleanProperty SHRIEKING = getBoolean("shrieking");

    @Override
    public boolean isCanSummon() {
        return get(CraftSculkShrieker.CAN_SUMMON);
    }

    @Override
    public void setCanSummon(boolean can_summon) {
        set(CraftSculkShrieker.CAN_SUMMON, can_summon);
    }

    @Override
    public boolean isShrieking() {
        return get(CraftSculkShrieker.SHRIEKING);
    }

    @Override
    public void setShrieking(boolean shrieking) {
        set(CraftSculkShrieker.SHRIEKING, shrieking);
    }
}
