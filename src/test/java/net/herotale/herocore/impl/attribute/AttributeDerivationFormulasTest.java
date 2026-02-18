package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AttributeDerivationFormulas} — pure math derivation of
 * secondary attributes from primary stats.
 */
class AttributeDerivationFormulasTest {

    private static final double DELTA = 0.001;

    @Test
    void zeroPrimariesProduceNoDerivations() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        assertTrue(derived.isEmpty(), "Zero primaries should produce no derived attributes");
    }

    @Test
    void physicalResistanceDerivedFromVitalityAndResolve() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 0, 100, 50);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // (100 + 50) / 2 = 75
        assertEquals(75.0, derived.get(RPGAttribute.PHYSICAL_RESISTANCE), DELTA);
    }

    @Test
    void spellPowerDerivedFromIntelligenceAndFaith() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 80, 60, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // (80 + 60) / 2 = 70
        assertEquals(70.0, derived.get(RPGAttribute.SPELL_POWER), DELTA);
    }

    @Test
    void critChanceDerivedFromDexterity() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 100, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 100 / 200 = 0.5
        assertEquals(0.5, derived.get(RPGAttribute.CRIT_CHANCE), DELTA);
    }

    @Test
    void critChanceCapsAt100Percent() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 300, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // min(1.0, 300/200) = 1.0
        assertEquals(1.0, derived.get(RPGAttribute.CRIT_CHANCE), DELTA);
    }

    @Test
    void critDamageMultiplierDerivedFromStrengthAndIntelligence() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 100, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 1.5 + (100 + 100) / 500 = 1.5 + 0.4 = 1.9
        assertEquals(1.9, derived.get(RPGAttribute.CRIT_DAMAGE_MULTIPLIER), DELTA);
    }

    @Test
    void attackDamageDerivedFromStrength() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.ATTACK_DAMAGE), DELTA);
    }

    @Test
    void moveSpeedDerivedFromDexterity() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 100, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 1.0 + 100/500 = 1.2
        assertEquals(1.2, derived.get(RPGAttribute.MOVE_SPEED), DELTA);
    }

    @Test
    void attackSpeedDerivedFromDexterity() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 100, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 1.0 + 100/300 = 1.333...
        assertEquals(1.333, derived.get(RPGAttribute.ATTACK_SPEED), DELTA);
    }

    @Test
    void magicResistDerivedFromIntelligence() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 100, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.MAGIC_RESIST), DELTA);
    }

    @Test
    void healingPowerDerivedFromFaith() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(0, 0, 0, 100, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 100 / 10 = 10
        assertEquals(10.0, derived.get(RPGAttribute.HEALING_POWER), DELTA);
    }

    @Test
    void miningSpeedDerivedFromStrength() {
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(100, 0, 0, 0, 0, 0);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);
        // 1.0 + 100/500 = 1.2
        assertEquals(1.2, derived.get(RPGAttribute.MINING_SPEED), DELTA);
    }

    @Test
    void balancedBuildDerivesMultipleAttributes() {
        // 50 in all primaries — realistic balanced build
        HeroCoreStatsComponent stats = new HeroCoreStatsComponent(50, 50, 50, 50, 50, 50);
        Map<RPGAttribute, Double> derived = AttributeDerivationFormulas.computeDerived(stats);

        // All derivations should be present
        assertTrue(derived.containsKey(RPGAttribute.PHYSICAL_RESISTANCE));
        assertTrue(derived.containsKey(RPGAttribute.SPELL_POWER));
        assertTrue(derived.containsKey(RPGAttribute.CRIT_CHANCE));
        assertTrue(derived.containsKey(RPGAttribute.ATTACK_DAMAGE));
        assertTrue(derived.containsKey(RPGAttribute.MOVE_SPEED));
        assertTrue(derived.containsKey(RPGAttribute.ATTACK_SPEED));
        assertTrue(derived.containsKey(RPGAttribute.MAGIC_RESIST));
        assertTrue(derived.containsKey(RPGAttribute.HEALING_POWER));
        assertTrue(derived.containsKey(RPGAttribute.MINING_SPEED));

        // Verify some values
        assertEquals(50.0, derived.get(RPGAttribute.PHYSICAL_RESISTANCE), DELTA); // (50+50)/2
        assertEquals(50.0, derived.get(RPGAttribute.SPELL_POWER), DELTA);          // (50+50)/2
        assertEquals(0.25, derived.get(RPGAttribute.CRIT_CHANCE), DELTA);          // 50/200
        assertEquals(5.0, derived.get(RPGAttribute.ATTACK_DAMAGE), DELTA);         // 50/10
    }
}
