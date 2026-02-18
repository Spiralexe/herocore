package net.herotale.herocore.impl;

import java.util.logging.Level;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Resolves HeroCore's custom stat type indices at asset load time.
 * 
 * Mirrors the {@code DefaultEntityStatTypes} pattern from the server exactly.
 * Called during asset load to store the int indices of our custom stats, which
 * are then used by gameplay systems for O(1) lookup into EntityStatMap.
 * 
 * Custom stat types defined in HeroCore assets:
 * - herocore:crit_damage_multiplier (CRIT_DAMAGE_MULTIPLIER)
 * - herocore:crit_chance (CRIT_CHANCE)
 * - herocore:attack_damage (ATTACK_DAMAGE)
 * - herocore:spell_power (SPELL_POWER)
 * - herocore:physical_defense (PHYSICAL_DEFENSE)
 * - herocore:physical_defense_percent (PHYSICAL_DEFENSE_PERCENT)
 * - etc. (see builtin/entitystats/ JSON assets)
 */
public class HeroCoreStatTypes {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Indices resolved at asset load time
    
    // ── Crit & Damage Stats ────────────────────────────────────────
    private static int CRIT_DAMAGE_MULTIPLIER = Integer.MIN_VALUE;
    private static int CRIT_CHANCE = Integer.MIN_VALUE;
    private static int ATTACK_DAMAGE = Integer.MIN_VALUE;
    private static int SPELL_POWER = Integer.MIN_VALUE;
    
    // ── Defense Stats ──────────────────────────────────────────────
    private static int PHYSICAL_RESISTANCE = Integer.MIN_VALUE;
    private static int MAGIC_RESIST = Integer.MIN_VALUE;
    
    // ── Speed Stats ────────────────────────────────────────────────
    private static int ATTACK_SPEED = Integer.MIN_VALUE;
    private static int MOVE_SPEED = Integer.MIN_VALUE;
    private static int MINING_SPEED = Integer.MIN_VALUE;
    
    // ── Healing Stats ──────────────────────────────────────────────
    private static int HEALING_POWER = Integer.MIN_VALUE;

    /**
     * Called when assets are reloaded. Resolves all custom stat indices.
     * Follows the exact pattern from DefaultEntityStatTypes.
     */
    public static void update() {
        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        if (assetMap == null) {
            LOGGER.at(Level.WARNING).log("EntityStatType asset map not found - custom stat types will not load");
            return;
        }

        // Resolve indices from asset IDs
        // If a stat is not found, index remains Integer.MIN_VALUE and validate() will catch it
        
        // Crit & Damage
        CRIT_DAMAGE_MULTIPLIER = assetMap.getIndex("herocore:crit_damage_multiplier");
        CRIT_CHANCE = assetMap.getIndex("herocore:crit_chance");
        ATTACK_DAMAGE = assetMap.getIndex("herocore:attack_damage");
        SPELL_POWER = assetMap.getIndex("herocore:spell_power");
        
        // Defense
        PHYSICAL_RESISTANCE = assetMap.getIndex("herocore:physical_resistance");
        MAGIC_RESIST = assetMap.getIndex("herocore:magic_resist");
        
        // Speed
        ATTACK_SPEED = assetMap.getIndex("herocore:attack_speed");
        MOVE_SPEED = assetMap.getIndex("herocore:move_speed");
        MINING_SPEED = assetMap.getIndex("herocore:mining_speed");
        
        // Healing
        HEALING_POWER = assetMap.getIndex("herocore:healing_power");

        LOGGER.at(Level.FINE).log("HeroCore stat types resolved: " +
                "crit_dmg=%d, crit=%d, atk=%d, spell_pwr=%d, phys_res=%d, magic=%d, " +
                "atk_spd=%d, move_spd=%d, mining_spd=%d, heal_pwr=%d",
                CRIT_DAMAGE_MULTIPLIER, CRIT_CHANCE, ATTACK_DAMAGE,
                SPELL_POWER, PHYSICAL_RESISTANCE, MAGIC_RESIST,
                ATTACK_SPEED, MOVE_SPEED, MINING_SPEED, HEALING_POWER);
    }

    /**
     * Called during plugin start(). Validates that all stat types resolved successfully.
     * Throws exception if any index is still Integer.MIN_VALUE (asset not found).
     */
    public static void validate() {
        StringBuilder issues = new StringBuilder();
        if (CRIT_DAMAGE_MULTIPLIER == Integer.MIN_VALUE) issues.append("crit_damage_multiplier ");
        if (CRIT_CHANCE == Integer.MIN_VALUE) issues.append("crit_chance ");
        if (ATTACK_DAMAGE == Integer.MIN_VALUE) issues.append("attack_damage ");
        if (SPELL_POWER == Integer.MIN_VALUE) issues.append("spell_power ");
        if (PHYSICAL_RESISTANCE == Integer.MIN_VALUE) issues.append("physical_resistance ");
        if (MAGIC_RESIST == Integer.MIN_VALUE) issues.append("magic_resist ");
        if (ATTACK_SPEED == Integer.MIN_VALUE) issues.append("attack_speed ");
        if (MOVE_SPEED == Integer.MIN_VALUE) issues.append("move_speed ");
        if (MINING_SPEED == Integer.MIN_VALUE) issues.append("mining_speed ");
        if (HEALING_POWER == Integer.MIN_VALUE) issues.append("healing_power ");

        if (issues.length() > 0) {
            throw new IllegalStateException("HeroCore stat types failed to load: " + issues.toString());
        }

        LOGGER.at(Level.INFO).log("All HeroCore stat types validated successfully.");
    }

    // ── Accessors ────────────────────────────────────────────────────

    // Crit & Damage
    public static int getCritDamageMultiplier() { return CRIT_DAMAGE_MULTIPLIER; }
    public static int getCritChance() { return CRIT_CHANCE; }
    public static int getAttackDamage() { return ATTACK_DAMAGE; }
    public static int getSpellPower() { return SPELL_POWER; }
    
    // Defense
    public static int getPhysicalResistance() { return PHYSICAL_RESISTANCE; }
    public static int getMagicResist() { return MAGIC_RESIST; }
    
    // Speed
    public static int getAttackSpeed() { return ATTACK_SPEED; }
    public static int getMoveSpeed() { return MOVE_SPEED; }
    public static int getMiningSpeed() { return MINING_SPEED; }
    
    // Healing
    public static int getHealingPower() { return HEALING_POWER; }

    // ── Generic helpers (used by damage/heal systems) ────────────────

    /**
     * Resolve a stat type index by asset ID at runtime.
     * Returns -1 if the asset map is not loaded or the key is not found.
     *
     * @param assetId full stat asset ID, e.g. "herocore:attack_damage"
     * @return the integer index, or -1 if not found
     */
    public static int getIndex(String assetId) {
        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        if (assetMap == null) return -1;
        int idx = assetMap.getIndex(assetId);
        return idx;
    }

    /**
     * Read the current computed value of a stat from an entity's {@link EntityStatMap}.
     *
     * @param entityRef the entity reference
     * @param statIndex the stat type index (from {@link #getIndex(String)} or named accessors)
     * @return the current stat value, or 0 if the entity has no EntityStatMap
     */
    public static float getStatValue(Ref<EntityStore> entityRef, int statIndex) {
        if (entityRef == null || statIndex < 0) return 0f;
        EntityStatMap statMap = entityRef.getStore().getComponent(entityRef,
                EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) return 0f;
        var statValue = statMap.get(statIndex);
        if (statValue == null) return 0f;
        return statValue.get();
    }

    private HeroCoreStatTypes() {}
}
