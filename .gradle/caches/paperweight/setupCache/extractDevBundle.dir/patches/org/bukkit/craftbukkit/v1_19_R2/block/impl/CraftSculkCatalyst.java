/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_19_R2.block.impl;

public final class CraftSculkCatalyst extends org.bukkit.craftbukkit.v1_19_R2.block.data.CraftBlockData implements org.bukkit.block.data.type.SculkCatalyst {

    public CraftSculkCatalyst() {
        super();
    }

    public CraftSculkCatalyst(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.v1_19_R2.block.data.type.CraftSculkCatalyst

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty BLOOM = getBoolean(net.minecraft.world.level.block.SculkCatalystBlock.class, "bloom");

    @Override
    public boolean isBloom() {
        return get(CraftSculkCatalyst.BLOOM);
    }

    @Override
    public void setBloom(boolean bloom) {
        set(CraftSculkCatalyst.BLOOM, bloom);
    }
}
