package dev.breeze.settlements.xxxxxxreactions.entities;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import dev.breeze.settlements.xxxxxxreactions.config.ReactionConfig;
import dev.breeze.settlements.xxxxxxreactions.elements.Element;
import lombok.Getter;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
public final class DendroCore extends ItemEntity {

    private static final HashMap<UUID, DendroCore> DENDRO_CORE_UUID_MAP = new HashMap<>();

    private final double damage;

    public DendroCore(@Nonnull Location spawnLocation, double damage) {
        super(((CraftWorld) spawnLocation.getWorld()).getHandle(),
                spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
                CraftItemStack.asNMSCopy(new ItemStackBuilder(ReactionConfig.DENDRO_CORE_MATERIAL)
                        .setDisplayName("&aDendro Core")
                        .setLore(RandomUtil.randomString())
                        .build()));
        this.damage = damage;

        // Add to UUID map
        DENDRO_CORE_UUID_MAP.put(this.getUUID(), this);

        // Configure dendro core properties
        this.setGlowingTag(true);
        this.setPickUpDelay(20 * 60 * 20); // 20 minutes, it shouldn't be picked up
        this.age = 6000 - ReactionConfig.DENDRO_CORE_LIFESPAN_TICKS - 20;
        this.portalCooldown = 20 * 60 * 20; // 20 minutes, it shouldn't go across dimensions

        // Schedule delayed explosion
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), this::onExpire, ReactionConfig.DENDRO_CORE_LIFESPAN_TICKS);
    }

    /**
     * Called when the dendro core expires to explode it
     * - only called internally
     */
    private void onExpire() {
        // If dendro core is removed in other ways, don't explode
        if (this.isRemoved())
            return;

        World world = this.level.getWorld();
        Location location = new Location(world, this.getX(), this.getY(), this.getZ());

        // Damage entities
        for (Entity nearby : world.getNearbyEntities(location, ReactionConfig.BLOOM_RADIUS_HORIZONTAL,
                ReactionConfig.BLOOM_RADIUS_VERTICAL, ReactionConfig.BLOOM_RADIUS_HORIZONTAL)) {
            // Dendro cores should deal damage to ALL entities, including the damage source
            if (!(nearby instanceof LivingEntity livingEntity) || nearby instanceof ArmorStand)
                continue;

            // Apply damage
            livingEntity.damage(this.damage);
            new TextDisplay(Element.DENDRO.colorize("%.1f", this.damage), 20, nearby.getLocation().add(0, 2, 0));
        }

        // Summon melon slices
        for (int a = 0; a < ReactionConfig.BLOOM_MELON_SLICE_COUNT; a++) {
            Item melonSliceItem = world.dropItem(location, new ItemStackBuilder(ReactionConfig.BLOOM_MELON_SLICE_MATERIAL)
                    .appendLore(String.valueOf(RandomUtil.RANDOM.nextDouble()))
                    .build());
            melonSliceItem.setVelocity(new Vector(RandomUtil.RANDOM.nextDouble() - 0.5, 0.5, RandomUtil.RANDOM.nextDouble() - 0.5).multiply(0.5));
            melonSliceItem.setPickupDelay(20 * 60 * 20); // 20 minutes, it shouldn't be picked up
            melonSliceItem.setTicksLived(6000 - ReactionConfig.BLOOM_MELON_SLICE_LIFESPAN_TICKS);
        }

        // Display particles
        ParticleUtil.globalParticle(location, Particle.EXPLOSION_NORMAL, 10, 0, 0, 0, 0.1);

        // Play sound
        SoundUtil.playSoundPublic(location, Sound.BLOCK_GRASS_BREAK, 0.9f, 1.3f);

        // Remove dendro core
        this.remove(RemovalReason.KILLED);
    }

    private ArmorStand summonBloomArmorStand(boolean isGlowing) {
        World world = this.level.getWorld();
        Location location = new Location(world, this.getX(), this.getY(), this.getZ());
        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location.clone().add(0, -0.5, 0), EntityType.ARMOR_STAND);
        armorStand.getEquipment().setHelmet(new ItemStack(ReactionConfig.DENDRO_CORE_MATERIAL));
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setVisible(false);
        armorStand.setGlowing(isGlowing);
        return armorStand;
    }

    /**
     * Explodes violently in a bigger radius
     * - only called externally
     */
    public void burgeon(double burgeonDamageMultiplier) {
        World world = this.level.getWorld();
        Location location = new Location(world, this.getX(), this.getY(), this.getZ());

        // Remove dendro core
        this.remove(RemovalReason.KILLED);

        // Display reaction
        new TextDisplay("&cBurgeon", 20, location.clone().add(0, 2, 0));

        new BukkitRunnable() {
            double t = 3 * Math.PI;
            final double maxT = t + ((double) ReactionConfig.BURGEON_EXPLOSION_DELAY_TICKS) / 16;
            final ArmorStand as = summonBloomArmorStand(true);

            public void run() {
                // Explode & deal damage
                if (t >= maxT) {
                    as.remove();

                    // Deal damage to nearby entities
                    for (Entity nearby : world.getNearbyEntities(location, ReactionConfig.BURGEON_AOE_RADIUS_HORIZONTAL,
                            ReactionConfig.BURGEON_AOE_RADIUS_VERTICAL, ReactionConfig.BURGEON_AOE_RADIUS_HORIZONTAL)) {
                        if (!(nearby instanceof LivingEntity) || nearby instanceof ArmorStand)
                            continue;
                        // Apply damage
                        ((LivingEntity) nearby).damage(damage * burgeonDamageMultiplier);

                        // Display reaction
                        new TextDisplay(Element.DENDRO.colorize("%.1f", damage * burgeonDamageMultiplier), 20,
                                nearby.getLocation().add(0, 2, 0));
                    }

                    // Summon glistering melon slices
                    for (int a = 0; a < ReactionConfig.BURGEON_MELON_SLICE_COUNT; a++) {
                        Item melonSliceItem = world.dropItem(location, new ItemStackBuilder(ReactionConfig.BURGEON_MELON_SLICE_MATERIAL)
                                .appendLore(String.valueOf(RandomUtil.RANDOM.nextDouble()))
                                .build());
                        melonSliceItem.setVelocity(new Vector(RandomUtil.RANDOM.nextDouble() - 0.5, 0.35, RandomUtil.RANDOM.nextDouble() - 0.5));
                        melonSliceItem.setPickupDelay(20 * 60 * 20); // 20 minutes, it shouldn't be picked up
                        melonSliceItem.setTicksLived(6000 - ReactionConfig.BURGEON_MELON_SLICE_LIFESPAN_TICKS);
                    }

                    // Display particle
                    ParticleUtil.globalParticle(location, Particle.SMOKE_NORMAL, 70, 2, 1, 2, 0.2);
                    ParticleUtil.globalParticle(location, Particle.EXPLOSION_LARGE, 5, 1, 0.5, 1, 1);

                    // Play sound
                    SoundUtil.playSoundPublic(location, Sound.BLOCK_AZALEA_LEAVES_PLACE, 0.7f, 1.5f);
                    SoundUtil.playSoundPublic(location, Sound.ENTITY_GENERIC_EXPLODE, 0.05f, 2f);

                    this.cancel();
                }

                t += Math.PI / 16;
                float y = (float) Math.toRadians(t * t * 5);
                EulerAngle ea = new EulerAngle(0, y, 0);
                as.setHeadPose(ea);
                as.teleport(as.getLocation().add(0, 0.003, 0));
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1);
    }


    /**
     * Fires a homing missile at the nearest entity
     * - only called externally
     */
    public void hyperbloom(Entity source, double hyperbloomDamageMultiplier) {
        World world = this.level.getWorld();
        Location location = new Location(world, this.getX(), this.getY(), this.getZ());

        // Remove dendro core
        this.remove(RemovalReason.KILLED);

        // Display reaction
        new TextDisplay("&dHyperbloom", 20, location.clone().add(0, 2, 0));

        // Find the nearest targetable entity
        LivingEntity livingEntity = null;
        for (Entity nearby : world.getNearbyEntities(location, ReactionConfig.HYPERBLOOM_SEEK_RADIUS_HORIZONTAL,
                ReactionConfig.HYPERBLOOM_SEEK_RADIUS_VERTICAL, ReactionConfig.HYPERBLOOM_SEEK_RADIUS_HORIZONTAL)) {
            if (!(nearby instanceof LivingEntity nearbyLivingEntity) || nearby instanceof ArmorStand || nearby == source)
                continue;
            livingEntity = nearbyLivingEntity;
            break;
        }

        // No target found
        if (livingEntity == null) {
            // TODO: launch into the sky?
            ParticleUtil.globalParticle(location, Particle.COMPOSTER, 30, 0.2, 0.2, 0.2, 1);
            return;
        }

        // Set source & target
        LivingEntity homingTarget = livingEntity;
        Location destination = livingEntity.getLocation().add(0, 1, 0);

        // Randomly pick two points between the target & dendro core (Bézier curve)
        Location anchor1 = percentageOfLine(0.33, location, destination);
        Location anchor2 = percentageOfLine(0.67, location, destination);

        // Calculate duration needed to reach the original target
        final double maxDuration = LocationUtil.distance(location, destination) / ReactionConfig.HYPERBLOOM_SEEK_SPEED_PER_TICK;

        // Fire the dendro core to seek the target
        new BukkitRunnable() {
            int duration = 0;
            Location current = null;
            final ArmorStand as = summonBloomArmorStand(false);

            @Override
            public void run() {
                if ((current != null && current.distanceSquared(homingTarget.getLocation()) < 4)
                        || duration >= ReactionConfig.HYPERBLOOM_SEEK_TIMEOUT_TICK * 5) {
                    // Deal damage to nearby entities
                    assert current != null;
                    for (Entity nearby : world.getNearbyEntities(current, ReactionConfig.HYPERBLOOM_AOE_RADIUS_HORIZONTAL,
                            ReactionConfig.HYPERBLOOM_AOE_RADIUS_VERTICAL, ReactionConfig.HYPERBLOOM_AOE_RADIUS_HORIZONTAL)) {
                        // Does not deal self damage
                        if (!(nearby instanceof LivingEntity) || nearby instanceof ArmorStand || nearby == source)
                            continue;
                        // Apply damage
                        ((LivingEntity) nearby).damage(damage * hyperbloomDamageMultiplier);

                        // Display reaction
                        new TextDisplay(Element.DENDRO.colorize("%.1f", damage * hyperbloomDamageMultiplier), 20, nearby.getLocation().add(0, 2, 0));
                    }

                    // Display particle
                    ParticleUtil.globalParticle(current, Particle.SLIME, 50, 1, 0.6, 1, 1);

                    // Play sound
                    SoundUtil.playSoundPublic(current, Sound.BLOCK_AZALEA_LEAVES_PLACE, 0.7f, 1.5f);

                    // Cancel task
                    as.remove();
                    this.cancel();
                    return;
                }

                // Display particle trail
                current = LocationUtil.bezierCurve(duration / maxDuration, location, homingTarget.getLocation(), anchor1, anchor2);
                ParticleUtil.globalParticle(current, Particle.COMPOSTER, 1, 0, 0, 0, 0);

                duration += 3;
                as.teleport(LocationUtil.bezierCurve(duration / maxDuration, location, homingTarget.getLocation(), anchor1, anchor2).add(0, 1, 0));
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1L);
    }

    private static Location percentageOfLine(double ratio, Location source, Location target) {
        double x = source.getX() + RandomUtil.RANDOM.nextDouble() * (target.getX() - source.getX()) * ratio
                + RandomUtil.RANDOM.nextDouble() * ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET - ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET;
        double y = (source.getY() + target.getY()) / 2 + 10 + RandomUtil.RANDOM.nextDouble() * ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET
                - ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET;
        double z = source.getZ() + RandomUtil.RANDOM.nextDouble() * (target.getZ() - source.getZ()) * ratio
                + RandomUtil.RANDOM.nextDouble() * ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET - ReactionConfig.HYPERBLOOM_SEEK_MAX_OFFSET;
        return new Location(source.getWorld(), x, y, z);
    }


    public static List<DendroCore> getNearbyDendroCores(World world, Location location) {
        ArrayList<DendroCore> cores = new ArrayList<>();
        for (Entity item : world.getNearbyEntities(location, ReactionConfig.BLOOM_TRIGGER_RADIUS_HORIZONTAL,
                ReactionConfig.BLOOM_TRIGGER_RADIUS_VERTICAL, ReactionConfig.BLOOM_TRIGGER_RADIUS_HORIZONTAL,
                (e) -> e.getType() == EntityType.DROPPED_ITEM)) {
            if (!DENDRO_CORE_UUID_MAP.containsKey(item.getUniqueId()))
                continue;
            cores.add(DENDRO_CORE_UUID_MAP.get(item.getUniqueId()));
        }
        return cores;
    }

}
