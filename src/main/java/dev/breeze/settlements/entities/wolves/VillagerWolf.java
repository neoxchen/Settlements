package dev.breeze.settlements.entities.wolves;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import dev.breeze.settlements.entities.wolves.behaviors.SitBehaviorController;
import dev.breeze.settlements.entities.wolves.behaviors.TestWolfBehavior;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class VillagerWolf extends Wolf {

    public static final String ENTITY_TYPE = "settlements_wolf";
    public static final int GOAL_PRIORITY = 100;

    private static final int MAX_CHECK_SCHEDULE_COOLDOWN = TimeUtil.seconds(30);
    private int checkScheduleCooldown;

    /**
     * Constructor called when Minecraft tries to load the entity
     */
    public VillagerWolf(@Nonnull EntityType<? extends Wolf> entityType, @Nonnull Level level) {
        super(EntityType.WOLF, level); // TODO
        this.init();
    }

    /**
     * Constructor to spawn the villager in manually
     */
    public VillagerWolf(@Nonnull World world, @Nonnull Location location) {
        super(EntityType.WOLF, ((CraftWorld) world).getHandle());
        this.setPos(location.getX(), location.getY(), location.getZ());
        if (!this.level.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom wolf to world");
        }

        this.init();
    }

    private void init() {
        this.initPathfinderGoals();
        this.refreshBrain(this.level.getMinecraftWorld());
    }

    /**
     * Called before world load to build the entity type
     */
    public static EntityType.Builder<Entity> getEntityTypeBuilder() {
        return EntityType.Builder.of(VillagerWolf::new, MobCategory.CREATURE)
                .sized(0.6F, 0.85F)
                .clientTrackingRange(10);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        LogUtil.info("Loading custom wolf...");

        // TODO: restore any NBT data if needed
    }

    @Override
    public boolean save(CompoundTag nbt) {
        LogUtil.info("Saving custom wolf");
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

    /*
     * Brain-related methods
     */
    @Override
    public @Nonnull Brain<Wolf> getBrain() {
        return (Brain<Wolf>) super.getBrain();
    }

    @Override
    protected @Nonnull Brain.Provider<Wolf> brainProvider() {
        ImmutableList<MemoryModuleType<Wolf>> memoryTypes = new ImmutableList.Builder<MemoryModuleType<Wolf>>()
                .build();
        ImmutableList<SensorType<Sensor<Wolf>>> sensorTypes = new ImmutableList.Builder<SensorType<Sensor<Wolf>>>()
                .build();
        return Brain.provider(memoryTypes, sensorTypes);
    }

    @Override
    protected @Nonnull Brain<?> makeBrain(@Nonnull Dynamic<?> dynamic) {
        Brain<Wolf> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    public void refreshBrain(@NotNull ServerLevel level) {
        Brain<Wolf> brain = this.getBrain();

        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    @Override
    public void tick() {
        super.tick();

        // Check schedule if needed
        if (--this.checkScheduleCooldown < 0) {
            this.brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
            this.checkScheduleCooldown = MAX_CHECK_SCHEDULE_COOLDOWN;
        }

        // TODO: edit this?
        this.getBrain().tick((ServerLevel) this.level, this);
    }

    /**
     * Core components copied from parent class
     */
    private void registerBrainGoals(Brain<Wolf> brain) {
        LogUtil.info("Registering wolf brain goals");

        // Set activity schedule
        // TODO: do we need to refine this?
        Schedule wolfSchedule = new ScheduleBuilder(new Schedule())
                .changeActivityAt(10, Activity.IDLE) // villager idle
                .changeActivityAt(2000, Activity.WORK) // villager work
                .changeActivityAt(9000, Activity.IDLE) // villager meet
                .changeActivityAt(11000, Activity.PLAY) // villager idle
                .changeActivityAt(12000, Activity.REST) // villager rest
                .build();
        brain.setSchedule(wolfSchedule);

        // Register behaviors
        brain.addActivity(Activity.CORE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(1, new TestWolfBehavior("CORE")))
                .build());
        brain.addActivity(Activity.IDLE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, SitBehaviorController.stand()))
                .add(Pair.of(1, new TestWolfBehavior("IDLE")))
                .build());
        brain.addActivity(Activity.WORK, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, SitBehaviorController.stand()))
                .add(Pair.of(0, new TestWolfBehavior("WORK")))
                .build());
        brain.addActivity(Activity.PLAY, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, SitBehaviorController.stand()))
                .add(Pair.of(0, new TestWolfBehavior("PLAY")))
                .build());
        brain.addActivity(Activity.REST, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(3, SitBehaviorController.sit()))
                .add(Pair.of(1, new TestWolfBehavior("REST")))
                .build());

        // Set important activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    /*
     * Pathfinder goals
     */
    private void initPathfinderGoals() {
        // Add target
//        this.targetSelector.addGoal(GOAL_PRIORITY, new HurtByTargetGoal(this, Villager.class).setAlertOthers(VillagerWolf.class));

//        Class[] hostiles = new Class[]{Zombie.class, Pillager.class, Vindicator.class, Vex.class, Witch.class, Evoker.class, Illusioner.class, Ravager.class};
//        for (Class clazz : hostiles)
//            this.targetSelector.addGoal(GOAL_PRIORITY + 1, new NearestAttackableTargetGoal<>(this, clazz, true));
//        this.goalSelector.addGoal(GOAL_PRIORITY, new TossItemGoal(this, new TossItemGoal.ItemEntry[]{}, 6));
    }


}
