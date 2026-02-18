package net.herotale.herocore.impl.damage;

import net.herotale.herocore.api.damage.DamageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DamageFormulas} — pure math functions used by the damage pipeline.
 */
class DamageFormulasTest {

    private static final float DELTA = 0.001f;

    // ── Attack Damage Bonus ────────────────────────────────────────

    @Test
    void attackDamageBonusScalesDamage() {
        // 100 raw * (1 + 0.5 attack) = 150
        assertEquals(150f, DamageFormulas.applyAttackDamageBonus(100f, 0.5f), DELTA);
    }

    @Test
    void attackDamageBonusZeroLeavesUnchanged() {
        assertEquals(100f, DamageFormulas.applyAttackDamageBonus(100f, 0f), DELTA);
    }

    @Test
    void attackDamageBonusNegativeLeavesUnchanged() {
        // Negative attack damage should not apply
        assertEquals(100f, DamageFormulas.applyAttackDamageBonus(100f, -0.5f), DELTA);
    }

    // ── Fall Damage Reduction ──────────────────────────────────────

    @Test
    void fallDamageReductionReducesDamage() {
        // 100 * (1 - 0.3) = 70
        assertEquals(70f, DamageFormulas.applyFallDamageReduction(100f, 0.3f), DELTA);
    }

    @Test
    void fallDamageReductionCapsAt90Percent() {
        // Cap at 0.9: 100 * (1 - 0.9) = 10
        assertEquals(10f, DamageFormulas.applyFallDamageReduction(100f, 1.0f), DELTA);
    }

    @Test
    void fallDamageReductionZeroLeavesUnchanged() {
        assertEquals(100f, DamageFormulas.applyFallDamageReduction(100f, 0f), DELTA);
    }

    // ── Resistance Mitigation ──────────────────────────────────────

    @Test
    void resistanceReducesDamage() {
        // 100 * (1 - clamp(0.4 - 0.0, 0, 0.9)) = 100 * 0.6 = 60
        assertEquals(60f, DamageFormulas.applyResistance(100f, 0.4f, 0f, 0.9f), DELTA);
    }

    @Test
    void resistanceWithPenetration() {
        // 100 * (1 - clamp(0.4 - 0.2, 0, 0.9)) = 100 * 0.8 = 80
        assertEquals(80f, DamageFormulas.applyResistance(100f, 0.4f, 0.2f, 0.9f), DELTA);
    }

    @Test
    void resistancePenExceedsResist() {
        // penetration > resistance → no reduction
        assertEquals(100f, DamageFormulas.applyResistance(100f, 0.3f, 0.5f, 0.9f), DELTA);
    }

    @Test
    void resistanceCappedAtMax() {
        // resist is 0.95 but max is 0.9 → 100 * (1 - 0.9) = 10
        assertEquals(10f, DamageFormulas.applyResistance(100f, 0.95f, 0f, 0.9f), DELTA);
    }

    @Test
    void shouldApplyResistanceExcludesPhysicalAndTrue() {
        assertFalse(DamageFormulas.shouldApplyResistance(DamageType.PHYSICAL));
        assertFalse(DamageFormulas.shouldApplyResistance(DamageType.TRUE));
        assertTrue(DamageFormulas.shouldApplyResistance(DamageType.MAGICAL));
        assertTrue(DamageFormulas.shouldApplyResistance(DamageType.FIRE));
        assertTrue(DamageFormulas.shouldApplyResistance(DamageType.ICE));
        assertTrue(DamageFormulas.shouldApplyResistance(DamageType.PROJECTILE));
    }

    // ── Critical Hit ───────────────────────────────────────────────

    @Test
    void critMultiplierApplied() {
        assertEquals(200f, DamageFormulas.applyCritMultiplier(100f, 2.0f), DELTA);
    }

    @Test
    void critRollSucceeds() {
        assertTrue(DamageFormulas.rollCrit(0.5f, 0.3f));  // roll < chance
    }

    @Test
    void critRollFails() {
        assertFalse(DamageFormulas.rollCrit(0.5f, 0.7f)); // roll >= chance
    }

    @Test
    void critRollZeroChanceAlwaysFails() {
        assertFalse(DamageFormulas.rollCrit(0f, 0f));
    }

    // ── Lifesteal ──────────────────────────────────────────────────

    @Test
    void lifestealComputesHealAmount() {
        // 100 * 0.2 = 20
        assertEquals(20f, DamageFormulas.computeLifestealHeal(100f, 0.2f), DELTA);
    }

    @Test
    void lifestealZeroReturnsZero() {
        assertEquals(0f, DamageFormulas.computeLifestealHeal(100f, 0f), DELTA);
    }

    @Test
    void lifestealNegativeReturnsZero() {
        assertEquals(0f, DamageFormulas.computeLifestealHeal(100f, -0.1f), DELTA);
    }

    // ── Minimum Damage ─────────────────────────────────────────────

    @Test
    void minimumDamageEnforcesFloor() {
        assertEquals(1.0f, DamageFormulas.applyMinimumDamage(0.1f, 1.0f), DELTA);
    }

    @Test
    void minimumDamageLeavesHigherDamageAlone() {
        assertEquals(50f, DamageFormulas.applyMinimumDamage(50f, 1.0f), DELTA);
    }

    // ── Full pipeline simulation (formulas only) ───────────────────

    @Test
    void fullPipelineInSequence() {
        float dmg = 100f;

        // 1. Attack damage bonus: +50%
        dmg = DamageFormulas.applyAttackDamageBonus(dmg, 0.5f);
        assertEquals(150f, dmg, DELTA);

        // 2. Resistance: 30% resist, no pen, 0.9 cap
        dmg = DamageFormulas.applyResistance(dmg, 0.3f, 0f, 0.9f);
        assertEquals(105f, dmg, DELTA);

        // 3. Crit: 2x multiplier
        dmg = DamageFormulas.applyCritMultiplier(dmg, 2.0f);
        assertEquals(210f, dmg, DELTA);

        // 4. Lifesteal: 20%
        float heal = DamageFormulas.computeLifestealHeal(dmg, 0.2f);
        assertEquals(42f, heal, DELTA);

        // 5. Minimum damage
        dmg = DamageFormulas.applyMinimumDamage(dmg, 1.0f);
        assertEquals(210f, dmg, DELTA);
    }
}
