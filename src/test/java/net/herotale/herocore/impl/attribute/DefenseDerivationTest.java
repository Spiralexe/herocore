package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.AttributeModifier;
import net.herotale.herocore.api.attribute.ModifierSource;
import net.herotale.herocore.api.attribute.ModifierType;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.impl.config.CoreConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for defense derivation in {@link AttributeCalculator}.
 * <p>
 * Verifies that Hytale-native resistance attributes (PHYSICAL_RESISTANCE,
 * PROJECTILE_RESISTANCE, FIRE_RESISTANCE, etc.) are correctly derived from
 * primary attributes using the defense derivation config.
 */
class DefenseDerivationTest {

    private static final CoreConfig.AttributeDerivationConfig ATTR_DERIVATION =
            new CoreConfig.AttributeDerivationConfig(
                    new CoreConfig.VitalityDerivation(10.0, 100.0, 0.1, 1.0, 0.002),
                    new CoreConfig.StrengthDerivation(0.5, 2.0, 3.0),
                    new CoreConfig.DexterityDerivation(0.005, 0.01, 1.0, 0.01, 0.5),
                    new CoreConfig.IntelligenceDerivation(0.01, 10.0, 50.0, 0.004, 0.002),
                    new CoreConfig.FaithDerivation(0.015, 8.0, 30.0, 0.15, 0.5, 0.003, 0.005),
                    new CoreConfig.ResolveDerivation(5.0, 0.01, 0.75, 0.008, 0.60, 0.02, 0.1, 0.003)
            );

    private static final CoreConfig.DefenseDerivationConfig DEFENSE_DERIVATION =
            new CoreConfig.DefenseDerivationConfig(
                    0.5,     // physicalResistanceFlatPerVitality
                    0.002,   // physicalResistancePercentPerVitality
                    0.01,    // physicalResistancePerLevelPercent
                    0.3,     // projectileResistanceFlatPerDexterity
                    0.001,   // projectileResistancePercentPerDexterity
                    0.4,     // fireResistanceFlatPerResolve
                    0.0015,  // fireResistancePercentPerResolve
                    0.75     // maxResistancePercent
            );

    private StatsComponent createStatsWithDerivations() {
        AttributeCalculator calc = new AttributeCalculator(3.0, 5.0, ATTR_DERIVATION, DEFENSE_DERIVATION);
        return new StatsComponent(calc);
    }

    // ── Physical Resistance from Vitality ──────────────────────────

    @Test
    void physicalResistance_derivesFromVitality() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 20.0);

        // Flat: 20 * 0.5 = 10.0
        double physFlat = stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE);
        assertEquals(10.0, physFlat, 0.001, "Physical resistance flat should be VIT * 0.5");

        // Percent: 20 * 0.002 = 0.04
        double physPercent = stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT);
        assertEquals(0.04, physPercent, 0.001, "Physical resistance percent should be VIT * 0.002");
    }

    @Test
    void physicalResistance_zeroWithNoVitality() {
        StatsComponent stats = createStatsWithDerivations();
        assertEquals(0.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001);
        assertEquals(0.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT), 0.001);
    }

    @Test
    void physicalResistance_percentCappedByMaxResistance() {
        StatsComponent stats = createStatsWithDerivations();
        // 500 VIT * 0.002 = 1.0, but capped at 0.75
        stats.setBase(RPGAttribute.VITALITY, 500.0);
        double physPercent = stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT);
        assertEquals(0.75, physPercent, 0.001, "Percent resistance should be capped at maxResistancePercent");
    }

    // ── Projectile Resistance from Dexterity ───────────────────────

    @Test
    void projectileResistance_derivesFromDexterity() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.DEXTERITY, 30.0);

        // Flat: 30 * 0.3 = 9.0
        double projFlat = stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE);
        assertEquals(9.0, projFlat, 0.001, "Projectile resistance flat should be DEX * 0.3");

        // Percent: 30 * 0.001 = 0.03
        double projPercent = stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT);
        assertEquals(0.03, projPercent, 0.001, "Projectile resistance percent should be DEX * 0.001");
    }

    // ── Fire Resistance from Resolve ───────────────────────────────

    @Test
    void fireResistance_derivesFromResolve() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.RESOLVE, 25.0);

        // Flat: 25 * 0.4 = 10.0
        double fireFlat = stats.getValue(RPGAttribute.FIRE_RESISTANCE);
        assertEquals(10.0, fireFlat, 0.001, "Fire resistance flat should be RESOLVE * 0.4");

        // Percent: 25 * 0.0015 = 0.0375
        double firePercent = stats.getValue(RPGAttribute.FIRE_RESISTANCE_PERCENT);
        assertEquals(0.0375, firePercent, 0.001, "Fire resistance percent should be RESOLVE * 0.0015");
    }

    // ── Additive Modifiers on Top of Derivation ────────────────────

    @Test
    void resistanceModifiers_stackOnTopOfDerivation() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 20.0);

        // Derived flat: 20 * 0.5 = 10.0
        // Now add a gear modifier: +5 flat physical resistance
        stats.addModifier(AttributeModifier.builder()
                .id("gear:iron_chestplate_phys")
                .attribute(RPGAttribute.PHYSICAL_RESISTANCE)
                .value(5.0)
                .type(ModifierType.FLAT)
                .source(ModifierSource.of("gear:iron_chestplate"))
                .build());

        // Total: 10.0 (derived) + 5.0 (gear) = 15.0
        assertEquals(15.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001,
                "Gear flat modifier should stack additively with derived resistance");
    }

    @Test
    void resistanceModifiers_percentFromBuffStacksOnDerivation() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 20.0);

        // Derived percent: 20 * 0.002 = 0.04
        // Add buff: +0.1 (10%) resistance percent
        stats.addModifier(AttributeModifier.builder()
                .id("buff:fortify_phys_pct")
                .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
                .value(0.1)
                .type(ModifierType.FLAT)
                .source(ModifierSource.of("buff:fortify"))
                .build());

        // Total: 0.04 (derived) + 0.1 (buff) = 0.14
        assertEquals(0.14, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT), 0.001,
                "Buff percent modifier should stack additively with derived resistance");
    }

    // ── Level-based Defense (via external modifier) ────────────────

    @Test
    void levelBasedDefense_appliedViaModifier() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 10.0);

        // Simulate level 30 with +1% physical resistance per level = 0.3 total
        int playerLevel = 30;
        double perLevelBonus = DEFENSE_DERIVATION.physicalResistancePerLevelPercent();
        double levelResistance = playerLevel * perLevelBonus; // 30 * 0.01 = 0.3

        stats.addModifier(AttributeModifier.builder()
                .id("herocore:level_defense_phys_pct")
                .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
                .value(levelResistance)
                .type(ModifierType.FLAT)
                .source(ModifierSource.of("herocore:level_defense"))
                .build());

        // Derived: 10 * 0.002 = 0.02
        // Level: 0.3
        // Total: 0.32
        assertEquals(0.32, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT), 0.001,
                "Level-based defense should add to derived resistance");
    }

    // ── ARMOR attribute (dormant but stored) ───────────────────────

    @Test
    void armorAttribute_existsAndAcceptsModifiers() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.ARMOR, 50.0);

        assertEquals(50.0, stats.getValue(RPGAttribute.ARMOR), 0.001,
                "ARMOR should be settable and readable as a standard attribute");

        // Add a flat modifier
        stats.addModifier(AttributeModifier.builder()
                .id("gear:plate_armor")
                .attribute(RPGAttribute.ARMOR)
                .value(25.0)
                .type(ModifierType.FLAT)
                .source(ModifierSource.of("gear:plate"))
                .build());

        assertEquals(75.0, stats.getValue(RPGAttribute.ARMOR), 0.001,
                "ARMOR should support standard modifier stacking");
    }

    // ── No Defense Config → No Derivation ──────────────────────────

    @Test
    void noDefenseConfig_resistancesRemainAtZero() {
        // AttributeCalculator with null defenseDerivation
        AttributeCalculator calc = new AttributeCalculator(3.0, 5.0, ATTR_DERIVATION, null);
        StatsComponent stats = new StatsComponent(calc);
        stats.setBase(RPGAttribute.VITALITY, 50.0);

        assertEquals(0.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001,
                "Without defense config, resistance should remain at default (0)");
        assertEquals(0.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT), 0.001);
    }

    // ── Invalidation recalculates defense ──────────────────────────

    @Test
    void invalidateAll_recalculatesDefenseDerivation() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 20.0);
        assertEquals(10.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001);

        // Change vitality
        stats.setBase(RPGAttribute.VITALITY, 40.0);
        // Derived should auto-recompute: 40 * 0.5 = 20.0
        assertEquals(20.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001,
                "Changing primary should trigger defense recalculation");
    }

    @Test
    void multipleResistances_deriveIndependently() {
        StatsComponent stats = createStatsWithDerivations();
        stats.setBase(RPGAttribute.VITALITY, 20.0);
        stats.setBase(RPGAttribute.DEXTERITY, 15.0);
        stats.setBase(RPGAttribute.RESOLVE, 10.0);

        assertEquals(10.0, stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE), 0.001);
        assertEquals(4.5, stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE), 0.001);
        assertEquals(4.0, stats.getValue(RPGAttribute.FIRE_RESISTANCE), 0.001);
    }
}
