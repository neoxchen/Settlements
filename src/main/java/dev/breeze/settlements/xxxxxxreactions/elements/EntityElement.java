package dev.breeze.settlements.xxxxxxreactions.elements;

import dev.breeze.settlements.Main;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.*;

public final class EntityElement {

    private static final Set<Entity> trackedEntities = new HashSet<>();

    public static void initElementDecayTimer() {
        new BukkitRunnable() {
            // Called every 0.25 second
            @Override
            public void run() {
                // Loop through all tracked entities
                Iterator<Entity> entityIterator = trackedEntities.iterator();
                while (entityIterator.hasNext()) {
                    Entity entity = entityIterator.next();

                    // Decrement all elements by 0.05 per 0.25 seconds
                    HashMap<Element, Double> affectedElements = getAffectedElements(entity);
                    affectedElements.replaceAll((element, amount) -> amount - 0.05);
                    affectedElements.values().removeIf(amount -> amount <= 0);

                    // Apply element
                    setAffectedElements(entity, affectedElements);

                    if (affectedElements.size() == 0)
                        entityIterator.remove();

                    displayAffectedElements(entity);
                }
            }
        }.runTaskTimerAsynchronously(Main.getPlugin(), 0L, 5L);
    }

    /**
     * Fetches the entity's affected elements from its metadata
     */
    @Nonnull
    public static HashMap<Element, Double> getAffectedElements(@Nonnull Entity entity) {
        // Return empty list if entity has no metadata
        if (!entity.hasMetadata(ElementConfig.METADATA_KEY))
            return new HashMap<>();

        HashMap<Element, Double> elements = new HashMap<>();
        String affectedElementString = entity.getMetadata(ElementConfig.METADATA_KEY).get(0).asString();

        // Return empty list if string is empty, and also remove metadata
        if (affectedElementString.isEmpty()) {
            entity.removeMetadata(ElementConfig.METADATA_KEY, Main.getPlugin());
            return new HashMap<>();
        }

        for (String elementString : affectedElementString.split(";")) {
            String[] elementData = elementString.split("=");
            elements.put(Element.valueOf(elementData[0].toUpperCase(Locale.ROOT)), Double.parseDouble(elementData[1]));
        }
        return elements;
    }

    /**
     * Convenient method of looking up if an entity is frozen
     */
    public static boolean isFrozen(@Nonnull Entity entity) {
        // TODO: change to when mob is actually frozen
        HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(entity);
        return currentlyApplied.containsKey(Element.HYDRO) && currentlyApplied.containsKey(Element.CRYO);
    }

    /**
     * Convenient method of looking up if an entity is quickened
     */
    public static boolean isQuickened(@Nonnull Entity entity) {
        // TODO: change to when mob is actually quickened
        HashMap<Element, Double> currentlyApplied = EntityElement.getAffectedElements(entity);
        return currentlyApplied.containsKey(Element.DENDRO) && currentlyApplied.containsKey(Element.ELECTRO);
    }

    public static void setAffectedElements(Entity entity, HashMap<Element, Double> affectedElements) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Element, Double> entry : affectedElements.entrySet())
            builder.append(entry.getKey().toString()).append('=').append(entry.getValue()).append(';');

        // Delete last ';' symbol if not empty
        if (!builder.isEmpty())
            builder.deleteCharAt(builder.length() - 1);

        // Save to metadata
        entity.setMetadata(ElementConfig.METADATA_KEY, new FixedMetadataValue(Main.getPlugin(), builder.toString()));

        // Track entity
        EntityElement.trackedEntities.add(entity);
    }

    public static void displayAffectedElements(Entity entity) {
        HashMap<Element, Double> affectedElements = EntityElement.getAffectedElements(entity);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Element, Double> entry : affectedElements.entrySet())
            builder.append(entry.getKey().getSymbol(true)).append(String.format("x%.2f", entry.getValue()));
        entity.setCustomName(builder.toString());
        entity.setCustomNameVisible(true);
    }

}
