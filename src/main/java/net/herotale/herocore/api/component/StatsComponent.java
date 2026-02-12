package net.herotale.herocore.api.component;

import net.herotale.herocore.api.attribute.AttributeMap;
import net.herotale.herocore.api.attribute.AttributeModifier;
import net.herotale.herocore.api.attribute.ModifierSource;
import net.herotale.herocore.api.attribute.ModifierType;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.impl.attribute.AttributeCalculator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ECS Component: Per-entity attribute storage.
 * <p>
 * Holds base values, active modifiers, and computed finals for all RPG attributes.
 * This is the shared data definition that all plugins reference — items, guilds,
 * heroes-rpg all talk about the same {@code StatsComponent}.
 * <p>
 * Computation formula:
 * {@code FinalValue = (Base + Σ FLAT) * (1 + Σ PERCENT_ADDITIVE) * Π (1 + each PERCENT_MULTIPLICATIVE)}
 * Then OVERRIDE caps/floors are applied.
 * <p>
 * Implements {@link AttributeMap} so generic utility code can operate on attribute
 * storage without coupling to this concrete class.
 */
public class StatsComponent implements AttributeMap {

    private static final Set<RPGAttribute> PRIMARY_ATTRIBUTES = EnumSet.of(
            RPGAttribute.STRENGTH,
            RPGAttribute.DEXTERITY,
            RPGAttribute.INTELLIGENCE,
            RPGAttribute.FAITH,
            RPGAttribute.VITALITY,
            RPGAttribute.RESOLVE
    );

    private static final ModifierSource DERIVED_SOURCE = ModifierSource.of("core:derived");

    private final Map<RPGAttribute, Double> baseValues = new ConcurrentHashMap<>();
    private final Map<String, AttributeModifier> modifiersById = new ConcurrentHashMap<>();
    private final Map<RPGAttribute, List<AttributeModifier>> modifiersByAttribute = new ConcurrentHashMap<>();
    private final Map<RPGAttribute, Double> computedCache = new ConcurrentHashMap<>();

    private final Map<RPGAttribute, Double> derivedBaseValues = new ConcurrentHashMap<>();
    private final Map<RPGAttribute, List<AttributeModifier>> derivedModifiersByAttribute = new ConcurrentHashMap<>();
    private final AtomicLong derivedModifierCounter = new AtomicLong(0);

    private final AttributeCalculator calculator;
    private final Object derivedLock = new Object();
    private volatile boolean derivedDirty;

    public StatsComponent() {
        this(new AttributeCalculator());
    }

    public StatsComponent(AttributeCalculator calculator) {
        this.calculator = calculator;
        this.derivedDirty = calculator.hasDerivations();
    }

    // ── Read ──────────────────────────────────────────────────────────

    /** Read the final computed value (base + all modifiers applied). */
    @Override
    public double getValue(RPGAttribute attribute) {
        if (!isPrimary(attribute)) {
            ensureDerivedUpToDate();
        }
        return computedCache.computeIfAbsent(attribute, this::compute);
    }

    /** Read the base value before any modifiers. */
    @Override
    public double getBase(RPGAttribute attribute) {
        return baseValues.getOrDefault(attribute, 0.0);
    }

    // ── Write ─────────────────────────────────────────────────────────

    /** Set the base value for an attribute. */
    @Override
    public void setBase(RPGAttribute attribute, double value) {
        baseValues.put(attribute, value);
        markDerivedDirtyIfPrimary(attribute);
        invalidate(attribute);
    }

    /** Add a modifier. */
    @Override
    public void addModifier(AttributeModifier modifier) {
        modifiersById.put(modifier.getId(), modifier);
        modifiersByAttribute.computeIfAbsent(modifier.getAttribute(), k -> new CopyOnWriteArrayList<>())
                .add(modifier);
        markDerivedDirtyIfPrimary(modifier.getAttribute());
        invalidate(modifier.getAttribute());
    }

    /** Remove all modifiers from a specific source. */
    @Override
    public void removeBySource(ModifierSource source) {
        Set<RPGAttribute> affected = new HashSet<>();
        modifiersById.entrySet().removeIf(entry -> {
            if (entry.getValue().getSource().equals(source)) {
                affected.add(entry.getValue().getAttribute());
                return true;
            }
            return false;
        });
        for (RPGAttribute attr : affected) {
            List<AttributeModifier> list = modifiersByAttribute.get(attr);
            if (list != null) {
                list.removeIf(m -> m.getSource().equals(source));
            }
            markDerivedDirtyIfPrimary(attr);
            invalidate(attr);
        }
    }

    /** Remove a specific modifier by its unique ID. */
    @Override
    public void removeById(String modifierId) {
        AttributeModifier removed = modifiersById.remove(modifierId);
        if (removed != null) {
            List<AttributeModifier> list = modifiersByAttribute.get(removed.getAttribute());
            if (list != null) {
                list.removeIf(m -> m.getId().equals(modifierId));
            }
            markDerivedDirtyIfPrimary(removed.getAttribute());
            invalidate(removed.getAttribute());
        }
    }

    /** Check if a source has any active modifiers. */
    @Override
    public boolean hasSource(ModifierSource source) {
        return modifiersById.values().stream().anyMatch(m -> m.getSource().equals(source));
    }

    /** Force recompute of a specific attribute. */
    @Override
    public void invalidate(RPGAttribute attribute) {
        computedCache.remove(attribute);
    }

    /** Force recompute of all attributes. */
    @Override
    public void invalidateAll() {
        computedCache.clear();
        derivedDirty = calculator.hasDerivations();
    }

    // ── Derived attributes (used by DerivedAttributeSystem) ──────────

    public void setDerivedBase(RPGAttribute attribute, double value) {
        derivedBaseValues.put(attribute, value);
    }

    public void addDerivedModifier(RPGAttribute attribute, double value, ModifierType type) {
        if (value == 0.0) return;
        derivedModifiersByAttribute.computeIfAbsent(attribute, k -> new CopyOnWriteArrayList<>())
                .add(AttributeModifier.builder()
                        .id("derived:" + attribute.name().toLowerCase() + ":" + derivedModifierCounter.incrementAndGet())
                        .attribute(attribute)
                        .value(value)
                        .type(type)
                        .source(DERIVED_SOURCE)
                        .build());
    }

    public void clearDerived() {
        derivedBaseValues.clear();
        derivedModifiersByAttribute.clear();
    }

    public void markDerivedClean() {
        derivedDirty = false;
    }

    public boolean isDerivedDirty() {
        return derivedDirty;
    }

    // ── Internal ─────────────────────────────────────────────────────

    private double compute(RPGAttribute attribute) {
        double base = baseValues.getOrDefault(attribute, 0.0)
                + derivedBaseValues.getOrDefault(attribute, 0.0);
        List<AttributeModifier> mods = modifiersByAttribute.getOrDefault(attribute, Collections.emptyList());
        List<AttributeModifier> derivedMods = derivedModifiersByAttribute.getOrDefault(attribute, Collections.emptyList());

        if (mods.isEmpty() && derivedMods.isEmpty()) {
            return base;
        }
        if (derivedMods.isEmpty()) {
            return calculator.compute(base, mods);
        }
        if (mods.isEmpty()) {
            return calculator.compute(base, derivedMods);
        }

        List<AttributeModifier> combined = new ArrayList<>(mods.size() + derivedMods.size());
        combined.addAll(mods);
        combined.addAll(derivedMods);
        return calculator.compute(base, combined);
    }

    private void ensureDerivedUpToDate() {
        if (!calculator.hasDerivations() || !derivedDirty) return;
        synchronized (derivedLock) {
            if (!derivedDirty) return;
            clearDerived();
            calculator.computeDerivedAttributes(this);
            computedCache.keySet().removeIf(attr -> !isPrimary(attr));
            derivedDirty = false;
        }
    }

    private void markDerivedDirtyIfPrimary(RPGAttribute attribute) {
        if (calculator.hasDerivations() && isPrimary(attribute)) {
            derivedDirty = true;
            computedCache.keySet().removeIf(attr -> !isPrimary(attr));
        }
    }

    private static boolean isPrimary(RPGAttribute attribute) {
        return PRIMARY_ATTRIBUTES.contains(attribute);
    }
}
