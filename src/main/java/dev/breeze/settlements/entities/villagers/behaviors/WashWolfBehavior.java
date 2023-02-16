package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public final class WashWolfBehavior extends InteractAtEntityBehavior {

    private static final ItemStack WATER_BUCKET = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.WATER_BUCKET).build());
    private static final ItemStack SPONGE = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.SPONGE).build());

    private static final int MAX_WASH_DURATION = TimeUtil.seconds(5);
    private static final int MAX_DRY_DURATION = TimeUtil.seconds(1);

    @Nullable
    private Wolf targetWolf;

    private int washDuration;
    private boolean dryEffectPlayed;

    public WashWolfBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // The villager should own a wolf
                        VillagerMemoryType.OWNED_DOG, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.minutes(1), Math.pow(20, 2),
                TimeUtil.minutes(20), Math.pow(1.5, 2),
                5, 1,
                TimeUtil.seconds(30), MAX_WASH_DURATION + MAX_DRY_DURATION);

        this.targetWolf = null;
        this.washDuration = 0;
        this.dryEffectPlayed = false;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        if (this.targetWolf == null) {
            UUID wolfUuid = brain.getMemory(VillagerMemoryType.OWNED_DOG).get();
            this.targetWolf = (Wolf) self.level.getMinecraftWorld().getEntity(wolfUuid);

            // If wolf is not alive, reset memory
            if (this.targetWolf == null || !this.targetWolf.isAlive()) {
                self.getBrain().eraseMemory(VillagerMemoryType.OWNED_DOG);
                return false;
            }

            self.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, this.targetWolf);
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        return this.targetWolf != null && this.targetWolf.isAlive();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        // Set item in hand
        if (this.washDuration < MAX_WASH_DURATION) {
            self.setItemSlot(EquipmentSlot.MAINHAND, WATER_BUCKET);
            self.setDropChance(EquipmentSlot.MAINHAND, 0f);
        } else {
            self.setItemSlot(EquipmentSlot.MAINHAND, SPONGE);
            self.setDropChance(EquipmentSlot.MAINHAND, 0f);
        }

        if (this.targetWolf != null)
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetWolf, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        if (this.targetWolf == null)
            return;
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetWolf, 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        if (this.targetWolf == null)
            return;

        // Stop both from walking away
        self.getNavigation().stop();
        this.targetWolf.getNavigation().stop();

        // Display effects
        Location location = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getY() + 0.3, this.targetWolf.getZ());
        if (this.washDuration++ < MAX_WASH_DURATION) {
            // Wash the wolf
            this.displayWashEffects(location);
        } else {
            // Dry the wolf
            if (!this.dryEffectPlayed)
                this.displayDryEffects(location);
        }
    }

    private void displayWashEffects(Location location) {
        // Display effects every 5 ticks
        if (this.washDuration % 5 == 0) {
            // Set the wolf to wet
            try {
                if (this.targetWolf != null)
                    // ci = private boolean isWet
                    FieldUtils.writeField(this.targetWolf, "ci", true, true);
            } catch (IllegalAccessException e) {
                LogUtil.exception(e, "Encountered exception while setting isWet of Wolf to true!");
            }
            ParticleUtil.globalParticle(location.clone().add(0, 0.5, 0), Particle.WATER_SPLASH, 3, 0.5, 0.4, 0.5, 0.1);
        }

        // Play sound every 20 ticks
        if (this.washDuration % 20 == 0)
            SoundUtil.playSoundPublic(location, Sound.BLOCK_WATER_AMBIENT, 0.2F, 1.2F);
    }

    private void displayDryEffects(Location location) {
        ParticleUtil.globalParticle(location, Particle.CLOUD, 10, 0.2, 0.2, 0.2, 0.05);
        SoundUtil.playSoundPublic(location, Sound.BLOCK_LAVA_EXTINGUISH, 0.2F, 1.2F);
        this.dryEffectPlayed = true;
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.stop(level, self, gameTime);

        // Reset held item
        self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Remove wolf from interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.targetWolf = null;
        this.washDuration = 0;
        this.dryEffectPlayed = false;
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
