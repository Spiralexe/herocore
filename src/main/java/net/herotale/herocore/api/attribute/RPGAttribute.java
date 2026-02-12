package net.herotale.herocore.api.attribute;

/**
 * Master attribute registry. Every attribute in the RPG system is defined here.
 * Attributes come in two tiers: primary (direct player inputs) and secondary/derived.
 */
public enum RPGAttribute {

    // ── Primary Attributes ──────────────────────────────────────────────
    STRENGTH,
    DEXTERITY,
    INTELLIGENCE,
    FAITH,
    VITALITY,
    RESOLVE,

    // ── Combat — Physical ───────────────────────────────────────────────
    MAX_HEALTH,
    ARMOR,
    ATTACK_DAMAGE,
    ATTACK_SPEED,
    BLOCK_STRENGTH,
    CRIT_CHANCE,
    CRIT_DAMAGE_MULTIPLIER,
    DODGE_RATING,
    ARMOR_PENETRATION,
    LIFESTEAL,

    // ── Defense — Hytale-Native Resistance (per DamageCause) ─────────
    /**
     * Additive flat resistance to PHYSICAL damage cause.
     * Synced to Hytale's native {@code ArmorDamageReduction} system via
     * {@code DefenseBridge}. Formula: {@code damage = max(0, raw - flat) * max(0, 1 - percent)}.
     */
    PHYSICAL_RESISTANCE,

    /**
     * Multiplicative percent resistance to PHYSICAL damage cause (0.0–1.0).
     * A value of 0.3 means 30% damage reduction applied after flat resistance.
     */
    PHYSICAL_RESISTANCE_PERCENT,

    /**
     * Additive flat resistance to PROJECTILE damage cause.
     */
    PROJECTILE_RESISTANCE,

    /**
     * Multiplicative percent resistance to PROJECTILE damage cause (0.0–1.0).
     */
    PROJECTILE_RESISTANCE_PERCENT,

    /**
     * Additive flat resistance to FIRE damage cause.
     */
    FIRE_RESISTANCE,

    /**
     * Multiplicative percent resistance to FIRE damage cause (0.0–1.0).
     */
    FIRE_RESISTANCE_PERCENT,

    // ── Combat — Magical ────────────────────────────────────────────────
    SPELL_POWER,
    SPELL_CRIT_CHANCE,
    SPELL_CRIT_MULTIPLIER,
    MAGIC_PENETRATION,
    MAGIC_RESIST,
    ELEMENTAL_DAMAGE_FIRE,
    ELEMENTAL_DAMAGE_ICE,
    ELEMENTAL_DAMAGE_LIGHTNING,
    ELEMENTAL_DAMAGE_POISON,
    ELEMENTAL_DAMAGE_ARCANE,
    ELEMENTAL_RESIST_FIRE,
    ELEMENTAL_RESIST_ICE,
    ELEMENTAL_RESIST_LIGHTNING,
    ELEMENTAL_RESIST_POISON,
    ELEMENTAL_RESIST_ARCANE,

    // ── Combat — Healing ────────────────────────────────────────────────
    HEALING_POWER,
    HEALING_RECEIVED_BONUS,
    HEAL_CRIT_CHANCE,
    HEAL_CRIT_MULTIPLIER,
    SHIELD_STRENGTH,
    BUFF_STRENGTH,

    // ── Resources ───────────────────────────────────────────────────────
    MAX_MANA,
    MAX_STAMINA,
    MAX_OXYGEN,
    MAX_AMMO,
    MANA_REGEN,
    STAMINA_REGEN,
    STAMINA_REGEN_DELAY_MS,
    HEALTH_REGEN,
    OXYGEN_DRAIN_RATE,
    OXYGEN_REGEN_RATE,
    AMMO_REGEN_RATE,
    MANA_COST_REDUCTION,
    STAMINA_COST_REDUCTION,

    // ── Mobility / World ────────────────────────────────────────────────
    MOVE_SPEED,
    JUMP_HEIGHT,
    MINING_SPEED,
    HARVEST_YIELD_MULTIPLIER,
    RARE_DROP_CHANCE,
    CRAFTING_SPEED_BONUS,
    CRAFTING_COST_REDUCTION,
    CRAFTING_OUTPUT_BONUS,

    // ── Control / Threat ───────────────────────────────────────────────
    CC_RESISTANCE,
    DEBUFF_RESISTANCE,
    THREAT_GENERATION,

    // ── Progression ─────────────────────────────────────────────────────
    XP_GAIN_MULTIPLIER,
    REP_GAIN_MULTIPLIER,
    REP_LOSS_REDUCTION,

    // ── Economy ─────────────────────────────────────────────────────────
    VENDOR_PRICE_MULTIPLIER,
    AUCTION_TAX_REDUCTION,

    // ── Misc ────────────────────────────────────────────────────────────
    RESPAWN_COOLDOWN_REDUCTION,
    DURABILITY_LOSS_REDUCTION,
    DEATH_PENALTY_REDUCTION,
    FALL_DAMAGE_REDUCTION
}
