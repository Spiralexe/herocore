package net.herotale.herocore.impl.bridge;

import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridge that reads HeroCore resistance attributes and syncs them into
 * Hytale's native damage resistance system.
 * <p>
 * <b>Strategy:</b> HeroCore computes resistance values from its own attribute
 * system (primaries, modifiers, gear, buffs, level-based bonuses). This bridge
 * dynamically creates/updates {@link StaticModifier} entries on the player's
 * {@link EntityStatMap}, which Hytale's built-in {@code ArmorDamageReduction}
 * system reads during its damage calculation pipeline.
 * <p>
 * This approach means:
 * <ul>
 *   <li>Hytale's damage calculation stays untouched (maximum robustness)</li>
 *   <li>No need to hook damage events or interfere with internal calculations</li>
 *   <li>HeroCore controls the values, Hytale applies them</li>
 *   <li>Other mods see standard damage calculations</li>
 *   <li>Easy to debug (modifiers are visible on the entity stat map)</li>
 *   <li>Flat armor values (ARMOR attribute) can be integrated later without
 *       architectural changes</li>
 * </ul>
 * <p>
 * <b>Modifier naming convention:</b> {@code "herocore:physical_resistance_flat"},
 * {@code "herocore:physical_resistance_pct"}, etc. These keys allow clean removal
 * and won't collide with other plugin modifiers.
 *
 * <h3>Architecture</h3>
 * <pre>
 * ┌──────────────────────────┐
 * │     StatsComponent       │
 * │  PHYSICAL_RESISTANCE     │─ computed from VIT + gear + buffs + level
 * │  PHYSICAL_RESISTANCE_PCT │
 * │  PROJECTILE_RESISTANCE   │─ computed from DEX + gear + buffs
 * │  PROJECTILE_RESISTANCE_PCT│
 * │  FIRE_RESISTANCE         │─ computed from RESOLVE + gear + buffs
 * │  FIRE_RESISTANCE_PERCENT │
 * │  ARMOR (dormant)         │─ stored, not yet wired to gameplay
 * └───────────┬──────────────┘
 *             │ DefenseBridge.apply()
 *             ▼
 * ┌──────────────────────────┐
 * │  Hytale EntityStatMap    │
 * │  putModifier("herocore:  │─ StaticModifier(ADDITIVE/MULTIPLICATIVE)
 * │    physical_resist_flat") │
 * └───────────┬──────────────┘
 *             │ (native pipeline)
 *             ▼
 * ┌──────────────────────────┐
 * │  ArmorDamageReduction    │─ Reads ItemArmor + EntityEffect + stat modifiers
 * │  (Hytale FilterDamage)   │  damage = max(0, raw - flat) * max(0, 1 - pct)
 * └──────────────────────────┘
 * </pre>
 *
 * @see net.herotale.herocore.impl.bridge.AttackSpeedBridge
 * @see net.herotale.herocore.impl.bridge.MovementSpeedBridge
 */
public class DefenseBridge {

    private static final Logger LOG = Logger.getLogger(DefenseBridge.class.getName());

    // Modifier keys — unique per HeroCore, won't collide with other plugins
    private static final String PHYS_FLAT_KEY  = "herocore:physical_resistance_flat";
    private static final String PHYS_PCT_KEY   = "herocore:physical_resistance_pct";
    private static final String PROJ_FLAT_KEY  = "herocore:projectile_resistance_flat";
    private static final String PROJ_PCT_KEY   = "herocore:projectile_resistance_pct";
    private static final String FIRE_FLAT_KEY  = "herocore:fire_resistance_flat";
    private static final String FIRE_PCT_KEY   = "herocore:fire_resistance_pct";

    private final UUID entityUuid;

    public DefenseBridge(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    /**
     * Sync all HeroCore resistance attributes to the entity's Hytale stat map.
     * <p>
     * Call this after {@link StatsComponent} recalculates (e.g. on attribute change,
     * modifier add/remove, level up). The bridge reads current computed values for
     * all resistance attributes and pushes them as named modifiers on the Hytale
     * {@link EntityStatMap}.
     *
     * @param stats the entity's HeroCore stats component (post-recalculation)
     */
    public void apply(StatsComponent stats) {
        PlayerRef playerRef = Universe.get().getPlayer(entityUuid);
        if (playerRef == null) {
            LOG.fine(() -> String.format("DefenseBridge[%s]: missing PlayerRef", entityUuid));
            return;
        }

        EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) {
            LOG.fine(() -> String.format("DefenseBridge[%s]: EntityStatMap missing", entityUuid));
            return;
        }

        // Physical resistance
        applyResistanceModifiers(statMap,
                stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE),
                stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT),
                "Physical",
                PHYS_FLAT_KEY,
                PHYS_PCT_KEY);

        // Projectile resistance
        applyResistanceModifiers(statMap,
                stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE),
                stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT),
                "Projectile",
                PROJ_FLAT_KEY,
                PROJ_PCT_KEY);

        // Fire resistance
        applyResistanceModifiers(statMap,
                stats.getValue(RPGAttribute.FIRE_RESISTANCE),
                stats.getValue(RPGAttribute.FIRE_RESISTANCE_PERCENT),
                "Fire",
                FIRE_FLAT_KEY,
                FIRE_PCT_KEY);

        LOG.fine(() -> String.format(
                "DefenseBridge[%s]: synced resistances — phys=%.2f/%.1f%%, proj=%.2f/%.1f%%, fire=%.2f/%.1f%%",
                entityUuid,
                stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE),
                stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT) * 100,
                stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE),
                stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT) * 100,
                stats.getValue(RPGAttribute.FIRE_RESISTANCE),
                stats.getValue(RPGAttribute.FIRE_RESISTANCE_PERCENT) * 100));
    }

    /**
     * Remove all HeroCore defense modifiers from the entity's stat map.
     * <p>
     * Call on entity disconnect or when HeroCore components are cleaned up,
     * so Hytale's stat system returns to its base state.
     */
    public void remove() {
        PlayerRef playerRef = Universe.get().getPlayer(entityUuid);
        if (playerRef == null) return;

        EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) return;

        removeModifierSafe(statMap, "Health", PHYS_FLAT_KEY);
        removeModifierSafe(statMap, "Health", PHYS_PCT_KEY);
        removeModifierSafe(statMap, "Health", PROJ_FLAT_KEY);
        removeModifierSafe(statMap, "Health", PROJ_PCT_KEY);
        removeModifierSafe(statMap, "Health", FIRE_FLAT_KEY);
        removeModifierSafe(statMap, "Health", FIRE_PCT_KEY);

        LOG.fine(() -> String.format("DefenseBridge[%s]: removed all defense modifiers", entityUuid));
    }

    // ── Internal ─────────────────────────────────────────────────────

    /**
     * Apply a pair of flat + percent resistance modifiers for a given damage cause.
     * <p>
     * Uses {@link EntityStatMap#putModifier} with named keys so they can be
     * updated in-place on subsequent calls without accumulating stale entries.
     */
    private void applyResistanceModifiers(EntityStatMap statMap,
                                          double flatValue, double percentValue,
                                          String damageCauseName,
                                          String flatKey, String pctKey) {
        // Flat resistance: ADDITIVE modifier on MAX target
        // This is read by ArmorDamageReduction as flat damage reduction
        if (flatValue > 0) {
            StaticModifier flatMod = new StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE,
                    (float) flatValue);
            statMap.putModifier(getHealthStatIndex(statMap), flatKey, flatMod);
        } else {
            removeModifierSafe(statMap, getHealthStatIndex(statMap), flatKey);
        }

        // Percent resistance: MULTIPLICATIVE modifier
        // Represents percentage-based damage reduction (0.3 = 30% reduction)
        if (percentValue > 0) {
            StaticModifier pctMod = new StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE,
                    (float) percentValue);
            statMap.putModifier(getHealthStatIndex(statMap), pctKey, pctMod);
        } else {
            removeModifierSafe(statMap, getHealthStatIndex(statMap), pctKey);
        }
    }

    private int getHealthStatIndex(EntityStatMap statMap) {
        return com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType
                .getAssetMap().getIndex("Health");
    }

    private void removeModifierSafe(EntityStatMap statMap, String statName, String key) {
        int index = com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType
                .getAssetMap().getIndex(statName);
        removeModifierSafe(statMap, index, key);
    }

    private void removeModifierSafe(EntityStatMap statMap, int index, String key) {
        if (index != Integer.MIN_VALUE && statMap.get(index) != null) {
            statMap.removeModifier(index, key);
        }
    }
}
