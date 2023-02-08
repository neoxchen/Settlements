package dev.breeze.settlements.entities;

import com.mojang.serialization.Codec;
import dev.breeze.settlements.entities.behaviors.InteractWithFenceGate;
import dev.breeze.settlements.entities.goals.item_toss.VillagerTossItemEvent;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.BaseModuleController;
import dev.breeze.settlements.utils.LogUtil;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dev.breeze.settlements.entities.behaviors.InteractWithFenceGate.REGISTRY_KEY_FENCE_GATE_TO_CLOSE;

public class EntityModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
        // Register all entities
        try {
            this.registerEntities(Map.of(
                    BaseVillager.ENTITY_TYPE, BaseVillager.getEntityTypeBuilder()
            ));

            this.registerMemories();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            LogUtil.exception(e, "Exception encountered while registering modules!");
            return false;
        }

        return true;
    }

    @Override
    protected boolean load(JavaPlugin plugin, PluginManager pm) {
        pm.registerEvents(new VillagerTossItemEvent(), plugin);
        pm.registerEvents(new VillagerRestockEvent(), plugin);
        return true;
    }

    @Override
    protected void teardown() {
        // Do nothing (for now?)
    }

    /**
     * Registers all entities created in this module to the registry
     * - must be done before the world loads
     * - after registering, '/summon' works and the entity can persist restarts
     */
    private void registerEntities(Map<String, EntityType.Builder<Entity>> entityTypeMap) throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, NoSuchFieldException {
        // Get entity type registry
        CraftServer server = ((CraftServer) Bukkit.getServer());
        DedicatedServer dedicatedServer = server.getServer();
        WritableRegistry<EntityType<?>> entityTypeRegistry =
                (WritableRegistry<EntityType<?>>) dedicatedServer.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        // Unfreeze registry
        LogUtil.info("Unfreezing entity type registry (1/2)...");
        // l = private boolean frozen
        Field frozen = MappedRegistry.class.getDeclaredField("l");
        frozen.setAccessible(true);
        frozen.set(entityTypeRegistry, false);

        LogUtil.info("Unfreezing entity type registry (2/2)...");
        // m = private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
        Field unregisteredHolderMap = MappedRegistry.class.getDeclaredField("m");
        unregisteredHolderMap.setAccessible(true);
        unregisteredHolderMap.set(BuiltInRegistries.ENTITY_TYPE, new HashMap<>());

        // Unlock register method
        // a = private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder builder)
        Method register = EntityType.class.getDeclaredMethod("a", String.class, EntityType.Builder.class);
        register.setAccessible(true);

        // Build & register entities
        for (Map.Entry<String, EntityType.Builder<Entity>> entry : entityTypeMap.entrySet()) {
            register.invoke(null, entry.getKey(), entry.getValue());
        }

        // Re-freeze registry
        LogUtil.info("Re-freezing entity type registry...");
        BuiltInRegistries.ENTITY_TYPE.freeze();
        unregisteredHolderMap.set(BuiltInRegistries.ENTITY_TYPE, null);
    }

    /**
     * Registers all memory types created in this module to the registry
     * - must be done before the world loads
     */
    private void registerMemories() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        // Get memory module type registry
        CraftServer server = ((CraftServer) Bukkit.getServer());
        DedicatedServer dedicatedServer = server.getServer();
        WritableRegistry<MemoryModuleType<?>> registry =
                (WritableRegistry<MemoryModuleType<?>>) dedicatedServer.registryAccess().registryOrThrow(Registries.MEMORY_MODULE_TYPE);

        // Unfreeze registry
        LogUtil.info("Unfreezing memory module type registry...");
        // l = private boolean frozen
        Field frozen = MappedRegistry.class.getDeclaredField("l");
        frozen.setAccessible(true);
        frozen.set(registry, false);

        // Unlock register method
        // a = private static <U> MemoryModuleType<U> register(String id)
        Method register = MemoryModuleType.class.getDeclaredMethod("a", String.class);
        register.setAccessible(true);

        // Build & register memories
        InteractWithFenceGate.MEMORY_FENCE_GATE_TO_CLOSE = registerMemory(REGISTRY_KEY_FENCE_GATE_TO_CLOSE, null);

        // Re-freeze registry
        LogUtil.info("Re-freezing memory module type registry...");
        BuiltInRegistries.MEMORY_MODULE_TYPE.freeze();
    }

    private <U> MemoryModuleType<U> registerMemory(@Nonnull String id, @Nullable Codec<U> codec) {
        if (codec == null) {
            return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, new ResourceLocation(id), new MemoryModuleType<>(Optional.empty()));
        }
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, new ResourceLocation(id), new MemoryModuleType<>(Optional.of(codec)));
    }


}
