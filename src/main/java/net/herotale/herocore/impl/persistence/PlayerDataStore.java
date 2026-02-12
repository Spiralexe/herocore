package net.herotale.herocore.impl.persistence;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory player data store for base attribute values.
 *
 * <p>This is the default implementation — stores data in memory only.
 * For production persistence, extend this to back with a database via HikariCP.</p>
 *
 * <p>Only base values are persisted. Modifiers must be re-applied by their
 * originating systems (equipment, buffs, zones, etc.) on load.</p>
 */
public class PlayerDataStore {

    private static final Logger LOG = Logger.getLogger(PlayerDataStore.class.getName());

    /** playerUUID → serialized JSON of base attribute values */
    private final Map<UUID, String> storedData = new ConcurrentHashMap<>();

    /**
     * Save all base values from a stats component.
     *
     * @param playerUuid the player
     * @param stats      the stats component to extract base values from
     */
    public void save(UUID playerUuid, StatsComponent stats) {
        Map<RPGAttribute, Double> baseValues = new EnumMap<>(RPGAttribute.class);
        for (RPGAttribute attr : RPGAttribute.values()) {
            double base = stats.getBase(attr);
            if (base != 0.0) {
                baseValues.put(attr, base);
            }
        }
        String json = AttributeSerializer.serialize(baseValues);
        storedData.put(playerUuid, json);
        LOG.fine(() -> "Saved attribute data for " + playerUuid);
    }

    /**
     * Load base values into a stats component.
     *
     * @param playerUuid the player
     * @param stats      the stats component to populate
     * @return true if data was found and loaded
     */
    public boolean load(UUID playerUuid, StatsComponent stats) {
        String json = storedData.get(playerUuid);
        if (json == null) return false;

        Map<RPGAttribute, Double> baseValues = AttributeSerializer.deserialize(json);
        for (Map.Entry<RPGAttribute, Double> entry : baseValues.entrySet()) {
            stats.setBase(entry.getKey(), entry.getValue());
        }
        LOG.fine(() -> "Loaded attribute data for " + playerUuid);
        return true;
    }

    /**
     * Remove stored data for a player.
     */
    public void remove(UUID playerUuid) {
        storedData.remove(playerUuid);
    }

    /**
     * Check if data exists for a player.
     */
    public boolean has(UUID playerUuid) {
        return storedData.containsKey(playerUuid);
    }
}
