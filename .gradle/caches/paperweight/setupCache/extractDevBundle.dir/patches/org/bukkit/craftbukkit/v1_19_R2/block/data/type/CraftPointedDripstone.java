package org.bukkit.craftbukkit.v1_19_R2.block.data.type;

import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.craftbukkit.v1_19_R2.block.data.CraftBlockData;

public abstract class CraftPointedDripstone extends CraftBlockData implements PointedDripstone {

    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> VERTICAL_DIRECTION = getEnum("vertical_direction");
    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> THICKNESS = getEnum("thickness");

    @Override
    public org.bukkit.block.BlockFace getVerticalDirection() {
        return get(CraftPointedDripstone.VERTICAL_DIRECTION, org.bukkit.block.BlockFace.class);
    }

    @Override
    public void setVerticalDirection(org.bukkit.block.BlockFace direction) {
        set(CraftPointedDripstone.VERTICAL_DIRECTION, direction);
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getVerticalDirections() {
        return getValues(CraftPointedDripstone.VERTICAL_DIRECTION, org.bukkit.block.BlockFace.class);
    }

    @Override
    public org.bukkit.block.data.type.PointedDripstone.Thickness getThickness() {
        return get(CraftPointedDripstone.THICKNESS, org.bukkit.block.data.type.PointedDripstone.Thickness.class);
    }

    @Override
    public void setThickness(org.bukkit.block.data.type.PointedDripstone.Thickness thickness) {
        set(CraftPointedDripstone.THICKNESS, thickness);
    }
}
