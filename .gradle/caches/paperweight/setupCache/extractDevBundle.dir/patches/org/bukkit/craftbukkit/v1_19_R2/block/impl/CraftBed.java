/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_19_R2.block.impl;

public final class CraftBed extends org.bukkit.craftbukkit.v1_19_R2.block.data.CraftBlockData implements org.bukkit.block.data.type.Bed, org.bukkit.block.data.Directional {

    public CraftBed() {
        super();
    }

    public CraftBed(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.v1_19_R2.block.data.type.CraftBed

    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> PART = getEnum(net.minecraft.world.level.block.BedBlock.class, "part");
    private static final net.minecraft.world.level.block.state.properties.BooleanProperty OCCUPIED = getBoolean(net.minecraft.world.level.block.BedBlock.class, "occupied");

    @Override
    public org.bukkit.block.data.type.Bed.Part getPart() {
        return get(CraftBed.PART, org.bukkit.block.data.type.Bed.Part.class);
    }

    @Override
    public void setPart(org.bukkit.block.data.type.Bed.Part part) {
        set(CraftBed.PART, part);
    }

    @Override
    public boolean isOccupied() {
        return get(CraftBed.OCCUPIED);
    }

    // org.bukkit.craftbukkit.v1_19_R2.block.data.CraftDirectional

    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> FACING = getEnum(net.minecraft.world.level.block.BedBlock.class, "facing");

    @Override
    public org.bukkit.block.BlockFace getFacing() {
        return get(CraftBed.FACING, org.bukkit.block.BlockFace.class);
    }

    @Override
    public void setFacing(org.bukkit.block.BlockFace facing) {
        set(CraftBed.FACING, facing);
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getFaces() {
        return getValues(CraftBed.FACING, org.bukkit.block.BlockFace.class);
    }

    // Paper start
    @Override
    public void setOccupied(boolean occupied) {
        set(CraftBed.OCCUPIED, occupied);
    }
    // Paper end
}
