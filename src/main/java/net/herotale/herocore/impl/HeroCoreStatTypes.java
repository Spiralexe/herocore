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
 * - HeroCoreCritDamageMultiplier (CRIT_DAMAGE_MULTIPLIER)
 * - HeroCoreCritChance (CRIT_CHANCE)
 * - HeroCoreAttackDamage (ATTACK_DAMAGE)
 * - HeroCoreSpellPower (SPELL_POWER)
 * - HeroCorePhysicalResistance (PHYSICAL_RESISTANCE)
 * - HeroCorePhysicalResistancePercent (PHYSICAL_RESISTANCE_PERCENT)
 * - etc. (see Server/Entity/Stats/ JSON assets)
 */
public class HeroCoreStatTypes {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Indices resolved at asset load time
    
    // ── Crit & Damage Stats ────────────────────────────────────────
    private static int CRIT_DAMAGE_MULTIPLIER = Integer.MIN_VALUE;
    private static int CRIT_CHANCE = Integer.MIN_VALUE;
    private static int ATTACK_DAMAGE = Integer.MIN_VALUE;
    private static int SPELL_POWER = Integer.MIN_VALUE;
    private static int SPELL_CRIT_CHANCE = Integer.MIN_VALUE;
    private static int SPELL_CRIT_MULTIPLIER = Integer.MIN_VALUE;
    private static int HEAL_CRIT_CHANCE = Integer.MIN_VALUE;
    private static int HEAL_CRIT_MULTIPLIER = Integer.MIN_VALUE;
    
    // ── Defense Stats ──────────────────────────────────────────────
    private static int PHYSICAL_RESISTANCE = Integer.MIN_VALUE;
    private static int PHYSICAL_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int MAGIC_RESIST = Integer.MIN_VALUE;
    private static int ARMOR = Integer.MIN_VALUE;
    private static int DODGE_RATING = Integer.MIN_VALUE;
    private static int BLOCK_STRENGTH = Integer.MIN_VALUE;
    private static int SHIELD_STRENGTH = Integer.MIN_VALUE;
    
    // ── Elemental Resistance ───────────────────────────────────────
    private static int PROJECTILE_RESISTANCE = Integer.MIN_VALUE;
    private static int PROJECTILE_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int FIRE_RESISTANCE = Integer.MIN_VALUE;
    private static int FIRE_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int ICE_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int LIGHTNING_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int POISON_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    private static int ARCANE_RESISTANCE_PERCENT = Integer.MIN_VALUE;
    
    // ── Control / Threat ───────────────────────────────────────────
    private static int CC_RESISTANCE = Integer.MIN_VALUE;
    private static int DEBUFF_RESISTANCE = Integer.MIN_VALUE;
    private static int THREAT_GENERATION = Integer.MIN_VALUE;
    
    // ── Speed Stats ────────────────────────────────────────────────
    private static int ATTACK_SPEED = Integer.MIN_VALUE;
    private static int MOVE_SPEED = Integer.MIN_VALUE;
    private static int MINING_SPEED = Integer.MIN_VALUE;
    
    // ── Resources ──────────────────────────────────────────────────
    private static int MAX_HEALTH = Integer.MIN_VALUE;
    private static int HEALTH_REGEN = Integer.MIN_VALUE;
    private static int MAX_MANA = Integer.MIN_VALUE;
    private static int MANA_REGEN = Integer.MIN_VALUE;
    private static int MAX_STAMINA = Integer.MIN_VALUE;
    private static int STAMINA_REGEN = Integer.MIN_VALUE;
    
    // ── Healing Stats ──────────────────────────────────────────────
    private static int HEALING_POWER = Integer.MIN_VALUE;
    private static int BUFF_STRENGTH = Integer.MIN_VALUE;
    
    // ── Misc ───────────────────────────────────────────────────────
    private static int FALL_DAMAGE_REDUCTION = Integer.MIN_VALUE;

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
        CRIT_DAMAGE_MULTIPLIER = assetMap.getIndex("HeroCoreCritDamageMultiplier");
        CRIT_CHANCE = assetMap.getIndex("HeroCoreCritChance");
        ATTACK_DAMAGE = assetMap.getIndex("HeroCoreAttackDamage");
        SPELL_POWER = assetMap.getIndex("HeroCoreSpellPower");
        SPELL_CRIT_CHANCE = assetMap.getIndex("HeroCoreSpellCritChance");
        SPELL_CRIT_MULTIPLIER = assetMap.getIndex("HeroCoreSpellCritMultiplier");
        HEAL_CRIT_CHANCE = assetMap.getIndex("HeroCoreHealCritChance");
        HEAL_CRIT_MULTIPLIER = assetMap.getIndex("HeroCoreHealCritMultiplier");
        
        // Defense
        PHYSICAL_RESISTANCE = assetMap.getIndex("HeroCorePhysicalResistance");
        PHYSICAL_RESISTANCE_PERCENT = assetMap.getIndex("HeroCorePhysicalResistancePercent");
        MAGIC_RESIST = assetMap.getIndex("HeroCoreMagicResist");
        ARMOR = assetMap.getIndex("HeroCoreArmor");
        DODGE_RATING = assetMap.getIndex("HeroCoreDodgeRating");
        BLOCK_STRENGTH = assetMap.getIndex("HeroCoreBlockStrength");
        SHIELD_STRENGTH = assetMap.getIndex("HeroCoreShieldStrength");
        
        // Elemental Resistance
        PROJECTILE_RESISTANCE = assetMap.getIndex("HeroCoreProjectileResistance");
        PROJECTILE_RESISTANCE_PERCENT = assetMap.getIndex("HeroCoreProjectileResistancePercent");
        FIRE_RESISTANCE = assetMap.getIndex("HeroCoreFireResistance");
        FIRE_RESISTANCE_PERCENT = assetMap.getIndex("HeroCoreFireResistancePercent");
        ICE_RESISTANCE_PERCENT = assetMap.getIndex("HeroCoreIceResistancePercent");
        LIGHTNING_RESISTANCE_PERCENT = assetMap.getIndex("HeroCoreLightningResistancePercent");
        POISON_RESISTANCE_PERCENT = assetMap.getIndex("HeroCorePoisonResistancePercent");
        ARCANE_RESISTANCE_PERCENT = assetMap.getIndex("HeroCoreArcaneResistancePercent");
        
        // Control / Threat
        CC_RESISTANCE = assetMap.getIndex("HeroCoreCcResistance");
        DEBUFF_RESISTANCE = assetMap.getIndex("HeroCoreDebuffResistance");
        THREAT_GENERATION = assetMap.getIndex("HeroCoreThreatGeneration");
        
        // Speed
        ATTACK_SPEED = assetMap.getIndex("HeroCoreAttackSpeed");
        MOVE_SPEED = assetMap.getIndex("HeroCoreMoveSpeed");
        MINING_SPEED = assetMap.getIndex("HeroCoreMiningSpeed");
        
        // Resources
        MAX_HEALTH = assetMap.getIndex("HeroCoreMaxHealth");
        HEALTH_REGEN = assetMap.getIndex("HeroCoreHealthRegen");
        MAX_MANA = assetMap.getIndex("HeroCoreMaxMana");
        MANA_REGEN = assetMap.getIndex("HeroCoreManaRegen");
        MAX_STAMINA = assetMap.getIndex("HeroCoreMaxStamina");
        STAMINA_REGEN = assetMap.getIndex("HeroCoreStaminaRegen");
        
        // Healing
        HEALING_POWER = assetMap.getIndex("HeroCoreHealingPower");
        BUFF_STRENGTH = assetMap.getIndex("HeroCoreBuffStrength");
        
        // Misc
        FALL_DAMAGE_REDUCTION = assetMap.getIndex("HeroCoreFallDamageReduction");

        LOGGER.at(Level.FINE).log("HeroCore stat types resolved: " +
                "crit_dmg=%d, crit=%d, atk=%d, spell_pwr=%d, phys_res=%d, magic=%d, " +
                "atk_spd=%d, move_spd=%d, mining_spd=%d, heal_pwr=%d",
                CRIT_DAMAGE_MULTIPLIER, CRIT_CHANCE, ATTACK_DAMAGE,
                SPELL_POWER, PHYSICAL_RESISTANCE, MAGIC_RESIST,
                ATTACK_SPEED, MOVE_SPEED, MINING_SPEED, HEALING_POWER);
    }

    /**
     * Called during plugin start(). Validates that all stat types resolved successfully.
     * Logs warnings for any index that is still Integer.MIN_VALUE (asset not found).
     * 
     * NOTE: Some HeroCore stat types may not have corresponding Hytale native assets.
     * This is expected - those stats will have indices of Integer.MIN_VALUE and will
     * be skipped during derivation. Systems should check if a stat index is valid
     * (not Integer.MIN_VALUE) before attempting to write to it.
     */
    public static void validate() {
        StringBuilder missing = new StringBuilder();
        int missingCount = 0;
        
        if (CRIT_DAMAGE_MULTIPLIER == Integer.MIN_VALUE) { missing.append("crit_damage_multiplier "); missingCount++; }
        if (CRIT_CHANCE == Integer.MIN_VALUE) { missing.append("crit_chance "); missingCount++; }
        if (ATTACK_DAMAGE == Integer.MIN_VALUE) { missing.append("attack_damage "); missingCount++; }
        if (SPELL_POWER == Integer.MIN_VALUE) { missing.append("spell_power "); missingCount++; }
        if (SPELL_CRIT_CHANCE == Integer.MIN_VALUE) { missing.append("spell_crit_chance "); missingCount++; }
        if (SPELL_CRIT_MULTIPLIER == Integer.MIN_VALUE) { missing.append("spell_crit_multiplier "); missingCount++; }
        if (HEAL_CRIT_CHANCE == Integer.MIN_VALUE) { missing.append("heal_crit_chance "); missingCount++; }
        if (HEAL_CRIT_MULTIPLIER == Integer.MIN_VALUE) { missing.append("heal_crit_multiplier "); missingCount++; }
        if (PHYSICAL_RESISTANCE == Integer.MIN_VALUE) { missing.append("physical_resistance "); missingCount++; }
        if (PHYSICAL_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("physical_resistance_percent "); missingCount++; }
        if (MAGIC_RESIST == Integer.MIN_VALUE) { missing.append("magic_resist "); missingCount++; }
        if (ARMOR == Integer.MIN_VALUE) { missing.append("armor "); missingCount++; }
        if (DODGE_RATING == Integer.MIN_VALUE) { missing.append("dodge_rating "); missingCount++; }
        if (BLOCK_STRENGTH == Integer.MIN_VALUE) { missing.append("block_strength "); missingCount++; }
        if (SHIELD_STRENGTH == Integer.MIN_VALUE) { missing.append("shield_strength "); missingCount++; }
        if (PROJECTILE_RESISTANCE == Integer.MIN_VALUE) { missing.append("projectile_resistance "); missingCount++; }
        if (PROJECTILE_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("projectile_resistance_percent "); missingCount++; }
        if (FIRE_RESISTANCE == Integer.MIN_VALUE) { missing.append("fire_resistance "); missingCount++; }
        if (FIRE_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("fire_resistance_percent "); missingCount++; }
        if (ICE_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("ice_resistance_percent "); missingCount++; }
        if (LIGHTNING_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("lightning_resistance_percent "); missingCount++; }
        if (POISON_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("poison_resistance_percent "); missingCount++; }
        if (ARCANE_RESISTANCE_PERCENT == Integer.MIN_VALUE) { missing.append("arcane_resistance_percent "); missingCount++; }
        if (CC_RESISTANCE == Integer.MIN_VALUE) { missing.append("cc_resistance "); missingCount++; }
        if (DEBUFF_RESISTANCE == Integer.MIN_VALUE) { missing.append("debuff_resistance "); missingCount++; }
        if (THREAT_GENERATION == Integer.MIN_VALUE) { missing.append("threat_generation "); missingCount++; }
        if (ATTACK_SPEED == Integer.MIN_VALUE) { missing.append("attack_speed "); missingCount++; }
        if (MOVE_SPEED == Integer.MIN_VALUE) { missing.append("move_speed "); missingCount++; }
        if (MINING_SPEED == Integer.MIN_VALUE) { missing.append("mining_speed "); missingCount++; }
        if (MAX_HEALTH == Integer.MIN_VALUE) { missing.append("max_health "); missingCount++; }
        if (HEALTH_REGEN == Integer.MIN_VALUE) { missing.append("health_regen "); missingCount++; }
        if (MAX_MANA == Integer.MIN_VALUE) { missing.append("max_mana "); missingCount++; }
        if (MANA_REGEN == Integer.MIN_VALUE) { missing.append("mana_regen "); missingCount++; }
        if (MAX_STAMINA == Integer.MIN_VALUE) { missing.append("max_stamina "); missingCount++; }
        if (STAMINA_REGEN == Integer.MIN_VALUE) { missing.append("stamina_regen "); missingCount++; }
        if (HEALING_POWER == Integer.MIN_VALUE) { missing.append("healing_power "); missingCount++; }
        if (BUFF_STRENGTH == Integer.MIN_VALUE) { missing.append("buff_strength "); missingCount++; }
        if (FALL_DAMAGE_REDUCTION == Integer.MIN_VALUE) { missing.append("fall_damage_reduction "); missingCount++; }

        if (missingCount > 0) {
            LOGGER.at(Level.WARNING).log(
                "HeroCore stat types: %d/%d failed to resolve from Hytale asset system. " +
                "These stats will be skipped during derivation. Missing assets: %s" +
                "To add support for these stats, define them in Hytale's entity stat asset files " +
                "or use Hytale native stat types where available.",
                missingCount, 48, missing.toString());
        } else {
            LOGGER.at(Level.INFO).log("All HeroCore stat types validated successfully.");
        }
    }

    // ── Accessors ────────────────────────────────────────────────────

    // Crit & Damage
    public static int getCritDamageMultiplier() { return CRIT_DAMAGE_MULTIPLIER; }
    public static int getCritChance() { return CRIT_CHANCE; }
    public static int getAttackDamage() { return ATTACK_DAMAGE; }
    public static int getSpellPower() { return SPELL_POWER; }
    public static int getSpellCritChance() { return SPELL_CRIT_CHANCE; }
    public static int getSpellCritMultiplier() { return SPELL_CRIT_MULTIPLIER; }
    public static int getHealCritChance() { return HEAL_CRIT_CHANCE; }
    public static int getHealCritMultiplier() { return HEAL_CRIT_MULTIPLIER; }
    
    // Defense
    public static int getPhysicalResistance() { return PHYSICAL_RESISTANCE; }
    public static int getPhysicalResistancePercent() { return PHYSICAL_RESISTANCE_PERCENT; }
    public static int getMagicResist() { return MAGIC_RESIST; }
    public static int getArmor() { return ARMOR; }
    public static int getDodgeRating() { return DODGE_RATING; }
    public static int getBlockStrength() { return BLOCK_STRENGTH; }
    public static int getShieldStrength() { return SHIELD_STRENGTH; }
    
    // Elemental Resistance
    public static int getProjectileResistance() { return PROJECTILE_RESISTANCE; }
    public static int getProjectileResistancePercent() { return PROJECTILE_RESISTANCE_PERCENT; }
    public static int getFireResistance() { return FIRE_RESISTANCE; }
    public static int getFireResistancePercent() { return FIRE_RESISTANCE_PERCENT; }
    public static int getIceResistancePercent() { return ICE_RESISTANCE_PERCENT; }
    public static int getLightningResistancePercent() { return LIGHTNING_RESISTANCE_PERCENT; }
    public static int getPoisonResistancePercent() { return POISON_RESISTANCE_PERCENT; }
    public static int getArcaneResistancePercent() { return ARCANE_RESISTANCE_PERCENT; }
    
    // Control / Threat
    public static int getCCResistance() { return CC_RESISTANCE; }
    public static int getDebuffResistance() { return DEBUFF_RESISTANCE; }
    public static int getThreatGeneration() { return THREAT_GENERATION; }
    
    // Speed
    public static int getAttackSpeed() { return ATTACK_SPEED; }
    public static int getMoveSpeed() { return MOVE_SPEED; }
    public static int getMiningSpeed() { return MINING_SPEED; }
    
    // Resources
    public static int getMaxHealth() { return MAX_HEALTH; }
    public static int getHealthRegen() { return HEALTH_REGEN; }
    public static int getMaxMana() { return MAX_MANA; }
    public static int getManaRegen() { return MANA_REGEN; }
    public static int getMaxStamina() { return MAX_STAMINA; }
    public static int getStaminaRegen() { return STAMINA_REGEN; }
    
    // Healing
    public static int getHealingPower() { return HEALING_POWER; }
    public static int getBuffStrength() { return BUFF_STRENGTH; }
    
    // Misc
    public static int getFallDamageReduction() { return FALL_DAMAGE_REDUCTION; }

    // ── Generic helpers (used by damage/heal systems) ────────────────

    /**
     * Resolve a stat type index by asset ID at runtime.
     * Returns -1 if the asset map is not loaded or the key is not found.
     *
    * @param assetId full stat asset ID, e.g. "HeroCoreAttackDamage"
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
