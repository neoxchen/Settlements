package dev.breeze.settlements.guis;

import dev.breeze.settlements.utils.MessageUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public final class CustomInventory {

    private final Inventory bukkitInventory;
    private final CustomInventoryHolder inventoryHolder;

    public CustomInventory(int rows, String name) {
        this.inventoryHolder = new CustomInventoryHolder();
        this.bukkitInventory = Bukkit.createInventory(this.inventoryHolder, rows * 9, MessageUtil.translateColorCode(name));
        this.inventoryHolder.setInventory(this.bukkitInventory);
    }

    public void showToPlayer(Player player) {
        player.openInventory(this.bukkitInventory);
    }

    public void setBoarderItem(ItemStack border) {
        int size = this.bukkitInventory.getSize();
        for (int a = 0; a < 9; a++)
            this.bukkitInventory.setItem(a, border);
        for (int a = size - 9; a < size; a++)
            this.bukkitInventory.setItem(a, border);
        for (int a = 0; a < size; a += 9)
            this.bukkitInventory.setItem(a, border);
        for (int a = 8; a < size; a += 9)
            this.bukkitInventory.setItem(a, border);
    }

    /*
     * Inventory item setter methods
     */
    public List<ItemStack> addItems(ItemStack... items) {
        List<ItemStack> failed = new ArrayList<>();
        for (ItemStack item : items)
            failed.addAll(this.bukkitInventory.addItem(item).values());
        return failed;
    }

    public void setItems(ItemStack item, int... slots) {
        for (int slot : slots)
            this.bukkitInventory.setItem(slot, item);
    }

    public void setItems(ItemStack item, int from, int to) {
        for (int a = from; a < to; a++)
            this.bukkitInventory.setItem(a, item);
    }


    /*
     * Utility methods
     */
    public boolean isEmpty() {
        return this.bukkitInventory.firstEmpty() == -1;
    }


    public boolean containsMaterial(Material material) {
        return this.bukkitInventory.first(material) != -1;
    }

    public boolean containsItem(ItemStack item) {
        return this.bukkitInventory.first(item) != -1;
    }

    public boolean removeOneItem(Material toClear) {
        // Find the matching material
        int slot = this.bukkitInventory.first(toClear);
        if (slot == -1)
            return false;

        // Remove it
        ItemStack item = Objects.requireNonNull(this.bukkitInventory.getItem(slot));
        if (item.getAmount() > 1)
            item.setAmount(item.getAmount() - 1);
        else
            this.bukkitInventory.setItem(slot, null);
        return true;
    }

}

