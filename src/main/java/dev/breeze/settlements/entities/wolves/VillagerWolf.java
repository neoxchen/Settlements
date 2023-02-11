package dev.breeze.settlements.entities.wolves;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.behaviors.TestWolfBehavior;
import dev.breeze.settlements.entities.wolves.behaviors.WolfFetchItemBehavior;
import dev.breeze.settlements.entities.wolves.behaviors.WolfPlayWithEntityBehavior;
import dev.breeze.settlements.entities.wolves.behaviors.WolfSitBehaviorController;
import dev.breeze.settlements.entities.wolves.goals.WolfFollowOwnerGoal;
import dev.breeze.settlements.entities.wolves.goals.WolfSitWhenOrderedToGoal;
import dev.breeze.settlements.entities.wolves.sensors.WolfNearbyItemsSensor;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.UUID;

public class VillagerWolf extends Wolf {

    public static final String ENTITY_TYPE = "settlements_wolf";

    private static final int MAX_CHECK_SCHEDULE_COOLDOWN = TimeUtil.seconds(30);
    private int checkScheduleCooldown;

    @Getter
    @Setter
    private boolean isFetching;

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
        // TODO: improve navigation to ignore fences
//        this.navigation = new WolfNavigation(this, this.level);

        // Configure pathfinder goals
        this.initGoals();

        // Set wolf to be tamed by a random UID
        this.setTame(true);
        this.setOwnerUUID(UUID.randomUUID());
        this.setCollarColor(DyeColor.LIME);

        // Set step height to 1.5 (able to cross fences)
        this.maxUpStep = 1.5F;

        this.isFetching = false;
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

    private void initGoals() {
        // Remove selected default goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        // Add replacement goals
        this.goalSelector.addGoal(2, new WolfSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new WolfFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));

        // TODO: Add extra goals

        // Add target
//        this.targetSelector.addGoal(GOAL_PRIORITY, new HurtByTargetGoal(this, Villager.class).setAlertOthers(VillagerWolf.class));

//        Class[] hostiles = new Class[]{Zombie.class, Pillager.class, Vindicator.class, Vex.class, Witch.class, Evoker.class, Illusioner.class, Ravager.class};
//        for (Class clazz : hostiles)
//            this.targetSelector.addGoal(GOAL_PRIORITY + 1, new NearestAttackableTargetGoal<>(this, clazz, true));
//        this.goalSelector.addGoal(GOAL_PRIORITY, new TossItemGoal(this, new TossItemGoal.ItemEntry[]{}, 6));
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
        ImmutableList<MemoryModuleType<?>> memoryTypes = new ImmutableList.Builder<MemoryModuleType<?>>()
                .add(WolfFetchItemBehavior.NEARBY_ITEMS_MEMORY)
                .build();
        ImmutableList<SensorType<? extends Sensor<Wolf>>> sensorTypes = new ImmutableList.Builder<SensorType<? extends Sensor<Wolf>>>()
                .add(WolfNearbyItemsSensor.NEARBY_ITEMS_SENSOR)
                .build();
        return Brain.provider(memoryTypes, sensorTypes);
    }

    @Override
    protected @Nonnull Brain<?> makeBrain(@Nonnull Dynamic<?> dynamic) {
        Brain<Wolf> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    @Override
    public void tick() {
        super.tick();

        // Check schedule on a cooldown
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
        // TODO: Are there any additional core behaviors needed?
        brain.addActivity(Activity.CORE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
//                .add(Pair.of(1, new TestWolfBehavior("CORE")))
                .build());
        // TODO: Is the default wander/idle behavior enough?
        brain.addActivity(Activity.IDLE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, WolfSitBehaviorController.stand()))
//                .add(Pair.of(1, new TestWolfBehavior("IDLE")))
                .build());
        // TODO: Chase sheep behaviors?
        brain.addActivity(Activity.WORK, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, WolfSitBehaviorController.stand()))
                .add(Pair.of(1, new WolfFetchItemBehavior((itemEntity) -> true)))
                .build());
        brain.addActivity(Activity.PLAY, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(5, WolfSitBehaviorController.stand()))
                .add(Pair.of(0, new WolfPlayWithEntityBehavior()))
                .build());
        brain.addActivity(Activity.REST, new ImmutableList.Builder<Pair<Integer, BehaviorControl<Wolf>>>()
                .add(Pair.of(3, WolfSitBehaviorController.sit()))
                .add(Pair.of(1, new TestWolfBehavior("REST")))
                .build());

        // Set important activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    @Override
    public boolean isTame() {
        return true;
    }

    @Override
    @Nullable
    public BaseVillager getOwner() {
        // Return null if no owner
        if (this.getOwnerUUID() == null)
            return null;

        // Try to get owner entity
        Entity entity = this.level.getMinecraftWorld().getEntity(this.getOwnerUUID());
        if (!(entity instanceof BaseVillager villager))
            return null;

        return villager;
    }

    /*
     * Custom navigation for wolves to ignore fence gate
     */
    private static class WolfNavigation extends GroundPathNavigation {

        public WolfNavigation(Mob entity, Level world) {
            super(entity, world);
        }

        @Override
        protected PathFinder createPathFinder(int range) {
            this.nodeEvaluator = new WolfNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, range);
        }
    }

    private static class WolfNodeEvaluator extends WalkNodeEvaluator {
        @Override
        protected BlockPathTypes evaluateBlockPathType(BlockGetter world, boolean canOpenDoors, boolean canEnterOpenDoors, BlockPos pos, BlockPathTypes type) {
            return type == BlockPathTypes.FENCE ? BlockPathTypes.OPEN : super.evaluateBlockPathType(world, canOpenDoors, canEnterOpenDoors, pos, type);
        }
    }

}
