package org.bukkit.craftbukkit.v1_19_R2.entity;

import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.entity.Camel;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;

public class CraftCamel extends CraftAbstractHorse implements Camel {

    public CraftCamel(CraftServer server, net.minecraft.world.entity.animal.camel.Camel entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.camel.Camel getHandle() {
        return (net.minecraft.world.entity.animal.camel.Camel) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftCamel";
    }

    @Override
    public EntityType getType() {
        return EntityType.CAMEL;
    }

    @Override
    public Horse.Variant getVariant() {
        return Horse.Variant.CAMEL;
    }

    @Override
    public boolean isDashing() {
        return this.getHandle().isDashing();
    }

    @Override
    public void setDashing(boolean dashing) {
        this.getHandle().setDashing(dashing);
    }

    @Override
    public boolean isSitting() {
        return this.getHandle().isPoseSitting();
    }

    @Override
    public void setSitting(boolean sitting) {
        if (sitting) {
            this.getHandle().sitDown();
        } else {
            this.getHandle().standUp();
        }
    }
}
