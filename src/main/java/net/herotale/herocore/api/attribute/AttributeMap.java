package net.herotale.herocore.api.attribute;

/**
 * Per-entity attribute storage contract. Holds base values, active modifiers,
 * and computed finals.
 * <p>
 * The computation formula is:
 * {@code FinalValue = (Base + Σ FLAT) × (1 + Σ PERCENT_ADDITIVE) × Π (1 + each PERCENT_MULTIPLICATIVE)}
 * Then OVERRIDE caps/floors are applied.
 * <p>
 * The canonical implementation is
 * {@link net.herotale.herocore.api.component.StatsComponent StatsComponent},
 * the ECS data-bag attached to every entity. Prefer using {@code StatsComponent}
 * directly when possible — this interface exists so generic utility code can
 * operate on attribute storage without coupling to the concrete component class.
 * <p>
 * <b>Note:</b> The old {@code onChange()} listener method was removed. In the ECS
 * architecture, use {@link net.herotale.herocore.api.event.AttributeChangeEvent}
 * through the {@link net.herotale.herocore.api.event.HeroCoreEventBus} instead.
 *
 * @see net.herotale.herocore.api.component.StatsComponent
 */
public interface AttributeMap {

    /** Read the final computed value (base + all modifiers applied). */
    double getValue(RPGAttribute attribute);

    /** Read the base value before any modifiers. */
    double getBase(RPGAttribute attribute);

    /** Set the base value for an attribute. */
    void setBase(RPGAttribute attribute, double value);

    /** Add a modifier — always named and sourced. */
    void addModifier(AttributeModifier modifier);

    /** Remove all modifiers from a specific source (clean up on rank change, unequip, etc.). */
    void removeBySource(ModifierSource source);

    /** Remove a specific modifier by its unique ID. */
    void removeById(String modifierId);

    /** Check if a source has any active modifiers. */
    boolean hasSource(ModifierSource source);

    /** Force recompute for a specific attribute. */
    void invalidate(RPGAttribute attribute);

    /** Force recompute of all attributes. */
    void invalidateAll();
}
