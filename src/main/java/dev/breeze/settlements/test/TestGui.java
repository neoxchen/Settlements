package dev.breeze.settlements.test;

import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.guis.CustomInventoryHolder;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TestGui implements Listener {

    public static void open(Player player) {
        CustomInventory inventory = new CustomInventory(4, "owo uwu");
        inventory.addItems(new ItemStackBuilder(Material.APPLE).setAmount(30).setDisplayName("&6owo").build());
        inventory.addItems(new ItemStackBuilder(Material.GLASS).setAmount(33).setDisplayName("&cuwu").build());
        inventory.addItems(new ItemStackBuilder(Material.TORCH).setAmount(3).setDisplayName("&2owu").build());
        inventory.showToPlayer(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CustomInventoryHolder holder) || event.getRawSlot() < 0) {
            return;
        }

        // Prevent players from taking items out of the GUI.
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        // Clicking empty slots should do nothing
        if (clickedItem == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        player.getInventory().addItem(clickedItem);
    }

}
