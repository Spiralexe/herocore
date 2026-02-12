package net.herotale.herocore.api.damage;

/**
 * Flags that describe properties of a damage event.
 * Multiple flags can be active on a single event.
 */
public enum DamageFlag {
    CRIT,
    DOT,
    AOE,
    MELEE,
    RANGED,
    SPELL,
    REFLECTED,
    COMBO_FINISHER,
    TRUE_DAMAGE,
    FALL_DAMAGE
}
