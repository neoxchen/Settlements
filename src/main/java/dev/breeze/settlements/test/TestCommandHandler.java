package dev.breeze.settlements.test;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        Block block = p.getTargetBlockExact(100);
        if (length == 0) {
            if (block == null)
                return true;
            BaseVillager villager = new BaseVillager(p.getWorld(), block.getLocation().add(0, 1, 0), VillagerType.PLAINS);
            VillagerWolf wolf = new VillagerWolf(p.getWorld(), block.getLocation().add(0, 1, 0));
//            wolf.setOwnerUUID(villager.getUUID());
        } else {
            MessageUtil.sendMessage(p, "Invalid testing format!");
            return true;
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

}
