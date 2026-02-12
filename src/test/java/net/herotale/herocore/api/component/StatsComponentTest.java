package net.herotale.herocore.api.component;

import net.herotale.herocore.api.attribute.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the modifier stacking formula on {@link StatsComponent}:
 * {@code FinalValue = (Base + Σ FLAT) * (1 + Σ PERCENT_ADDITIVE) * Π (1 + each PERCENT_MULTIPLICATIVE)}
 * Then OVERRIDE applied.
 */
class StatsComponentTest {

    private StatsComponent stats;

    @BeforeEach
    void setUp() {
        stats = new StatsComponent();
    }

    @Test
    void baseValueDefaultsToZero() {
        assertEquals(0.0, stats.getBase(RPGAttribute.STRENGTH));
        assertEquals(0.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void setBaseReturnsBase() {
        stats.setBase(RPGAttribute.STRENGTH, 50.0);
        assertEquals(50.0, stats.getBase(RPGAttribute.STRENGTH));
        assertEquals(50.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void flatModifierAddsToBase() {
        stats.setBase(RPGAttribute.STRENGTH, 10.0);
        stats.addModifier(modifier("flat1", RPGAttribute.STRENGTH, 5.0, ModifierType.FLAT, "equip:sword"));
        // (10 + 5) = 15
        assertEquals(15.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void percentAdditiveStacksAdditiveThenMultiplies() {
        stats.setBase(RPGAttribute.STRENGTH, 100.0);
        stats.addModifier(modifier("pct1", RPGAttribute.STRENGTH, 0.2, ModifierType.PERCENT_ADDITIVE, "buff:a"));
        stats.addModifier(modifier("pct2", RPGAttribute.STRENGTH, 0.3, ModifierType.PERCENT_ADDITIVE, "buff:b"));
        // (100) * (1 + 0.2 + 0.3) = 100 * 1.5 = 150
        assertEquals(150.0, stats.getValue(RPGAttribute.STRENGTH), 0.001);
    }

    @Test
    void percentMultiplicativeStacksMultiplicatively() {
        stats.setBase(RPGAttribute.STRENGTH, 100.0);
        stats.addModifier(modifier("m1", RPGAttribute.STRENGTH, 0.5, ModifierType.PERCENT_MULTIPLICATIVE, "buff:a"));
        stats.addModifier(modifier("m2", RPGAttribute.STRENGTH, 0.5, ModifierType.PERCENT_MULTIPLICATIVE, "buff:b"));
        // 100 * (1 + 0.5) * (1 + 0.5) = 100 * 1.5 * 1.5 = 225
        assertEquals(225.0, stats.getValue(RPGAttribute.STRENGTH), 0.001);
    }

    @Test
    void fullStackingFormula() {
        stats.setBase(RPGAttribute.STRENGTH, 100.0);
        stats.addModifier(modifier("f1", RPGAttribute.STRENGTH, 20.0, ModifierType.FLAT, "equip:helm"));
        stats.addModifier(modifier("pa1", RPGAttribute.STRENGTH, 0.5, ModifierType.PERCENT_ADDITIVE, "skill:vigor"));
        stats.addModifier(modifier("pm1", RPGAttribute.STRENGTH, 0.1, ModifierType.PERCENT_MULTIPLICATIVE, "buff:fort"));

        // (100 + 20) * (1 + 0.5) * (1 + 0.1) = 120 * 1.5 * 1.1 = 198
        assertEquals(198.0, stats.getValue(RPGAttribute.STRENGTH), 0.001);
    }

    @Test
    void overrideReplacesCalculation() {
        stats.setBase(RPGAttribute.STRENGTH, 1.0);
        stats.addModifier(modifier("f1", RPGAttribute.STRENGTH, 10.0, ModifierType.FLAT, "equip:boot"));
        stats.addModifier(modifier("o1", RPGAttribute.STRENGTH, 5.0, ModifierType.OVERRIDE, "admin:set"));

        assertEquals(5.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void percentAdditiveCapsAtMax() {
        stats.setBase(RPGAttribute.STRENGTH, 100.0);
        // maxPercentAdditive = 3.0 by default
        stats.addModifier(modifier("p1", RPGAttribute.STRENGTH, 2.0, ModifierType.PERCENT_ADDITIVE, "a:1"));
        stats.addModifier(modifier("p2", RPGAttribute.STRENGTH, 2.0, ModifierType.PERCENT_ADDITIVE, "a:2"));
        // Sum = 4.0 but capped at 3.0 → 100 * (1 + 3.0) = 400
        assertEquals(400.0, stats.getValue(RPGAttribute.STRENGTH), 0.001);
    }

    @Test
    void multiplicativeCapIsEnforced() {
        stats.setBase(RPGAttribute.STRENGTH, 10.0);
        // multiplicativeCap = 5.0 by default
        stats.addModifier(modifier("m1", RPGAttribute.STRENGTH, 2.0, ModifierType.PERCENT_MULTIPLICATIVE, "a:1"));
        stats.addModifier(modifier("m2", RPGAttribute.STRENGTH, 2.0, ModifierType.PERCENT_MULTIPLICATIVE, "a:2"));
        stats.addModifier(modifier("m3", RPGAttribute.STRENGTH, 2.0, ModifierType.PERCENT_MULTIPLICATIVE, "a:3"));
        // product = 3 * 3 * 3 = 27; capped at 5 → 10 * 5 = 50
        assertEquals(50.0, stats.getValue(RPGAttribute.STRENGTH), 0.001);
    }

    @Test
    void removeBySource_removesAllFromThatSource() {
        ModifierSource source = ModifierSource.of("equip:helm");
        stats.setBase(RPGAttribute.STRENGTH, 10.0);
        stats.addModifier(modifier("a1", RPGAttribute.STRENGTH, 5.0, ModifierType.FLAT, "equip:helm"));
        stats.addModifier(modifier("a2", RPGAttribute.STRENGTH, 3.0, ModifierType.FLAT, "equip:helm"));
        stats.addModifier(modifier("a3", RPGAttribute.STRENGTH, 7.0, ModifierType.FLAT, "equip:boots"));

        assertEquals(25.0, stats.getValue(RPGAttribute.STRENGTH));
        stats.removeBySource(source);
        // Only boots modifier remains: 10 + 7 = 17
        assertEquals(17.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void removeById_removesSingleModifier() {
        stats.setBase(RPGAttribute.STRENGTH, 10.0);
        stats.addModifier(modifier("s1", RPGAttribute.STRENGTH, 5.0, ModifierType.FLAT, "equip:ring"));
        stats.addModifier(modifier("s2", RPGAttribute.STRENGTH, 3.0, ModifierType.FLAT, "equip:amulet"));

        assertEquals(18.0, stats.getValue(RPGAttribute.STRENGTH));
        stats.removeById("s1");
        assertEquals(13.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    @Test
    void hasSource_returnsTrueWhenPresent() {
        ModifierSource source = ModifierSource.of("equip:sword");
        stats.addModifier(modifier("h1", RPGAttribute.STRENGTH, 5.0, ModifierType.FLAT, "equip:sword"));
        assertTrue(stats.hasSource(source));
        stats.removeBySource(source);
        assertFalse(stats.hasSource(source));
    }

    @Test
    void invalidateAll_forcesRecompute() {
        stats.setBase(RPGAttribute.STRENGTH, 50.0);
        assertEquals(50.0, stats.getValue(RPGAttribute.STRENGTH));
        stats.invalidateAll();
        // Should still compute correctly after invalidation
        assertEquals(50.0, stats.getValue(RPGAttribute.STRENGTH));
    }

    // --- Helper ---

    private static AttributeModifier modifier(String id, RPGAttribute attr, double value,
                                               ModifierType type, String sourceKey) {
        return AttributeModifier.builder()
                .id(id)
                .attribute(attr)
                .value(value)
                .type(type)
                .source(ModifierSource.of(sourceKey))
                .build();
    }
}
