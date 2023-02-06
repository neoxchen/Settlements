package dev.breeze.settlements.xxxxxxreactions.entities;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import javax.annotation.Nonnull;

public final class TextDisplay {

    /**
     * Colored text to be displayed as the damage indicator
     */
    private final String coloredDisplay;

    /**
     * Duration in ticks that this display will exist for
     */
    private final int duration;

    private final Location location;
    private final ArmorStand armorStand;

    public TextDisplay(@Nonnull String coloredDisplay, int duration, @Nonnull Location location) {
        this.coloredDisplay = coloredDisplay;
        this.duration = duration;
        this.location = RandomUtil.addRandomOffset(location);

        // Initialize armor stand display
        assert this.location.getWorld() != null;

        this.armorStand = (ArmorStand) this.location.getWorld().spawnEntity(this.location.clone().add(0, -100, 0), EntityType.ARMOR_STAND);
        this.armorStand.setVisible(false);
        this.armorStand.setMarker(true);
        this.armorStand.setCustomName(MessageUtil.translateColorCode(this.coloredDisplay));
        this.armorStand.setCustomNameVisible(true);

        // Teleport after a short delay (otherwise teleport is ignored by the game)
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> this.armorStand.teleport(this.location), 2);

        // Schedule removal
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), this::remove, this.duration);
    }

    public void remove() {
        this.armorStand.remove();
    }
}
