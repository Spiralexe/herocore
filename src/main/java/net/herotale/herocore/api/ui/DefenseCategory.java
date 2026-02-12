package net.herotale.herocore.api.ui;

/**
 * Canonical defense categories for UI display.
 * <p>
 * These names are HeroCore-facing and do not alter vanilla Hytale behavior.
 * The UI layer should treat all values as percentages (0.0 to 1.0).
 */
public enum DefenseCategory {

    /** Physical defense shown in vanilla UI (tab menu). */
    PHYSICAL_DEFENSE,

    /** Magical defense (aggregate non-physical mitigation). */
    MAGICAL_DEFENSE,

    /** Projectile resistance. */
    PROJECTILE_RESISTANCE,

    /** Elemental resistances. */
    ELEMENTAL_FIRE,
    ELEMENTAL_FROST,
    ELEMENTAL_LIGHTNING,
    ELEMENTAL_POISON,
    ELEMENTAL_ARCANE
}
