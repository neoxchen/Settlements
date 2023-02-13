package dev.breeze.settlements.entities.villagers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breeze.settlements.entities.villagers.behaviors.*;
import dev.breeze.settlements.entities.villagers.goals.item_toss.TossItemGoal;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.navigation.VillagerNavigation;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.MessageUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BaseVillager extends Villager {

    public static final String ENTITY_TYPE = "settlements_villager";
    public static final int GOAL_PRIORITY = 100;

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
    public BaseVillager(@Nonnull World world, @Nonnull Location location, @Nonnull VillagerType villagertype) {
        super(EntityType.VILLAGER, ((CraftWorld) world).getHandle(), villagertype);
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
    public void load(CompoundTag nbt) {
        super.load(nbt);
        LogUtil.info("Loading custom villager...");

        // TODO: restore any NBT data if needed
    }

    @Override
    public boolean save(CompoundTag nbt) {
        // There shouldn't be many things to do here
        LogUtil.info("Saving custom villager");
        return super.save(nbt);
    }

    /**
     * Saves villager data to the NBT tag
     */
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        // IMPORTANT: save as custom ID to persist this entity
        nbt.putString("id", "minecraft:" + ENTITY_TYPE);

        // TODO: save any other important things
        nbt.putString("Plugin", "Settlements");

        CompoundTag villagerData = new CompoundTag();
        villagerData.putString("test1", "answer1");
        villagerData.putDouble("test2", 2);
        villagerData.putBoolean("test3", true);
        nbt.put("CustomVillagerData", villagerData);
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
                    .add(VillagerMemoryType.WALK_DOG_TARGET)
                    .build();

            return Brain.provider(customMemoryTypes, DEFAULT_SENSOR_TYPES);
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

        // Register core activities
        brain.addActivity(Activity.CORE, new ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>>()
                .addAll(VillagerGoalPackages.getCorePackage(profession, 0.5F))
                .addAll(this.getExtraCoreBehaviors())
                .build());

        // Register idle activities
        brain.addActivity(Activity.IDLE, new ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>>()
                .addAll(VillagerGoalPackages.getIdlePackage(profession, 0.5F))
                .addAll(this.getExtraIdleBehaviors())
                .build());

        // Register work activities if not baby
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
            brain.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
            brain.addActivityWithConditions(Activity.WORK, new ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>>()
                            .addAll(VillagerGoalPackages.getWorkPackage(profession, 0.5F))
                            .addAll(this.getExtraWorkBehaviors(profession))
                            .build(),
                    ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        // Register meet activities
        brain.addActivityWithConditions(Activity.MEET, new ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>>()
                .addAll(VillagerGoalPackages.getMeetPackage(profession, 0.5F))
                .addAll(this.getExtraMeetBehaviors(profession))
                .build(), Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));

        // Register other activities
        // - copied from the parent class
        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(profession, 0.5F));
        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(profession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(profession, 0.5F));

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }


    public List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getExtraCoreBehaviors() {
        return List.of(
                Pair.of(0, new InteractWithFenceGate())
        );
    }

    public List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getExtraIdleBehaviors() {
        return List.of(
                Pair.of(3, new WalkDogBehavior())
        );
    }

    public List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getExtraWorkBehaviors(VillagerProfession profession) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = new ArrayList<>();

        // Assign extra work behaviors based on profession
        if (profession == VillagerProfession.NONE) {
            // Do nothing?
        } else if (profession == VillagerProfession.ARMORER) {
            behaviors.add(Pair.of(10, new RepairIronGolemBehavior()));
        } else if (profession == VillagerProfession.BUTCHER) {

        } else if (profession == VillagerProfession.CARTOGRAPHER) {

        } else if (profession == VillagerProfession.CLERIC) {

        } else if (profession == VillagerProfession.FARMER) {

        } else if (profession == VillagerProfession.FISHERMAN) {

        } else if (profession == VillagerProfession.FLETCHER) {

        } else if (profession == VillagerProfession.LEATHERWORKER) {

        } else if (profession == VillagerProfession.LIBRARIAN) {

        } else if (profession == VillagerProfession.MASON) {

        } else if (profession == VillagerProfession.NITWIT) {

        } else if (profession == VillagerProfession.SHEPHERD) {
            behaviors.add(Pair.of(10, new ShearSheepBehavior()));
        } else if (profession == VillagerProfession.TOOLSMITH) {
            behaviors.add(Pair.of(10, new RepairIronGolemBehavior()));
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            behaviors.add(Pair.of(10, new RepairIronGolemBehavior()));
        }

        return behaviors;
    }

    public List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getExtraMeetBehaviors(VillagerProfession profession) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = new ArrayList<>();
        if (profession == VillagerProfession.BUTCHER) {
            behaviors.add(Pair.of(4, new FeedWolfBehavior()));
        }
        return behaviors;
    }

    /*
     * Pathfinder goals
     */
    private void initPathfinderGoals() {
        // Filter by profession & level
        List<TossItemGoal.ItemEntry> entries = new ArrayList<>();
        LogUtil.info("Profession: " + this.getProfession());
        if (this.getProfession() == VillagerProfession.NONE) {
            // Unemployed
        } else if (this.getProfession() == VillagerProfession.ARMORER) {
            entries.add(new TossItemGoal.ItemEntry(100, new ItemStack(Material.COAL), 3, 1, 5));
            // TODO: armor
            // TODO: shield
        } else if (this.getProfession() == VillagerProfession.BUTCHER) {
            for (Material material : new Material[]{
                    Material.CHICKEN, Material.PORKCHOP, Material.BEEF, Material.RABBIT, Material.MUTTON,
                    Material.COOKED_CHICKEN, Material.COOKED_PORKCHOP, Material.COOKED_BEEF, Material.COOKED_RABBIT, Material.COOKED_MUTTON,
            }) {
                entries.add(new TossItemGoal.ItemEntry(10, new ItemStack(material), 1.5, 0.3, -5));
            }

            entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(Material.KELP), 0.5, 0.01, -10));
            entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(Material.DRIED_KELP), 1, 0.1, 0));
            entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(Material.DRIED_KELP_BLOCK), 5, 1, 15));

            // TODO: sweet berries
        } else if (this.getProfession() == VillagerProfession.CARTOGRAPHER) {
            for (Material material : new Material[]{Material.PAPER, Material.MAP, Material.FILLED_MAP}) {
                entries.add(new TossItemGoal.ItemEntry(20, new ItemStack(material), 0.5, 0.01, -10));
            }
            entries.add(new TossItemGoal.ItemEntry(10, new ItemStack(Material.GLASS_PANE), 3, 0.3, 5));
            // TODO: more?
        } else if (this.getProfession() == VillagerProfession.CLERIC) {
            entries.add(new TossItemGoal.ItemEntry(50, new ItemStack(Material.ROTTEN_FLESH), 1, 0.1, 0));
            for (Material material : new Material[]{Material.REDSTONE, Material.LAPIS_LAZULI}) {
                entries.add(new TossItemGoal.ItemEntry(15, new ItemStack(material), 2, 0.5, 0));
            }
            for (Material material : new Material[]{Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK}) {
                entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(material), 5, 2, 20));
            }

            // TODO: glowstone
            // TODO: pearl
            // TODO: priest stuff
        } else if (this.getProfession() == VillagerProfession.FARMER) {
            for (Material material : new Material[]{Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS,
                    Material.PUMPKIN_SEEDS}) {
                entries.add(new TossItemGoal.ItemEntry(10, new ItemStack(material), 0.1, 0.01, -20));
            }
            for (Material material : new Material[]{Material.POTATO, Material.CARROT, Material.BEETROOT}) {
                entries.add(new TossItemGoal.ItemEntry(15, new ItemStack(material), 1, 0.1, 0));
            }
            for (Material material : new Material[]{Material.MELON, Material.PUMPKIN}) {
                entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(material), 4, 1.5, 10));
            }

            // TODO: pumpkin golem?
            // TODO: golden carrot / glistering melon regen?
        } else if (this.getProfession() == VillagerProfession.FISHERMAN) {
            for (Material material : new Material[]{Material.COD, Material.SALMON, Material.TROPICAL_FISH}) {
                entries.add(new TossItemGoal.ItemEntry(10, new ItemStack(material), 0.5, 0.1, -5));
            }

            // TODO: pufferfish
            // TODO: fishing rod?
        } else if (this.getProfession() == VillagerProfession.FLETCHER) {
            entries.add(new TossItemGoal.ItemEntry(100, new ItemStack(Material.FLINT), 0.5, 0.5, -3));

            // TODO: arrow & tipped arrows at enemy
            // TODO: at friendly
        } else if (this.getProfession() == VillagerProfession.LEATHERWORKER) {
            entries.add(new TossItemGoal.ItemEntry(90, new ItemStack(Material.LEATHER), 0.5, 0.1, 5));
            entries.add(new TossItemGoal.ItemEntry(10, new ItemStack(Material.SCUTE), 2, 1, 15));

            // TODO: leather armor
        } else if (this.getProfession() == VillagerProfession.LIBRARIAN) {
            entries.add(new TossItemGoal.ItemEntry(100, new ItemStack(Material.BOOK), 1.5, 0.8, 10));
            // TODO: toss enchanted books
            // TODO: cast spells
        } else if (this.getProfession() == VillagerProfession.MASON) {
            for (Material material : new Material[]{Material.STONE, Material.STONE_BRICKS, Material.ANDESITE, Material.POLISHED_ANDESITE,
                    Material.GRANITE, Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE, Material.DRIPSTONE_BLOCK,
                    Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.MAGENTA_TERRACOTTA,
                    Material.LIGHT_BLUE_TERRACOTTA,
                    Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA, Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
                    Material.LIGHT_GRAY_TERRACOTTA,
                    Material.CYAN_TERRACOTTA, Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA, Material.BROWN_TERRACOTTA,
                    Material.GREEN_TERRACOTTA,
                    Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA}) {
                entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(material), 2.5, 2, 10));
            }
        } else if (this.getProfession() == VillagerProfession.NITWIT) {
            // Almost does nothing
            entries.add(new TossItemGoal.ItemEntry(80, new ItemStack(Material.STONE_BUTTON), 0.01, 0.01, 0));
            entries.add(new TossItemGoal.ItemEntry(20, new ItemStack(Material.POLISHED_BLACKSTONE_BUTTON), 0.01, 0.01, 0));
        } else if (this.getProfession() == VillagerProfession.SHEPHERD) {
            for (Material material : new Material[]{Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
                    Material.LIGHT_BLUE_WOOL,
                    Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
                    Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL, Material.BROWN_WOOL, Material.GREEN_WOOL,
                    Material.RED_WOOL, Material.BLACK_WOOL}) {
                entries.add(new TossItemGoal.ItemEntry(5, new ItemStack(material), 1, 0.5, 0));
            }

            // TODO: wolf
        } else if (this.getProfession() == VillagerProfession.TOOLSMITH) {
            // TODO: throw tools
            // TODO: repair golems
        } else if (this.getProfession() == VillagerProfession.WEAPONSMITH) {
            // TODO: throw weapons
            // TODO: repair golems
        }

        entries.add(new TossItemGoal.ItemEntry(2, new ItemStack(Material.EMERALD), 3, 0.5, 0));
        entries.add(new TossItemGoal.ItemEntry(1, new ItemStack(Material.EMERALD_BLOCK), 5, 2, 10));

        // Add target
        this.targetSelector.addGoal(GOAL_PRIORITY, new HurtByTargetGoal(this, Villager.class).setAlertOthers(BaseVillager.class));

        Class[] hostiles = new Class[]{Zombie.class, Pillager.class, Vindicator.class, Vex.class,
                Witch.class, Evoker.class, Illusioner.class, Ravager.class};
        for (Class clazz : hostiles)
            this.targetSelector.addGoal(GOAL_PRIORITY + 1, new NearestAttackableTargetGoal<>(this, clazz, true));
        this.goalSelector.addGoal(GOAL_PRIORITY, new TossItemGoal(this, entries.toArray(new TossItemGoal.ItemEntry[0]), 6));
    }

    /*
     * Interaction methods
     */

    /**
     * Receive an item from another entity or something???
     *
     * @return whether the receving is successful????
     */
    public boolean receiveItem(ItemStack item) {
        MessageUtil.broadcast("&b[DEBUG] Villager received item " + item.getType() + "!");
        return false;
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
