package org.bukkit.craftbukkit.v1_19_R2.inventory;

import static org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemFactory.*;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftMetaItem.SerializableMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

@DelegateDeserialization(SerializableMeta.class)
class CraftMetaLeatherArmor extends CraftMetaItem implements LeatherArmorMeta {

    private static final Set<Material> LEATHER_ARMOR_MATERIALS = Sets.newHashSet(
            Material.LEATHER_HELMET,
            Material.LEATHER_HORSE_ARMOR,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS
    );

    static final ItemMetaKey COLOR = new ItemMetaKey("color");

    private Color color = DEFAULT_LEATHER_COLOR;

    CraftMetaLeatherArmor(CraftMetaItem meta) {
        super(meta);
        if (!(meta instanceof CraftMetaLeatherArmor)) {
            return;
        }

        CraftMetaLeatherArmor armorMeta = (CraftMetaLeatherArmor) meta;
        this.color = armorMeta.color;
    }

    CraftMetaLeatherArmor(CompoundTag tag) {
        super(tag);
        if (tag.contains(DISPLAY.NBT)) {
            CompoundTag display = tag.getCompound(DISPLAY.NBT);
            if (display.contains(COLOR.NBT)) {
                try {
                    this.color = Color.fromRGB(display.getInt(COLOR.NBT));
                } catch (IllegalArgumentException ex) {
                    // Invalid colour
                }
            }
        }
    }

    CraftMetaLeatherArmor(Map<String, Object> map) {
        super(map);
        this.setColor(SerializableMeta.getObject(Color.class, map, COLOR.BUKKIT, true));
    }

    @Override
    void applyToItem(CompoundTag itemTag) {
        super.applyToItem(itemTag);

        if (this.hasColor()) {
            setDisplayTag(itemTag, COLOR.NBT, IntTag.valueOf(this.color.asRGB()));
        }
    }

    @Override
    boolean isEmpty() {
        return super.isEmpty() && this.isLeatherArmorEmpty();
    }

    boolean isLeatherArmorEmpty() {
        return !(this.hasColor());
    }

    @Override
    boolean applicableTo(Material type) {
        return CraftMetaLeatherArmor.LEATHER_ARMOR_MATERIALS.contains(type);
    }

    @Override
    public CraftMetaLeatherArmor clone() {
        return (CraftMetaLeatherArmor) super.clone();
    }

    @Override
    public Color getColor() {
        return this.color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color == null ? DEFAULT_LEATHER_COLOR : color;
    }

    boolean hasColor() {
        return !DEFAULT_LEATHER_COLOR.equals(color);
    }

    @Override
    Builder<String, Object> serialize(Builder<String, Object> builder) {
        super.serialize(builder);

        if (this.hasColor()) {
            builder.put(COLOR.BUKKIT, color);
        }

        return builder;
    }

    @Override
    boolean equalsCommon(CraftMetaItem meta) {
        if (!super.equalsCommon(meta)) {
            return false;
        }
        if (meta instanceof CraftMetaLeatherArmor) {
            CraftMetaLeatherArmor that = (CraftMetaLeatherArmor) meta;

            return this.color.equals(that.color);
        }
        return true;
    }

    @Override
    boolean notUncommon(CraftMetaItem meta) {
        return super.notUncommon(meta) && (meta instanceof CraftMetaLeatherArmor || this.isLeatherArmorEmpty());
    }

    @Override
    int applyHash() {
        final int original;
        int hash = original = super.applyHash();
        if (this.hasColor()) {
            hash ^= this.color.hashCode();
        }
        return original != hash ? CraftMetaSkull.class.hashCode() ^ hash : hash;
    }
}
