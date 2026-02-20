package net.herotale.herocore.impl;

/**
 * Centralised modifier key constants for all {@code StaticModifier} entries
 * that HeroCore writes to {@code EntityStatMap}.
 * <p>
 * Every key follows the format {@code "HC_category_descriptor"} to prevent
 * collisions with other mods and with Hytale's built-in modifiers.
 * <p>
 * HeroCore systems add and remove modifiers by these keys. Downstream
 * plugins can inspect them via {@code EntityStatMap.getModifier(statIndex, key)}.
 */
public final class HeroCoreModifiers {

    private HeroCoreModifiers() {}

    // ── Derived attribute modifiers (written by AttributeDerivationSystem) ──

    /** Prefix for all derivation modifiers: {@code "HC_derived_"} */
    public static final String DERIVED_PREFIX = "HC_derived_";

    // Primary → secondary derivation keys
    public static final String DERIVED_PHYSICAL_RESISTANCE = "HC_derived_physical_resistance";
    public static final String DERIVED_SPELL_POWER = "HC_derived_spell_power";
    public static final String DERIVED_CRIT_CHANCE = "HC_derived_crit_chance";
    public static final String DERIVED_CRIT_DAMAGE_MULTIPLIER = "HC_derived_crit_damage_multiplier";
    public static final String DERIVED_ATTACK_DAMAGE = "HC_derived_attack_damage";
    public static final String DERIVED_MOVE_SPEED = "HC_derived_move_speed";
    public static final String DERIVED_ATTACK_SPEED = "HC_derived_attack_speed";
    public static final String DERIVED_MAGIC_RESIST = "HC_derived_magic_resist";
    public static final String DERIVED_HEALING_POWER = "HC_derived_healing_power";
    public static final String DERIVED_MINING_SPEED = "HC_derived_mining_speed";
    public static final String DERIVED_MAX_HEALTH = "HC_derived_max_health";
    public static final String DERIVED_HEALTH_REGEN = "HC_derived_health_regen";
    public static final String DERIVED_ARMOR = "HC_derived_armor";
    public static final String DERIVED_BLOCK_STRENGTH = "HC_derived_block_strength";
    public static final String DERIVED_SHIELD_STRENGTH = "HC_derived_shield_strength";
    public static final String DERIVED_DODGE_RATING = "HC_derived_dodge_rating";
    public static final String DERIVED_FALL_DAMAGE_REDUCTION = "HC_derived_fall_damage_reduction";
    public static final String DERIVED_MAX_MANA = "HC_derived_max_mana";
    public static final String DERIVED_SPELL_CRIT_CHANCE = "HC_derived_spell_crit_chance";
    public static final String DERIVED_SPELL_CRIT_MULTIPLIER = "HC_derived_spell_crit_multiplier";
    public static final String DERIVED_MANA_REGEN = "HC_derived_mana_regen";
    public static final String DERIVED_HEAL_CRIT_CHANCE = "HC_derived_heal_crit_chance";
    public static final String DERIVED_BUFF_STRENGTH = "HC_derived_buff_strength";
    public static final String DERIVED_CC_RESISTANCE = "HC_derived_cc_resistance";
    public static final String DERIVED_DEBUFF_RESISTANCE = "HC_derived_debuff_resistance";
    public static final String DERIVED_THREAT_GENERATION = "HC_derived_threat_generation";
    public static final String DERIVED_STAMINA_REGEN = "HC_derived_stamina_regen";

    // Defense derivation keys
    public static final String DERIVED_PHYSICAL_RESISTANCE_PERCENT = "HC_derived_physical_resistance_pct";
    public static final String DERIVED_PROJECTILE_RESISTANCE = "HC_derived_projectile_resistance";
    public static final String DERIVED_PROJECTILE_RESISTANCE_PERCENT = "HC_derived_projectile_resistance_pct";
    public static final String DERIVED_FIRE_RESISTANCE = "HC_derived_fire_resistance";
    public static final String DERIVED_FIRE_RESISTANCE_PERCENT = "HC_derived_fire_resistance_pct";
    public static final String DERIVED_ICE_RESISTANCE_PERCENT = "HC_derived_ice_resistance_pct";
    public static final String DERIVED_LIGHTNING_RESISTANCE_PERCENT = "HC_derived_lightning_resistance_pct";
    public static final String DERIVED_POISON_RESISTANCE_PERCENT = "HC_derived_poison_resistance_pct";
    public static final String DERIVED_ARCANE_RESISTANCE_PERCENT = "HC_derived_arcane_resistance_pct";

    // ── Status effect modifiers ────────────────────────────────────

    /** Prefix for status effect modifiers: {@code "HC_effect_"} */
    public static final String EFFECT_PREFIX = "HC_effect_";

    // ── Level-based modifiers ──────────────────────────────────────

    /** Prefix for level-based modifiers: {@code "HC_level_"} */
    public static final String LEVEL_PREFIX = "HC_level_";

    public static final String LEVEL_PHYSICAL_RESIST_PERCENT = "HC_level_physical_resist_pct";

    // ── Gear modifiers ─────────────────────────────────────────────

    /** Prefix for gear modifiers: {@code "HC_gear_"} */
    public static final String GEAR_PREFIX = "HC_gear_";

    // ── Buff/debuff modifiers ──────────────────────────────────────

    /** Prefix for buff modifiers: {@code "HC_buff_"} */
    public static final String BUFF_PREFIX = "HC_buff_";

    /**
     * Build a modifier key for a specific derived attribute.
     *
     * @param attribute the RPGAttribute name (e.g., "attack_speed")
     * @return the modifier key (e.g., "HC_derived_attack_speed")
     */
    public static String derived(String attribute) {
        return DERIVED_PREFIX + attribute;
    }

    /**
     * Build a modifier key for a status effect.
     *
     * @param effectId the effect ID (e.g., "fortify")
     * @param stat     the stat being modified (e.g., "physical_resistance")
     * @return the modifier key (e.g., "HC_effect_fortify_physical_resistance")
     */
    public static String effect(String effectId, String stat) {
        return EFFECT_PREFIX + effectId + "_" + stat;
    }

    /**
     * Build a modifier key for gear.
     *
     * @param slotAndItem e.g., "chestplate_iron"
     * @param stat        the stat being modified
     * @return the modifier key
     */
    public static String gear(String slotAndItem, String stat) {
        return GEAR_PREFIX + slotAndItem + "_" + stat;
    }
}
