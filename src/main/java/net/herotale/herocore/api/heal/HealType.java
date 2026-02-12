package net.herotale.herocore.api.heal;

/**
 * Classification of healing sources.
 */
public enum HealType {
    /** Cast by a skill (uses HEALING_POWER / SPELL_POWER scaling). */
    SPELL,
    /** From consumable item. */
    POTION,
    /** From HEALTH_REGEN attribute (fired by ResourceRegenManager). */
    REGEN_TICK,
    /** Class passive or item proc. */
    PASSIVE,
    /** Shrine, zone, etc. */
    ENVIRONMENTAL
}
