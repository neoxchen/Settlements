package dev.breeze.settlements.test;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

import javax.annotation.Nonnull;

public class TestCommandHandler implements CommandExecutor {

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            // Console command
            MessageUtil.sendMessage(sender, "Go away evil console!!!");
            return true;
        }

        // Admin-only command
        if (!p.isOp())
            return true;

        int length = args.length;
        MessageUtil.sendMessage(p, "Starting test execution...");
        if (length == 0) {
            Block block = p.getTargetBlockExact(100);
            if (block == null)
                return true;
            BaseVillager villager = new BaseVillager(p.getWorld(), block.getLocation().add(0, 1, 0), VillagerType.PLAINS);
            villager.setTarget(((CraftPlayer) p).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, true);
        } else {
            MessageUtil.sendMessage(p, "Invalid testing format!");
            return true;
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

}
