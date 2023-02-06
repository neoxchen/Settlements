package dev.breeze.settlements.xxxxxxreactions.entities;

import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.sound.SoundUtil;
import dev.breeze.settlements.xxxxxxreactions.config.ReactionConfig;
import dev.breeze.settlements.xxxxxxreactions.elements.Element;
import lombok.Getter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;

@Getter
public final class GeoCrystal extends ItemEntity {

    private final Element element;
    private final double absorptionAmount;

    public GeoCrystal(@Nonnull Location spawnLocation, Element element, double absorptionAmount) {
        super(((CraftWorld) spawnLocation.getWorld()).getHandle(),
                spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
                CraftItemStack.asNMSCopy(new ItemStackBuilder(element.getCrystalMaterial())
                        .setDisplayName(element.colorize("%s crystal", element))
                        .setLore(RandomUtil.randomString())
                        .build()));

        this.element = element;
        this.absorptionAmount = absorptionAmount;

        // Configure crystal properties
        this.setPickUpDelay(20 * 60 * 20); // it shouldn't be picked up, use playerTouch() to handle +health
        this.age = 6000 - ReactionConfig.CRYSTALLIZE_LIFESPAN_TICKS;
    }

    /**
     * Apply crystallized absorption health to the player
     * - ignore regular playerTouch logic by not calling super
     */
    @Override
    public void playerTouch(Player player) {
        // Remove crystal
        this.remove(RemovalReason.KILLED);

        // Set absorption amount
        player.setAbsorptionAmount((float) Math.min(player.getAbsorptionAmount() + this.absorptionAmount, ReactionConfig.CRYSTALLIZE_MAX_HEALTH));

        // Play notification sound
        org.bukkit.entity.Player bukkitPlayer = Objects.requireNonNull(Bukkit.getPlayer(player.getUUID()));
        SoundUtil.playSound(bukkitPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.707f);
        SoundUtil.playSound(bukkitPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.059f);
    }

}
