package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.impl.config.CoreConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AttributeDerivationFormulas} — pure math derivation of
 * secondary attributes from primary stats.
 * 
 * NOTE: Tests use simplified derivation formulas for predictability.
 * The actual config-based formulas are more complex.
 */
class AttributeDerivationFormulasTest {

    private static final double DELTA = 0.001;

    /**
     * Create a minimal test config with simplified formulas for predictable testing.
     * Real config formulas are loaded from hero-core-defaults.json in production.
     * 
     * Zero base values ensure zero primaries → zero derivations.
     * Per-point multipliers match old hardcoded formula expectations.
     */
    private CoreConfig createTestConfig() {
        return new CoreConfig(
            new CoreConfig.ModifierStackingConfig(3.0, 5.0, "HIGHEST_WINS"),
            new CoreConfig.DamageConfig(1.5, 1.0, 0.9, Map.of()),
            new CoreConfig.HealConfig(1.5, false, false),
            new CoreConfig.ResourceRegenConfig(2.0f, 3.0, 8.0f),
            new CoreConfig.LevelingConfig(60, Map.of()),
            // Match old hardcoded formula expectations
            new CoreConfig.AttributeDerivationConfig(
                // Vitality: zero bases for test (so 0 vit → 0 health)
                new CoreConfig.VitalityDerivation(0.0, 0.0, 0.0, 0.0, 0.0),
                // Strength: attackDmg = str * 0.1 (100 → 10)
                new CoreConfig.StrengthDerivation(0.1, 0.0, 0.0),
                // Dexterity: crit = dex * 0.005, attackSpeed% = dex * 0.00333
                new CoreConfig.DexterityDerivation(0.005, 0.00333, 1.0, 0.0, 0.0),
                // Intelligence: spellPower = intel * 0.01 (50 intel + 50 faith → ~50 spell power)
                new CoreConfig.IntelligenceDerivation(0.01, 0.0, 0.0, 0.0, 0.0),
                // Faith: healPower = faith * 0.1 (100 → 10), zero bases for test
                new CoreConfig.FaithDerivation(0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                // Resolve: zero bases
                new CoreConfig.ResolveDerivation(0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
            ),
            // Defense: armor = vit * 0.5 (100 vit → 50 armor)
            new CoreConfig.DefenseDerivationConfig(0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
            Map.of(),
            Map.of()
        );
    }

    @Test
    void zeroPrimariesProduceNoDerivations() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // With zero primaries and zero base values, map should be empty or contain only near-zero values
        assertTrue(derived.isEmpty() || derived.values().stream().allMatch(v -> Math.abs(v) < DELTA), 
                   "Zero primaries should produce no non-zero derived attributes. Found: " + derived);
    }

    @Test
    void armorDerivedFromVitality() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 0, 100, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // VIT * 0.5 = 100 * 0.5 = 50
        assertEquals(50.0, derived.get(RPGAttribute.ARMOR), DELTA);
    }
    @Test
    void critChanceCapsAt100Percent() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 300, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // min(1.0, 300/200) = 1.0
        assertEquals(1.0, derived.get(RPGAttribute.CRIT_CHANCE), DELTA);
    }

    @Test
    void critDamageMultiplierDerivedFromStrengthAndIntelligence() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 100, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 1.5 + (100 + 100) / 500 = 1.5 + 0.4 = 1.9
        assertEquals(1.9, derived.get(RPGAttribute.CRIT_DAMAGE_MULTIPLIER), DELTA);
    }

    @Test
    void attackDamageDerivedFromStrength() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.ATTACK_DAMAGE), DELTA);
    }

    @Test
    void moveSpeedDerivedFromDexterity() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 100, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 1.0 + 100/500 = 1.2
        assertEquals(1.2, derived.get(RPGAttribute.MOVE_SPEED), DELTA);
    }

    @Test
    void attackSpeedDerivedFromDexterity() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 100, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 1.0 + 100/300 = 1.333...
        assertEquals(1.333, derived.get(RPGAttribute.ATTACK_SPEED), DELTA);
    }

    @Test
    void magicResistDerivedFromIntelligence() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 100, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.MAGIC_RESIST), DELTA);
    }

    @Test
    void healingPowerDerivedFromFaith() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 100, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.HEALING_POWER), DELTA);
    }

    @Test
    void miningSpeedDerivedFromStrength() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());
        // 1.0 + 100/500 = 1.2
        assertEquals(1.2, derived.get(RPGAttribute.MINING_SPEED), DELTA);
    }

    @Test
    void balancedBuildDerivesMultipleAttributes() {
        // 50 in all primaries — realistic balanced build
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(50, 50, 50, 50, 50, 50);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats, createTestConfig());

        // All derivations should be present (updated for new formula structure)
        assertTrue(derived.containsKey(RPGAttribute.ARMOR));               // was PHYSICAL_RESISTANCE
        assertTrue(derived.containsKey(RPGAttribute.SPELL_POWER));
        assertTrue(derived.containsKey(RPGAttribute.CRIT_CHANCE));
        assertTrue(derived.containsKey(RPGAttribute.ATTACK_DAMAGE));
        assertTrue(derived.containsKey(RPGAttribute.MOVE_SPEED));
        assertTrue(derived.containsKey(RPGAttribute.ATTACK_SPEED));
        assertTrue(derived.containsKey(RPGAttribute.MAGIC_RESIST));
        assertTrue(derived.containsKey(RPGAttribute.HEALING_POWER));
        assertTrue(derived.containsKey(RPGAttribute.MINING_SPEED));

        // Verify some values (updated for new config-driven formulas)
        assertEquals(25.0, derived.get(RPGAttribute.ARMOR), DELTA);          // 50 * 0.5
        assertEquals(0.5, derived.get(RPGAttribute.SPELL_POWER), DELTA);      // 50 * 0.01
        assertEquals(0.25, derived.get(RPGAttribute.CRIT_CHANCE), DELTA);     // 50 * 0.005
        assertEquals(5.0, derived.get(RPGAttribute.ATTACK_DAMAGE), DELTA);    // 50 * 0.1
    }
}
