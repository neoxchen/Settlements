package dev.breeze.settlements.xxxxxxreactions.config;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import dev.breeze.settlements.utils.sound.SoundUtil;
import dev.breeze.settlements.xxxxxxreactions.elements.Element;
import dev.breeze.settlements.xxxxxxreactions.elements.EntityElement;
import dev.breeze.settlements.xxxxxxreactions.entities.DendroCore;
import dev.breeze.settlements.xxxxxxreactions.entities.GeoCrystal;
import dev.breeze.settlements.xxxxxxreactions.entities.TextDisplay;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ReactionEffects {

    private static final Random RANDOM = new Random();

    /**
     * Swirl: group mobs to the target & apply swirled element
     * - does not amplify damage
     */
    public static void swirl(Entity source, Entity target, Element swirledElement, double swirledAmount, double swirlDamage) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());
        for (Entity nearby : world.getNearbyEntities(target.getLocation(), ReactionConfig.SWIRL_RADIUS_HORIZONTAL,
                ReactionConfig.SWIRL_RADIUS_VERTICAL, ReactionConfig.SWIRL_RADIUS_HORIZONTAL)) {
            // Does not deal self or target damage
            if (nearby == source || nearby == target)
                continue;

            if (nearby instanceof LivingEntity livingEntity && !(nearby instanceof ArmorStand)) {
                // Calculate swirl damage
                double finalSwirlDamage = swirledElement.applyElement(source, nearby, swirledAmount, swirlDamage);
                new TextDisplay(swirledElement.colorize(String.format("Swirl %.1f", swirlDamage)), 20, livingEntity.getLocation().add(0, 2, 0));
                if (finalSwirlDamage > 0)
                    livingEntity.damage(finalSwirlDamage);

                // Pull entities to the swirl center
                Vector pullDirection = target.getLocation().toVector().subtract(nearby.getLocation().toVector()).setY(0.3);
                nearby.setVelocity(pullDirection.normalize().multiply(0.5).setY(0.3));

                // TODO: remove debugging statements
                EntityElement.displayAffectedElements(target);
            }
        }

        // Display particles
        double[] color = swirledElement.getSwirlParticleColor();
        ArrayList<Location> circle = LocationUtil.getCircle(target.getLocation(), ReactionConfig.SWIRL_RADIUS_HORIZONTAL, 16);
        for (int a = 0; a < 3; a++)
            ParticleUtil.globalParticle(circle, Particle.SPELL_MOB, 0, color[0], color[1], color[2], 1);
        for (int a = 0; a < 20; a++)
            ParticleUtil.globalParticle(target.getLocation(), Particle.SPELL_MOB, 0, color[0], color[1], color[2], 1);

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.3f, 1.3f);
    }

    /**
     * Creates a "crystal" that gives absorption hearts when picked up
     * - does not amplify damage
     */
    public static void crystallize(Entity target, Element crystallizedElement) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());

        // Produces a crystal item
        GeoCrystal crystal = new GeoCrystal(target.getLocation(), crystallizedElement, RandomUtil.RANDOM.nextInt(3) + 1);
        ((CraftWorld) world).getHandle().addFreshEntity(crystal, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Display crystallize message
        new TextDisplay("&6Crystallize", 20, target.getLocation().add(0, 2, 0));

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.3f, 0f);
    }

    /**
     * Simple amplification of damage
     * - hydro trigger: 2x damage
     * - pyro trigger: 1.5x damage
     */
    public static double vaporize(Entity target, Element vaporizeElement, double rawDamage) {
        // Calculate raw damage
        double vaporizeDamage = rawDamage * (vaporizeElement == Element.HYDRO ? 2.0 : 1.5);
        new TextDisplay(vaporizeElement.colorize(String.format("Vaporize %.1f", vaporizeDamage)),
                20, target.getLocation().add(0, 2, 0));
        return vaporizeDamage;
    }

    /**
     * Simple amplification of damage
     * - pyro trigger: 2x damage
     * - cryo trigger: 1.5x damage
     */
    public static double melt(Entity target, Element meltElement, double rawDamage) {
        // Calculate raw damage
        double meltDamage = rawDamage * (meltElement == Element.PYRO ? 2.0 : 1.5);
        new TextDisplay(meltElement.colorize(String.format("Melt %.1f", meltDamage)),
                20, target.getLocation().add(0, 2, 0));
        return meltDamage;
    }

    /**
     * Applies slowness & weakness infinity on a target
     * - does not deal or amplify damage
     */
    public static void frozen(Entity target) {
        target.setVelocity(new Vector());

        if (target instanceof LivingEntity livingEntity) {
            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ReactionConfig.FROZEN_DURATION_TICKS, 100));
            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ReactionConfig.FROZEN_DURATION_TICKS, 100));
            livingEntity.setFreezeTicks(ReactionConfig.FROZEN_DURATION_TICKS);
        }

        // TODO: add to shatter

        // Display frozen message
        new TextDisplay("&bFrozen", 20, target.getLocation().add(0, 2, 0));

        // Display particles
        new BukkitRunnable() {
            int duration = 0;
            double dy = 0;
            int increment = 1;

            @Override
            public void run() {
                if (duration > ReactionConfig.FROZEN_DURATION_TICKS || target.isDead()) {
                    this.cancel();
                    return;
                }

                if (dy > 1)
                    increment = -1;
                else if (dy < -1)
                    increment = 1;
                dy += 0.1 * increment;

                List<Location> circle = LocationUtil.getCircle(target.getLocation().add(0, 1, 0), 0.5, 10);
                ParticleUtil.globalParticle(circle.get(duration % circle.size()).clone().add(0, dy, 0),
                        Particle.SNOWFLAKE, 3, 0, 0, 0, 0.05);
                duration++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1);

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 0.5f);
    }

    /**
     * Removes frozen status, dealing extra physical damage
     * - does not amplify damage
     */
    public static void shatter(Entity target, double shatterDamage) {
        // TODO: actually shatter it
        // Display shatter message
        new TextDisplay(String.format("&fShatter %.1f", shatterDamage), 20, target.getLocation().add(0, 2, 0));
        if (target instanceof LivingEntity livingEntity)
            livingEntity.damage(shatterDamage);

        // Display particles
        ParticleUtil.blockBreak(target.getLocation(), Material.PACKED_ICE, 100, 0.5, 1, 0.5, 0.05);

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 0.8f);
    }

    /**
     * Creates a mini explosion dealing pyro damage
     * - does not amplify damage
     */
    public static void overload(Entity source, Entity target, double overloadDamage) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());
        for (Entity nearby : world.getNearbyEntities(target.getLocation(), ReactionConfig.OVERLOAD_RADIUS_HORIZONTAL,
                ReactionConfig.OVERLOAD_RADIUS_VERTICAL, ReactionConfig.OVERLOAD_RADIUS_HORIZONTAL)) {
            // Does not deal self damage
            if (nearby == source)
                continue;

            if (nearby instanceof LivingEntity livingEntity && !(nearby instanceof ArmorStand)) {
                // Damage entity
                new TextDisplay(String.format("&cOverload %.1f", overloadDamage), 20, livingEntity.getLocation().add(0, 2, 0));
                livingEntity.damage(overloadDamage);

                // Push entities away from overload center
                Vector pullDirection = nearby.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.3);
                nearby.setVelocity(pullDirection.normalize().setY(0.3));
            }
        }

        // Display particles
        ParticleUtil.globalParticle(target.getLocation(), Particle.LAVA, 15, 0.3, 0.3, 0.3, 1);
        ParticleUtil.globalParticle(target.getLocation(), Particle.EXPLOSION_NORMAL, 3, 0.3, 0.3, 0.3, 1);

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3f, 0.8f);
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.3f, 0.5f);
    }

    /**
     * Deals cryo damage in a small AOE & inflicts physical RES debuff for a short while
     * - does not amplify damage
     */
    public static void superconduct(Entity source, Entity target, double superconductDamage) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());
        for (Entity nearby : world.getNearbyEntities(target.getLocation(), ReactionConfig.SUPERCONDUCT_RADIUS_HORIZONTAL,
                ReactionConfig.SUPERCONDUCT_RADIUS_VERTICAL, ReactionConfig.SUPERCONDUCT_RADIUS_HORIZONTAL)) {
            // Does not deal self damage
            if (nearby == source)
                continue;

            // Damage entity & apply effect
            if (nearby instanceof LivingEntity livingEntity && !(nearby instanceof ArmorStand)) {
                new TextDisplay(String.format("&dSuperconduct %.1f", superconductDamage), 20, livingEntity.getLocation().add(0, 2, 0));
                livingEntity.damage(superconductDamage);
                // TODO: replace this by RES decrease
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ReactionConfig.SUPERCONDUCT_DURATION_TICKS, 1));
            }
        }

        // Display particles
        ParticleUtil.globalParticle(target.getLocation(), Particle.SNOWFLAKE, 60, 0.3, 0.3, 0.3, 0.5);
        ParticleUtil.globalParticle(target.getLocation(), Particle.SPELL_WITCH, 20, 1, 0.2, 1, 1);

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.7f, 1.5f);
        SoundUtil.playSoundPublic(target.getLocation(), Sound.BLOCK_CAVE_VINES_BREAK, 1f, 0.5f);
    }

    /**
     * Deals electro damage to the target and nearby entities periodically
     * - does not amplify damage
     */
    public static void electroCharged(Entity source, Entity target, double electroChargedDamage) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());

        // Deal damage to self & nearby entities
        new BukkitRunnable() {
            int duration = 0;

            @Override
            public void run() {
                if (duration > ReactionConfig.ELECTRO_CHARGED_DAMAGE_COUNT || target.isDead()) {
                    this.cancel();
                    return;
                }

                // Deal damage to 4 nearby entities
                int count = 0;
                for (Entity nearby : world.getNearbyEntities(target.getLocation(), ReactionConfig.ELECTRO_CHARGED_RADIUS_HORIZONTAL,
                        ReactionConfig.ELECTRO_CHARGED_RADIUS_VERTICAL, ReactionConfig.ELECTRO_CHARGED_RADIUS_HORIZONTAL)) {
                    if (count >= 4)
                        break;
                    // Does not deal self damage
                    if (!(nearby instanceof LivingEntity) || nearby instanceof ArmorStand || nearby == source)
                        continue;

                    // Apply damage & slow
                    ((LivingEntity) nearby).damage(electroChargedDamage);
                    nearby.setVelocity(new Vector());

                    // Display reaction
                    new TextDisplay(String.format("&5Electro-Charged %.1f", electroChargedDamage), 20, nearby.getLocation().add(0, 2, 0));

                    // Display particle
                    List<Location> line = LocationUtil.getLine(target.getLocation().add(0, 1, 0), nearby.getLocation().add(0, 1, 0), 32);
                    ParticleUtil.globalParticle(line, Particle.CRIT_MAGIC, 3, 0, 0, 0, 0.05);
                    count++;
                }

                // Play sound
                SoundUtil.playSoundPublic(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1f, 1.5f);
                duration++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, ReactionConfig.ELECTRO_CHARGED_DAMAGE_DELAY_TICKS);
    }

    /**
     * Produces a dendro core
     * - does not deal or amplify damage
     */
    public static DendroCore bloom(Entity target, double dendroCoreDamage) {
        World world = Objects.requireNonNull(target.getLocation().getWorld());

        // Produces a dendro core item
        DendroCore dendroCore = new DendroCore(target.getLocation(), dendroCoreDamage);
        ((CraftWorld) world).getHandle().addFreshEntity(dendroCore, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Display bloom message
        new TextDisplay("&aBloom", 20, target.getLocation().add(0, 2, 0));

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.3f, 1.5f);

        return dendroCore;
    }

    /**
     * Launches a seeking dendro core at the closest entity, dealing AOE dendro damage
     * - it will not launch at the damager
     * - does not amplify damage
     */
    public static void hyperbloom(Entity source, DendroCore dendroCore, double multiplier) {
        dendroCore.hyperbloom(source, multiplier);
    }


    /**
     * Explodes violently, dealing massive dendro damage
     * - does not amplify damage
     */
    public static void burgeon(DendroCore dendroCore, double multiplier) {
        dendroCore.burgeon(multiplier);
    }


    /**
     * Ignites the entity, dealing fire damage periodically
     * - does not amplify damage
     */
    public static void burning(Entity target) {
        // TODO: consume elements
        target.setFireTicks(10 * 20);
    }


    /**
     * Renders the affected entity 'quickened', taking more electro & dendro damage
     * - does not deal or amplify damage
     */
    public static double quicken(Entity target) {
        // Set entity data as quickened

        // Play sound
        SoundUtil.playSoundPublic(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.05f, 2f);

        // Display quicken message
        new TextDisplay("&2Quicken", 20, target.getLocation().add(0, 2, 0));

        // Display particles for 10 seconds
        new BukkitRunnable() {
            int duration = 0;

            @Override
            public void run() {
                if (duration > ReactionConfig.QUICKEN_DURATION_TICKS || target.isDead()) {
                    // Remove entity data as quickened

                    this.cancel();
                    return;
                }

                ParticleUtil.globalParticle(target.getLocation().add(0, 0.5, 0),
                        Particle.CRIT_MAGIC, 10, 0.2, 0.5, 0.2, 0.05);
                duration += 10;
            }
        }.runTaskTimer(Main.getPlugin(), 0, 10L);

        // Damage is not changed
        return 1;
    }


    /**
     * Simple amplification of electro damage
     */
    public static double aggravate(Entity target, double rawDamage) {
        double aggravateDamage = rawDamage * 1.15;
        new TextDisplay(Element.ELECTRO.colorize(String.format("Aggravate %.1f", aggravateDamage)),
                20, target.getLocation().add(0, 2, 0));
        return aggravateDamage;
    }


    /**
     * Simple amplification of dendro damage
     */
    public static double spread(Entity target, double rawDamage) {
        double spreadDamage = rawDamage * 1.25;
        new TextDisplay(Element.DENDRO.colorize(String.format("Spread %.1f", spreadDamage)),
                20, target.getLocation().add(0, 2, 0));
        return spreadDamage;
    }

}
