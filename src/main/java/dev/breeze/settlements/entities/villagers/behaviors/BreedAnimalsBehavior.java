package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BreedAnimalsBehavior extends InteractAtEntityBehavior {

    private static final ItemStack WHEAT = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.WHEAT).build());
    private static final ItemStack CARROT = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.CARROT).build());
    private static final ItemStack POTATO = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.POTATO).build());
    private static final ItemStack BEETROOT = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BEETROOT).build());
    private static final ItemStack WHEAT_SEEDS = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.WHEAT_SEEDS).build());
    private static final ItemStack BEETROOT_SEEDS = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BEETROOT_SEEDS).build());
    private static final ItemStack MELON_SEEDS = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.MELON_SEEDS).build());
    private static final ItemStack PUMPKIN_SEEDS = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.PUMPKIN_SEEDS).build());

    /**
     * Only breed animals when the number of nearby mobs is less than this number
     */
    private static final int MAX_NEARBY_MOBS_COUNT = 25;

    private static final Map<EntityType<? extends Animal>, ItemStack[]> BREED_ITEMS = Map.of(
            EntityType.COW, new ItemStack[]{WHEAT},
            EntityType.SHEEP, new ItemStack[]{WHEAT},
            EntityType.CHICKEN, new ItemStack[]{WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS},
            EntityType.PIG, new ItemStack[]{CARROT, POTATO, BEETROOT},
            EntityType.RABBIT, new ItemStack[]{CARROT}
    );

    @Nonnull
    private final Set<EntityType<? extends Animal>> canBreed;

    @Nullable
    private Animal targetAnimal1;
    @Nullable
    private Animal targetAnimal2;
    @Nonnull
    private BreedStatus status;
    @Nullable
    private ItemStack heldItem;

    public BreedAnimalsBehavior(@Nonnull Set<EntityType<? extends Animal>> canBreed) {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not be interacting with other targets
                        MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), Math.pow(30, 2),
                TimeUtil.minutes(5), Math.pow(2, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(2));

        this.canBreed = canBreed;

        this.targetAnimal1 = null;
        this.targetAnimal2 = null;

        this.status = BreedStatus.STANDBY;
        this.heldItem = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager self) {
        Brain<Villager> brain = self.getBrain();

        // If there are no nearby living entities, ignore
        if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            return false;

        // Check for nearby breedable animals
        List<LivingEntity> nearbyEntities = self.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();

        // Check nearby entity limit
        if (nearbyEntities.size() > MAX_NEARBY_MOBS_COUNT)
            return false;

        boolean matchFound = false;
        Map<EntityType<?>, Animal> nearbyAnimalsMap = new HashMap<>();
        for (LivingEntity entity : nearbyEntities) {
            if (entity == null || !entity.isAlive() || !(entity instanceof Animal animal))
                continue;

            // Check breeding requirements
            if (animal.getAge() != 0 || !animal.canFallInLove())
                continue;

            // Check if we can breed the entity type
            EntityType<?> type = animal.getType();
            if (!this.canBreed.contains(type))
                continue;

            // Check if we've seen a breedable entity of the same type before
            if (!nearbyAnimalsMap.containsKey(type)) {
                // Haven't seen, put current entity in cache
                nearbyAnimalsMap.put(type, animal);
                continue;
            }

            // Successfully found a pair, break the scan loop
            this.targetAnimal1 = nearbyAnimalsMap.get(type);
            this.targetAnimal2 = animal;
            matchFound = true;
            break;
        }

        return matchFound;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.start(level, self, gameTime);

        this.status = BreedStatus.FEEDING_FIRST;
        this.heldItem = RandomUtil.choice(BREED_ITEMS.get(this.targetAnimal1.getType()));
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager self, long gameTime) {
        return this.targetAnimal1 != null && this.targetAnimal1.isAlive()
                && this.targetAnimal2 != null && this.targetAnimal2.isAlive();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager self, long gameTime) {
        if (this.heldItem != null) {
            self.setItemSlot(EquipmentSlot.MAINHAND, this.heldItem);
            self.setDropChance(EquipmentSlot.MAINHAND, 0f);
        }

        Animal target = this.getCurrentTarget();
        if (target != null)
            self.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));

        // Make both animals follow the villager
        if (this.targetAnimal1 != null && this.targetAnimal1.getNavigation().isDone())
            this.targetAnimal1.getNavigation().moveTo(self, 1);
        if (this.targetAnimal2 != null && this.targetAnimal2.getNavigation().isDone())
            this.targetAnimal2.getNavigation().moveTo(self, 1);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        Animal target = this.getCurrentTarget();
        if (target == null)
            return;

        // Pathfind to the target animal
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 0.5F, 1));

        // Stop any movement of the target animal
        target.getNavigation().stop();
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager self, long gameTime) {
        // Safety check
        Animal target = this.getCurrentTarget();
        if (target == null)
            return;

        // Feed the animal
        target.setInLoveTime(TimeUtil.seconds(30));

        // Display effects
        Location location = new Location(level.getWorld(), target.getEyePosition().x, target.getEyePosition().y, target.getEyePosition().z);
        if (this.heldItem != null)
            ParticleUtil.itemBreak(location, CraftItemStack.asBukkitCopy(this.heldItem), 25, 0.3, 0.3, 0.3, 0.1);
        ParticleUtil.globalParticle(location, Particle.HEART, 5, 0.4, 0.4, 0.4, 1);
        SoundUtil.playSoundPublic(location, Sound.ENTITY_GENERIC_EAT, 1.2f);

        // Set status
        if (this.status == BreedStatus.FEEDING_FIRST) {
            this.status = BreedStatus.FEEDING_SECOND;
        } else if (this.status == BreedStatus.FEEDING_SECOND) {
            this.stop(level, self, gameTime);
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager self, long gameTime) {
        super.stop(level, self, gameTime);

        // Reset held item
        self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Remove golem from interaction memory
        self.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.targetAnimal1 = null;
        this.targetAnimal2 = null;

        this.status = BreedStatus.STANDBY;
        this.heldItem = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetAnimal1 != null && this.targetAnimal2 != null;
    }

    @Override
    protected boolean isTargetReachable(Villager self) {
        Animal target = this.getCurrentTarget();
        return target != null && self.distanceToSqr(target) < this.getInteractRangeSquared();
    }

    @Nullable
    private Animal getCurrentTarget() {
        return this.status == BreedStatus.FEEDING_FIRST ? this.targetAnimal1 : this.targetAnimal2;
    }

    private enum BreedStatus {
        /**
         * Not actively breeding
         */
        STANDBY,

        /**
         * Feeding the first animal
         */
        FEEDING_FIRST,

        /**
         * Feeding the second animal
         */
        FEEDING_SECOND
    }

}
