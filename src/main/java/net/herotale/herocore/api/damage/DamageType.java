package net.herotale.herocore.api.damage;

/**
 * Classification of damage sources for system routing and resistance lookups.
 */
public enum DamageType {
    PHYSICAL,
    PROJECTILE,
    MAGICAL,
    FIRE,
    ICE,
    LIGHTNING,
    POISON,
    ARCANE,
    TRUE,       // bypasses all resistances — use sparingly
    FALL,       // environmental
    VOID        // custom/misc
}
