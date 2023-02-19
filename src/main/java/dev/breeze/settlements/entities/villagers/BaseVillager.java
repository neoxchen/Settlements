package dev.breeze.settlements.entities.villagers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.navigation.VillagerNavigation;
import dev.breeze.settlements.entities.villagers.sensors.VillagerSensorType;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.MessageUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.function.Predicate;

public class BaseVillager extends Villager {

    public static final String ENTITY_TYPE = "settlements_villager";

    @Getter
    @Setter
    private boolean defaultWalkTargetDisabled;

    /**
     * Constructor called when Minecraft tries to load the entity
     */
    public BaseVillager(@Nonnull EntityType<? extends Villager> entityType, @Nonnull Level level) {
        super(EntityType.VILLAGER, level); // TODO
        this.init();
    }

    /**
     * Constructor to spawn the villager in manually
     */
    public BaseVillager(@Nonnull Location location, @Nonnull VillagerType villagertype) {
        super(EntityType.VILLAGER, ((CraftWorld) location.getWorld()).getHandle(), villagertype);
        this.setPos(location.getX(), location.getY(), location.getZ());
        if (!this.level.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom villager to world");
        }

        this.init();
    }

    private void init() {
        // Configure navigation controller
        VillagerNavigation navigation = new VillagerNavigation(this, this.level);
        navigation.setCanOpenDoors(true);
        navigation.setCanFloat(true);
        this.navigation = navigation;

        // this.initPathfinderGoals();
        this.refreshBrain(this.level.getMinecraftWorld());

        this.defaultWalkTargetDisabled = false;
    }

    /**
     * Called before world load to build the entity type
     */
    public static EntityType.Builder<Entity> getEntityTypeBuilder() {
        return EntityType.Builder.of(BaseVillager::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        LogUtil.info("Loading custom villager...");

        // Load custom memories to brain
        VillagerMemoryType.load(nbt, this);
    }

    @Override
    public boolean save(@Nonnull CompoundTag nbt) {
        // There shouldn't be many things to do here
        LogUtil.info("Saving custom villager");
        return super.save(nbt);
    }

    /**
     * Saves villager data to the NBT tag
     */
    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        // IMPORTANT: save as custom ID to persist this entity
        nbt.putString("id", "minecraft:" + ENTITY_TYPE);
        nbt.putString("plugin", "Settlements");

        // Save custom memories
        VillagerMemoryType.save(nbt, this);
    }


    @Override
    protected @Nonnull Brain.Provider<Villager> brainProvider() {
        try {
            // cB = private static final ImmutableList<MemoryModuleType<?>>
            final ImmutableList<MemoryModuleType<?>> DEFAULT_MEMORY_TYPES = (ImmutableList<MemoryModuleType<?>>) FieldUtils.readStaticField(Villager.class,
                    "cB", true);
            // cC = private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>>
            final ImmutableList<SensorType<Sensor<Villager>>> DEFAULT_SENSOR_TYPES = (ImmutableList<SensorType<Sensor<Villager>>>)
                    FieldUtils.readStaticField(Villager.class, "cC", true);

            ImmutableList<MemoryModuleType<?>> customMemoryTypes = new ImmutableList.Builder<MemoryModuleType<?>>()
                    .addAll(DEFAULT_MEMORY_TYPES)
                    .add(VillagerMemoryType.FENCE_GATE_TO_CLOSE)
                    .add(VillagerMemoryType.OWNED_DOG)
                    .add(VillagerMemoryType.OWNED_CAT)
                    .add(VillagerMemoryType.WALK_DOG_TARGET)
                    .add(VillagerMemoryType.NEAREST_WATER_AREA)
                    .build();

            ImmutableList<SensorType<? extends Sensor<Villager>>> customSensorTypes = new ImmutableList.Builder<SensorType<? extends Sensor<Villager>>>()
                    .addAll(DEFAULT_SENSOR_TYPES)
                    .add(VillagerSensorType.NEAREST_WATER_AREA)
                    .build();

            return Brain.provider(customMemoryTypes, customSensorTypes);
        } catch (IllegalAccessException e) {
            LogUtil.exception(e, "Encountered exception when creating custom villager brain!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refreshBrain(@NotNull ServerLevel level) {
        Brain<Villager> brain = this.getBrain();

        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    /**
     * Core components copied from parent class
     */
    private void registerBrainGoals(Brain<Villager> brain) {
        VillagerProfession profession = this.getVillagerData().getProfession();

        // Register activities & behaviors
        brain.addActivity(Activity.CORE, CustomVillagerBehaviorPackages.getCorePackage(profession, 0.5F));
        brain.addActivity(Activity.IDLE, CustomVillagerBehaviorPackages.getIdlePackage(profession, 0.5F));

        if (this.isBaby()) {
            // If baby, register PLAY activities
            brain.addActivity(Activity.PLAY, CustomVillagerBehaviorPackages.getPlayPackage(0.5F));
        } else {
            // Otherwise, register WORK activities if job site is present
            brain.addActivityWithConditions(Activity.WORK, CustomVillagerBehaviorPackages.getWorkPackage(profession, 0.5F),
                    ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        // Register meet activities if meeting point is present
        brain.addActivityWithConditions(Activity.MEET, CustomVillagerBehaviorPackages.getMeetPackage(profession, 0.5F),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));

        // Register other activities
        brain.addActivity(Activity.REST, CustomVillagerBehaviorPackages.getRestPackage(profession, 0.5F));
        brain.addActivity(Activity.PANIC, CustomVillagerBehaviorPackages.getPanicPackage(profession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, CustomVillagerBehaviorPackages.getPreRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.RAID, CustomVillagerBehaviorPackages.getRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.HIDE, CustomVillagerBehaviorPackages.getHidePackage(profession, 0.5F));

        // Set schedule
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
        }

        // Configure activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }

    /*
     * Interaction methods
     */

    /**
     * Returns a predicate that determines whether the wolves that this villager owns should pick up an item or not
     * - only professions that can own wolves will be supported
     */
    public Predicate<ItemEntity> getFetchableItemsPredicate() {
        VillagerProfession profession = this.getProfession();
        return itemEntity -> {
            if (itemEntity == null)
                return false;
            ItemStack item = itemEntity.getItem();

            boolean wantsItem = false;
            if (profession == VillagerProfession.BUTCHER) {
                // Cow
                wantsItem = item.is(Items.BEEF);
                // Sheep
                wantsItem = wantsItem || item.is(Items.MUTTON);
                // Chicken
                wantsItem = wantsItem || item.is(Items.CHICKEN);
                // Pig
                wantsItem = wantsItem || item.is(Items.PORKCHOP);
                // Rabbit
                wantsItem = wantsItem || item.is(Items.RABBIT);
            } else if (profession == VillagerProfession.FARMER) {
                // Wheat
                wantsItem = item.is(Items.WHEAT) || item.is(Items.WHEAT_SEEDS);
                // Potato
                wantsItem = wantsItem || item.is(Items.POTATO) || item.is(Items.POISONOUS_POTATO);
                // Carrot
                wantsItem = wantsItem || item.is(Items.CARROT);
                // Beetroot
                wantsItem = wantsItem || item.is(Items.BEETROOT) || item.is(Items.BEETROOT_SEEDS);
                // Pumpkin
                wantsItem = wantsItem || item.is(Items.PUMPKIN);
                // Melon
                wantsItem = wantsItem || item.is(Items.MELON);
                // Sugarcane
                wantsItem = wantsItem || item.is(Items.SUGAR_CANE);
                // Egg
                wantsItem = wantsItem || item.is(Items.EGG);
            } else if (profession == VillagerProfession.LEATHERWORKER) {
                // Leather
                wantsItem = item.is(Items.LEATHER);
                // Rabbit hide
                wantsItem = wantsItem || item.is(Items.RABBIT_HIDE);
            } else if (profession == VillagerProfession.SHEPHERD) {
                // Wool (when sheared)
                wantsItem = item.is(ItemTags.WOOL);
            }

            // Return false on all other professions
            return wantsItem;
        };
    }

    /**
     * Returns a predicate that determines whether the villager want an item when trading with another villager
     */
    public Predicate<ItemEntity> getTradeItemsPredicate() {
        VillagerProfession profession = this.getProfession();
        return itemEntity -> {
            if (itemEntity == null)
                return false;
            ItemStack item = itemEntity.getItem();
            if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
                // TODO: do they want anything? potentially food?
            } else if (profession == VillagerProfession.ARMORER) {

            } else if (profession == VillagerProfession.BUTCHER) {

            } else if (profession == VillagerProfession.CARTOGRAPHER) {

            } else if (profession == VillagerProfession.CLERIC) {

            } else if (profession == VillagerProfession.FARMER) {

            } else if (profession == VillagerProfession.FISHERMAN) {

            } else if (profession == VillagerProfession.FLETCHER) {

            } else if (profession == VillagerProfession.LEATHERWORKER) {

            } else if (profession == VillagerProfession.LIBRARIAN) {

            } else if (profession == VillagerProfession.MASON) {

            } else if (profession == VillagerProfession.SHEPHERD) {

            } else if (profession == VillagerProfession.TOOLSMITH) {

            } else if (profession == VillagerProfession.WEAPONSMITH) {

            }

            // If no early returns, return false
            return false;
        };
    }

    /**
     * Receive an item from another entity
     * - e.g. from a tamed wolf fetching an item
     *
     * @return whether the receiving is successful
     */
    public boolean receiveItem(@Nonnull ItemEntity item) {
        if (!item.isAlive())
            return false;

        this.take(item, item.getItem().getCount());
        item.remove(RemovalReason.DISCARDED);
        MessageUtil.broadcast("&b[DEBUG] Villager received item " + item.getItem() + " - " + item.getItem().getCount() + "!");
        return true;
    }

    /*
     * Misc methods
     */
    public VillagerProfession getProfession() {
        return this.getVillagerData().getProfession();
    }

    public int getExpertiseLevel() {
        return this.getVillagerData().getLevel();
    }

}
