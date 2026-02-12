package net.herotale.herocore.api.attribute;

/**
 * Modifier stacking order. This is the most important math contract in the system.
 * <p>
 * Formula:
 * {@code FinalValue = (Base + Σ FLAT) * (1 + Σ PERCENT_ADDITIVE) * Π (1 + each PERCENT_MULTIPLICATIVE)}
 * Then apply OVERRIDE caps/floors.
 */
public enum ModifierType {

    /** Added to base before any percentage math. */
    FLAT,

    /** These stack together additively, then applied as one multiplier. */
    PERCENT_ADDITIVE,

    /** Each one multiplies the result independently (powerful/rare). */
    PERCENT_MULTIPLICATIVE,

    /** Absolute cap or floor — applied last, ignores all other math. */
    OVERRIDE
}
