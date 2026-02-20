package net.herotale.herocore.api.attribute;

/**
 * A lightweight data record representing a named modifier to an RPG attribute.
 * <p>
 * Used by zone modifiers, difficulty tiers, and other systems that need to describe
 * attribute modifications declaratively. The actual application of these modifiers
 * to {@code EntityStatMap} is handled by the system that owns the modifier source
 * (e.g., zone enter/leave logic).
 *
 * @param attribute the RPG attribute being modified
 * @param value     the modifier value (flat amount or percentage depending on context)
 * @param label     optional human-readable label for this modifier source
 */
public record AttributeModifier(
        RPGAttribute attribute,
        double value,
        String label
) {
    /**
     * Create a modifier with just an attribute and value (no label).
     */
    public AttributeModifier(RPGAttribute attribute, double value) {
        this(attribute, value, "");
    }
}
