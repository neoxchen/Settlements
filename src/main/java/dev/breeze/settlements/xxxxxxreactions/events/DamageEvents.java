package dev.breeze.settlements.xxxxxxreactions.events;

import dev.breeze.settlements.xxxxxxreactions.elements.Element;
import dev.breeze.settlements.xxxxxxreactions.elements.EntityElement;
import dev.breeze.settlements.xxxxxxreactions.entities.TextDisplay;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Ignore event if target is not insentient
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof ArmorStand)
            return;

        // Ignore event if damager is not player
        if (!(event.getDamager() instanceof Player player))
            return;

        // Get element of the damage
        ItemStack holding = player.getInventory().getItemInMainHand();
        Element element = Element.fromItemInfusion(holding);

        // Calculate damage from application amplifier
        double finalDamage = element.applyElement(player, target, 1, event.getDamage());
        event.setDamage(finalDamage);

        // Display element & damage
        new TextDisplay(element.colorize("%d &l%s", ((int) finalDamage), element.getSymbol()), 20, target.getLocation().add(0, 2.5, 0));
        EntityElement.displayAffectedElements(target);

        // Set no damage ticks
        target.setMaximumNoDamageTicks(1);
        target.setNoDamageTicks(1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDamage(EntityDamageByEntityEvent event) {
        // Ignore event if target is not insentient
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof ArmorStand)
            return;

        // Ignore event if damager is player
        if (event.getDamager() instanceof Player)
            return;

        Element element = Element.randomElement();

        // Calculate damage from application amplifier
        double finalDamage = element.applyElement(event.getDamager(), target, 1, event.getDamage());
        event.setDamage(finalDamage);

        // Display element & damage
        new TextDisplay(element.colorize("%d &l%s", ((int) finalDamage), element.getSymbol()), 20, target.getLocation().add(0, 2.5, 0));
        EntityElement.displayAffectedElements(target);

        // Set no damage ticks
        target.setMaximumNoDamageTicks(1);
        target.setNoDamageTicks(1);
    }

}
