package dev.breeze.settlements.test;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public class MemoryEvent implements Listener {

    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item.getType() != Material.BONE)
            return;
        Entity entity = ((CraftEntity) event.getRightClicked()).getHandle();
        if (entity instanceof VillagerWolf wolf) {
            Brain<Wolf> brain = wolf.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.broadcast("&bMemories of wolf:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.broadcast("&b - " + entry.getKey().toString() + " : " + entry.getValue());
            MessageUtil.broadcast("&b - Owned by: " + wolf.getOwner() + " ( " + wolf.getOwnerUUID() + " )");
        } else if (entity instanceof Villager villager) {
            Brain<Villager> brain = villager.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.broadcast("&bMemories of villager:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.broadcast("&b - " + entry.getKey().toString() + " : " + entry.getValue());
        }

    }

}
