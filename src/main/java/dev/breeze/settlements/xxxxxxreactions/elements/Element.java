package dev.breeze.settlements.xxxxxxreactions.elements;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.xxxxxxreactions.config.ReactionEffects;
import dev.breeze.settlements.xxxxxxreactions.entities.DendroCore;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Getter
public enum Element {

    PYRO('4', "\uD83D\uDD25") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // Reactions: burning, burgeon, vaporize, melt, overload
            // TODO: might need to change the order of these as element might be consumed
            // Overload reaction
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.overload(source, target, 5); // TODO
            }

            // Vaporize reaction (amplifies damage)
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                rawDamage = ReactionEffects.vaporize(target, Element.PYRO, rawDamage);
            }

            // Melt reaction (amplifies damage)
            if (currentlyApplied.containsKey(Element.CRYO)) {
                rawDamage = ReactionEffects.melt(target, Element.PYRO, rawDamage);
            }

            // Burning reaction
            if (currentlyApplied.containsKey(Element.DENDRO)) {
                ReactionEffects.burning(target);
            }

            // Burgeon reaction (scan for nearby dendro cores)
            List<DendroCore> cores = DendroCore.getNearbyDendroCores(target.getWorld(), target.getLocation());
            for (DendroCore core : cores)
                core.burgeon(3); // TODO

            // TODO: elemental application system
            // Element has already been applied
            if (currentlyApplied.containsKey(this)) {
                // Refresh element (max of currently applied and newly applied)
                currentlyApplied.put(this, Math.max(currentlyApplied.get(this), amount));
            }
            // New element to be applied
            else {
                // Apply element
                currentlyApplied.put(this, amount);
            }

            // Save elements
            EntityElement.setAffectedElements(target, currentlyApplied);

            return rawDamage;
        }
    },
    HYDRO('9', "\uD83C\uDF27") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // Reactions: vaporize, frozen, electro-charged, bloom
            // TODO: might need to change the order of these as element might be consumed
            // Vaporize reaction (amplifies damage)
            if (currentlyApplied.containsKey(Element.PYRO)) {
                rawDamage = ReactionEffects.vaporize(target, Element.HYDRO, rawDamage);
            }

            // Frozen reaction
            if (currentlyApplied.containsKey(Element.CRYO)) {
                ReactionEffects.frozen(target);
            }

            // Electro-charged reaction
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.electroCharged(source, target, 5); // TODO
            }

            // Bloom reaction
            if (currentlyApplied.containsKey(Element.DENDRO)) {
                ReactionEffects.bloom(target, 5); // TODO
            }

            // TODO: elemental application system
            // Element has already been applied
            if (currentlyApplied.containsKey(this)) {
                // Refresh element (max of currently applied and newly applied)
                currentlyApplied.put(this, Math.max(currentlyApplied.get(this), amount));
            }
            // New element to be applied
            else {
                // Apply element
                currentlyApplied.put(this, amount);
            }

            // Save elements
            EntityElement.setAffectedElements(target, currentlyApplied);

            return rawDamage;
        }
    },
    ANEMO('3', "≈") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, final double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // TODO: might need to change the order of these as element might be consumed
            // Swirl reactions
            if (currentlyApplied.containsKey(Element.PYRO)) {
                ReactionEffects.swirl(source, target, Element.PYRO, 1, 5); // TODO
            }
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                // Hydro swirl deals no damage
                ReactionEffects.swirl(source, target, Element.HYDRO, 1, 0);
            }
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.swirl(source, target, Element.ELECTRO, 1, 5); // TODO
            }
            if (currentlyApplied.containsKey(Element.CRYO)) {
                ReactionEffects.swirl(source, target, Element.CRYO, 1, 5); // TODO
            }

            // Anemo element cannot be applied to the entity
            return rawDamage;
        }
    },
    ELECTRO('5', "⚡") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // Reactions: overload, superconduct, electro-charged, quicken, hyperbloom, aggravate
            // TODO: might need to change the order of these as element might be consumed
            // Overload reaction
            if (currentlyApplied.containsKey(Element.PYRO)) {
                ReactionEffects.overload(source, target, 5); // TODO
            }

            // Superconduct reaction
            if (currentlyApplied.containsKey(Element.CRYO)) {
                ReactionEffects.superconduct(source, target, 5); // TODO
            }

            // Electro-charged reaction
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                ReactionEffects.electroCharged(source, target, 5); // TODO
            }

            // Quicken reaction
            if (currentlyApplied.containsKey(Element.DENDRO)) {
                ReactionEffects.quicken(target);
            }

            // Aggravate reaction (amplifies damage)
            if (EntityElement.isQuickened(target)) {
                rawDamage = ReactionEffects.aggravate(target, rawDamage);
            }

            // Hyperbloom reaction (scan for nearby dendro cores)
            List<DendroCore> cores = DendroCore.getNearbyDendroCores(target.getWorld(), target.getLocation());
            for (DendroCore core : cores)
                core.hyperbloom(source, 3); // TODO

            // TODO: elemental application system
            // Element has already been applied
            if (currentlyApplied.containsKey(this)) {
                // Refresh element (max of currently applied and newly applied)
                currentlyApplied.put(this, Math.max(currentlyApplied.get(this), amount));
            }
            // New element to be applied
            else {
                // Apply element
                currentlyApplied.put(this, amount);
            }

            // Save elements
            EntityElement.setAffectedElements(target, currentlyApplied);

            return rawDamage;
        }
    },
    DENDRO('2', "✿") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // Reactions: bloom, burning, quicken, spread
            // TODO: might need to change the order of these as element might be consumed
            // Bloom reaction
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                ReactionEffects.bloom(target, 5); // TODO
            }

            // Burning reaction
            if (currentlyApplied.containsKey(Element.PYRO)) {
                ReactionEffects.burning(target); // TODO
            }

            // Quicken reaction
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.quicken(target);
            }

            // Spread reaction (amplifies damage)
            if (EntityElement.isQuickened(target)) {
                rawDamage = ReactionEffects.spread(target, rawDamage);
            }

            // TODO: elemental application system
            // Element has already been applied
            if (currentlyApplied.containsKey(this)) {
                // Refresh element (max of currently applied and newly applied)
                currentlyApplied.put(this, Math.max(currentlyApplied.get(this), amount));
            }
            // New element to be applied
            else {
                // Apply element
                currentlyApplied.put(this, amount);
            }

            // Save elements
            EntityElement.setAffectedElements(target, currentlyApplied);

            return rawDamage;
        }
    },
    CRYO('b', "❄") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double rawDamage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // Reactions: melt, frozen, superconduct
            // TODO: might need to change the order of these as element might be consumed
            // Melt reaction
            if (currentlyApplied.containsKey(Element.PYRO)) {
                ReactionEffects.melt(target, Element.CRYO, 5); // TODO
            }

            // Frozen reaction
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                ReactionEffects.frozen(target); // TODO
            }

            // Superconduct reaction
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.superconduct(source, target, 5); // TODO
            }

            // TODO: elemental application system
            // Element has already been applied
            if (currentlyApplied.containsKey(this)) {
                // Refresh element (max of currently applied and newly applied)
                currentlyApplied.put(this, Math.max(currentlyApplied.get(this), amount));
            }
            // New element to be applied
            else {
                // Apply element
                currentlyApplied.put(this, amount);
            }

            // Save elements
            EntityElement.setAffectedElements(target, currentlyApplied);

            return rawDamage;
        }
    },
    GEO('6', "⏹") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, final double damage) {
            HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(target);

            // TODO: might need to change the order of these as element might be consumed
            // Crystallize reactions
            if (currentlyApplied.containsKey(Element.PYRO)) {
                ReactionEffects.crystallize(target, Element.PYRO);
            }
            if (currentlyApplied.containsKey(Element.HYDRO)) {
                ReactionEffects.crystallize(target, Element.HYDRO);
            }
            if (currentlyApplied.containsKey(Element.ELECTRO)) {
                ReactionEffects.crystallize(target, Element.ELECTRO);
            }
            if (currentlyApplied.containsKey(Element.CRYO)) {
                ReactionEffects.crystallize(target, Element.CRYO);
            }

            // Geo element cannot be applied to the entity
            return damage;
        }
    },
    PHYSICAL('f', "⛏") {
        @Override
        public double applyElement(final Entity source, final Entity target, final double amount, double damage) {
            // Shatter reaction
            if (EntityElement.isFrozen(target)) {
                ReactionEffects.shatter(target, 5); // TODO
            }

            // Physical 'element' cannot be applied to the entity
            return damage;
        }
    };

    private static final String INFUSION_LORE_PREFIX = MessageUtil.translateColorCode("&7Infusion: ");

    private final String colorCode;
    private final String symbol;

    Element(char colorCode, String symbol) {
        this.colorCode = MessageUtil.translateColorCode("&%c", colorCode);
        this.symbol = symbol;
    }

    /**
     * Applies the current element to the target, triggering reactions if needed
     *
     * @param target the entity to apply the element to
     * @param amount the amount of element to be applied (weak=0.5, moderate=1, strong=2, super=4)
     * @param damage initial attack damage, used to calculate final damage (i.e. damage after reaction)
     * @return the amount of damage that should be dealt after the reaction has been applied
     */
    public abstract double applyElement(final Entity source, final Entity target, final double amount, double damage);

    /**
     * Attempts to get the infusion from the itemstack
     * - defaults to 'PHYSICAL' if no infusion is detected
     * - item must contain lore specified in the {{INFUSION_LORE_PREFIX}} variable
     */
    @Nonnull
    public static Element fromItemInfusion(ItemStack itemStack) {
        if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasLore())
            return PHYSICAL;

        Element element = PHYSICAL;
        List<String> loreLines = Objects.requireNonNull(itemStack.getItemMeta().getLore());
        for (String lore : loreLines) {
            if (!lore.startsWith(INFUSION_LORE_PREFIX))
                continue;
            String elementString = MessageUtil.stripColor(lore.replace(INFUSION_LORE_PREFIX, ""));
            try {
                element = Element.valueOf(elementString.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                element = PHYSICAL;
            }
        }
        return element;
    }

    @Nonnull
    public static Element randomElement() {
        return Element.values()[RandomUtil.RANDOM.nextInt(Element.values().length)];
    }

    /**
     * Colors message with the element's respective color
     */
    public String colorize(String format, Object... args) {
        return String.format(this.colorCode + format, args);
    }

    /**
     * Gets the symbol of the element, optionally coloring it
     */
    public String getSymbol(boolean colorize) {
        if (!colorize)
            return this.symbol;
        return this.colorize(this.symbol);
    }

    /**
     * Gets the material of the 'crystal' produced by the 'crystallize' reaction
     */
    public Material getCrystalMaterial() {
        return switch (this) {
            case ANEMO -> Material.CYAN_STAINED_GLASS;
            case GEO -> Material.YELLOW_STAINED_GLASS;
            case PYRO -> Material.RED_STAINED_GLASS;
            case CRYO -> Material.LIGHT_BLUE_STAINED_GLASS;
            case HYDRO -> Material.BLUE_STAINED_GLASS;
            case ELECTRO -> Material.MAGENTA_STAINED_GLASS;
            case DENDRO -> Material.LIME_STAINED_GLASS;
            case PHYSICAL -> Material.WHITE_STAINED_GLASS;
        };
    }

    /**
     * Gets the swirl colors [red, green, blue] for particles
     * - only works for PYRO, HYDRO, ELECTRO, and CRYO elements
     */
    public double[] getSwirlParticleColor() {
        return switch (this) {
            case PYRO -> new double[]{255 / 255D, 68 / 255D, 61 / 255D};
            case CRYO -> new double[]{61 / 255D, 255 / 255D, 249 / 255D};
            case HYDRO -> new double[]{61 / 255D, 103 / 255D, 255 / 255D};
            case ELECTRO -> new double[]{184 / 255D, 61 / 255D, 255 / 255D};
            default -> throw new IllegalArgumentException("Element %s cannot be swirled!".formatted(this.toString()));
        };
    }

}
