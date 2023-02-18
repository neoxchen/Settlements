package dev.breeze.settlements.test;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.SafeRunnable;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

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
            new BaseVillager(block.getLocation().add(0, 1, 0), VillagerType.PLAINS);
        } else if (length == 1 && args[0].equals("wolf")) {
            BaseVillager villager = new BaseVillager(block.getLocation().add(0, 1, 0), VillagerType.PLAINS);
            VillagerWolf wolf = new VillagerWolf(block.getLocation().add(0, 1, 0));
            wolf.setOwnerUUID(villager.getUUID());
        } else if (length == 1 && args[0].equals("armorstand")) {
            if (block == null)
                return true;
            ServerLevel level = ((CraftWorld) p.getWorld()).getHandle();

            ArmorStand armorStand = new ArmorStand(level, block.getX(), block.getY() + 1, block.getZ());
            armorStand.setNoGravity(true);
            armorStand.setInvisible(true);
            armorStand.setRightArmPose(new Rotations(-90, 0, 0));
            armorStand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build()));
            armorStand.setItemSlot(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build()));
            level.addFreshEntity(armorStand, CreatureSpawnEvent.SpawnReason.CUSTOM);

            BaseVillager vil = new BaseVillager(block.getLocation().add(0, 1, 0), VillagerType.PLAINS);

            new SafeRunnable() {
                final int ANIMATION_LENGTH = 10;
                int elapsed = 0;

                @Override
                public void safeRun() {
                    if (this.elapsed > 10 * 20) {
                        armorStand.remove(Entity.RemovalReason.DISCARDED);
                        vil.remove(Entity.RemovalReason.DISCARDED);
                        this.cancel();
                        return;
                    }

                    armorStand.setLeftArmPose(new Rotations(-90 + 90 * ((float) (this.elapsed % ANIMATION_LENGTH) / ANIMATION_LENGTH), 0, 0));
                    armorStand.setRightArmPose(new Rotations(-90 + 90 * ((float) (this.elapsed % ANIMATION_LENGTH) / ANIMATION_LENGTH), 0, 0));

                    armorStand.moveTo(vil.getX(), vil.getY(), vil.getZ(), vil.yBodyRot, 0);
                    this.elapsed++;
                }
            }.runTaskTimer(Main.getPlugin(), 0, 1);
        } else if (length == 4 && args[0].equals("armorstand")) {
            if (block == null)
                return true;

            float pitch = Float.parseFloat(args[1]);
            float yaw = Float.parseFloat(args[2]);
            float roll = Float.parseFloat(args[3]);

            MessageUtil.broadcast("Pitch: %f, Yaw: %f, Roll: %f", pitch, yaw, roll);

            ServerLevel level = ((CraftWorld) p.getWorld()).getHandle();
            ArmorStand armorStand = new ArmorStand(level, block.getX(), block.getY() + 1, block.getZ());
            armorStand.setNoGravity(true);
//            armorStand.setInvisible(true);
            armorStand.setShowArms(true);

            armorStand.setRightArmPose(new Rotations(pitch, yaw, roll));

            armorStand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build()));
            level.addFreshEntity(armorStand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        } else {
            MessageUtil.sendMessage(p, "Invalid testing format!");
            return true;
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

}
