package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TameWolfBehavior extends InteractAtEntityBehavior {

    private static final ItemStack BONE = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BONE).build());

    /**
     * The chance of the villager taming a wolf
     */
    private static final double TAME_CHANCE = 0.3;

    @Nullable
    private Wolf targetWolf;

    public TameWolfBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // The villager should not already have a wolf
                        VillagerMemoryType.OWNED_DOG, MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.minutes(1), Math.pow(20, 2),
                TimeUtil.hours(1), Math.pow(1.5, 2),
                5, TimeUtil.seconds(1),
                TimeUtil.seconds(20), TimeUtil.seconds(10));

        this.targetWolf = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetWolf == null) {
            // Check for nearby untamed wolves
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestWolf = target.stream().filter(e -> {
                if (e.getType() != EntityType.WOLF || !(e instanceof Wolf wolf))
                    return false;
                // Should not tame villager wolf
                if (e instanceof VillagerWolf)
                    return false;
                // Check if wolf is already owned/tamed
                return !wolf.isTame();
            }).findFirst();

            if (nearestWolf.isEmpty())
                return false;

            this.targetWolf = (Wolf) nearestWolf.get();
            self.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, nearestWolf.get());
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        if (this.targetWolf == null || !this.targetWolf.isAlive())
            return false;
        return this.targetWolf.getOwnerUUID() == null;
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        self.setItemSlot(EquipmentSlot.MAINHAND, BONE);
        self.setDropChance(EquipmentSlot.MAINHAND, 0f);

        if (this.targetWolf != null)
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetWolf, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        if (this.targetWolf == null)
            return;
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetWolf, 0.6F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.targetWolf == null)
            return;

        // Attempt to tame the wolf
        boolean successful = RandomUtil.RANDOM.nextDouble() < TAME_CHANCE;

        // Play effect
        Location villagerLoc = new Location(level.getWorld(), self.getX(), self.getY() + 1.8, self.getZ());
        ParticleUtil.globalParticle(villagerLoc, successful ? Particle.VILLAGER_HAPPY : Particle.VILLAGER_ANGRY, 3, 0.2, 0.2, 0.2, 0.1);
        SoundUtil.playSoundPublic(villagerLoc, successful ? Sound.ENTITY_VILLAGER_YES : Sound.ENTITY_VILLAGER_NO, 1.2f);

        Location wolfLoc = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getEyeY(), this.targetWolf.getZ());
        ParticleUtil.globalParticle(wolfLoc, successful ? Particle.HEART : Particle.SMOKE_NORMAL, successful ? 3 : 8, 0.2, 0.2, 0.2, 0);

        // Successful taming logic
        if (successful) {
            // Remove the vanilla wolf and spawn a VillagerWolf instead
            Location wolfLocation = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getY(), this.targetWolf.getZ());
            this.targetWolf.remove(Entity.RemovalReason.KILLED);
            this.targetWolf = null;

            // Spawn a VillagerWolf instead
            VillagerWolf villagerWolf = new VillagerWolf(wolfLocation);
            if (self instanceof BaseVillager baseVillager)
                villagerWolf.tameByVillager(baseVillager);

            // Set memory
            self.getBrain().setMemory(VillagerMemoryType.OWNED_DOG, Optional.of(villagerWolf.getUUID()));

            // Stop after taming
            this.stop(level, self, gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager self, long gameTime) {
        super.stop(level, self, gameTime);

        // Reset held item
        self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Remove interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.targetWolf = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetWolf != null;
    }

    @Override
    protected boolean isTargetReachable(Villager self) {
        return this.targetWolf != null && self.distanceToSqr(this.targetWolf) < this.getInteractRangeSquared();
    }

}
