package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.EntityModuleController;
import dev.breeze.settlements.utils.SafeRunnable;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ButcherAnimalsBehavior extends InteractAtEntityBehavior {

    private static final ItemStack IRON_AXE = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build());

    @Nonnull
    private final Map<EntityType<? extends Animal>, Integer> butcherAtLeastCount;

    @Nullable
    private Animal target;
    @Nullable
    private ArmorStand armorStand;

    private boolean animationStarted;
    private boolean stopBehavior;

    public ButcherAnimalsBehavior(@Nonnull Map<EntityType<? extends Animal>, Integer> butcherAtLeastCount) {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), Math.pow(30, 2),
                TimeUtil.minutes(2), Math.pow(2, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(2));

        this.butcherAtLeastCount = butcherAtLeastCount;
        this.target = null;
        this.armorStand = null;

        this.animationStarted = false;
        this.stopBehavior = false;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            return false;

        // Check for nearby animals
        List<LivingEntity> nearbyEntities = self.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();

        boolean matchFound = false;
        Map<EntityType<?>, Integer> nearbyAnimalsMap = new HashMap<>();
        for (LivingEntity entity : nearbyEntities) {
            if (entity == null || !entity.isAlive() || !(entity instanceof Animal animal))
                continue;

            // Check if we can butcher the entity type
            EntityType<?> type = animal.getType();
            if (!this.butcherAtLeastCount.containsKey(type))
                continue;

            // If no more than 2 of the same animal type, ignore
            nearbyAnimalsMap.put(type, nearbyAnimalsMap.getOrDefault(type, 0) + 1);
            if (nearbyAnimalsMap.get(type) <= this.butcherAtLeastCount.get(type))
                continue;

            // Ignore baby animals
            if (animal.isBaby())
                continue;

            // Successfully found an animal to butcher
            this.target = animal;
            matchFound = true;
            break;
        }

        return matchFound;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.start(level, self, gameTime);

        // Summon armor stand
        this.armorStand = new ArmorStand(level, self.getX(), self.getY(), self.getZ());
        this.armorStand.setInvisible(true);
        this.armorStand.setMarker(true);
        this.armorStand.setItemSlot(EquipmentSlot.MAINHAND, IRON_AXE);
        this.armorStand.setRightArmPose(new Rotations(-90, -40, 30));

        level.addFreshEntity(armorStand, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Add armor stand to temporary entities
        EntityModuleController.temporaryEntities.add(this.armorStand);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        return this.target != null && this.target.isAlive();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        if (this.stopBehavior) {
            this.stop(level, self, gameTime);
            return;
        }

        if (this.target != null)
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));

        // Teleport the armor stand to the villager
        if (this.armorStand != null)
            this.armorStand.moveTo(self.getX(), self.getY(), self.getZ(), self.yBodyRot, 0);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        if (this.target == null)
            return;

        // Pathfind to the target animal
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        // Check if we've already started the animation
        if (this.animationStarted)
            return;

        // Safety check
        if (this.target == null)
            return;

        // Display butcher animation (only once)
        // - activity stops upon animation complete
        this.animationStarted = true;

        // Freeze the mob
        this.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TimeUtil.seconds(10), 100, false, false), self,
                EntityPotionEffectEvent.Cause.ATTACK);

        new SafeRunnable() {
            final int ANIMATION_LENGTH = 7;
            float elapsed = 0;

            @Override
            public void safeRun() {
                // Check if animation has completed
                if (this.elapsed > ANIMATION_LENGTH) {
                    // We want to insta-kill the animal
                    target.hurt(DamageSource.mobAttack(self), Float.MAX_VALUE);
                    ParticleUtil.blockBreak(target.getBukkitEntity().getLocation(), Material.REDSTONE_BLOCK, 30, 0.5, 0.5, 0.5, 0.1);

                    stopBehavior = true;
                    this.cancel();
                    return;
                }

                // Swing axe
                if (armorStand != null) {
                    float progress = Math.min(this.elapsed / ANIMATION_LENGTH, 1);
                    armorStand.setRightArmPose(new Rotations(Mth.lerp(progress, -90, 10), -40, 30));
                }

                this.elapsed++;
            }
        }.runTaskTimer(Main.getPlugin(), 1L, 1L);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.stop(level, self, gameTime);

        // Remove armor stand
        if (this.armorStand != null) {
            this.armorStand.remove(Entity.RemovalReason.DISCARDED);
            EntityModuleController.temporaryEntities.remove(this.armorStand);
        }

        // Remove interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.target = null;
        this.armorStand = null;
        this.animationStarted = false;
        this.stopBehavior = false;
    }

    @Override
    protected boolean hasTarget() {
        return this.target != null;
    }

    @Override
    protected boolean isTargetReachable(Villager self) {
        return this.target != null && self.distanceToSqr(this.target) < this.getInteractRangeSquared();
    }

}
