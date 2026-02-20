package net.herotale.herocore.impl.heal;

import net.herotale.herocore.api.heal.HealType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HealFormulas} — pure math functions used by the heal pipeline.
 */
class HealFormulasTest {

    private static final float DELTA = 0.001f;

    // ── Healing Power Scaling ──────────────────────────────────────

    @Test
    void healingPowerScalesSpellHeals() {
        // 100 * (1 + 0.5) = 150
        assertEquals(150f, HealFormulas.applyHealingPowerScaling(100f, 0.5f, HealType.SPELL, false), DELTA);
    }

    @Test
    void healingPowerScalesPassiveHeals() {
        assertEquals(150f, HealFormulas.applyHealingPowerScaling(100f, 0.5f, HealType.PASSIVE, false), DELTA);
    }

    @Test
    void healingPowerDoesNotScalePotionHeals() {
        assertEquals(100f, HealFormulas.applyHealingPowerScaling(100f, 0.5f, HealType.POTION, false), DELTA);
    }

    @Test
    void healingPowerDoesNotScaleRegenTickByDefault() {
        assertEquals(100f, HealFormulas.applyHealingPowerScaling(100f, 0.5f, HealType.REGEN_TICK, false), DELTA);
    }

    @Test
    void healingPowerScalesRegenTickWhenEnabled() {
        assertEquals(150f, HealFormulas.applyHealingPowerScaling(100f, 0.5f, HealType.REGEN_TICK, true), DELTA);
    }

    @Test
    void healingPowerZeroLeavesUnchanged() {
        assertEquals(100f, HealFormulas.applyHealingPowerScaling(100f, 0f, HealType.SPELL, false), DELTA);
    }

    // ── Healing Received Bonus ─────────────────────────────────────

    @Test
    void healingReceivedBonusIncreasesHealing() {
        // 100 * (1 + 0.3) = 130
        assertEquals(130f, HealFormulas.applyHealingReceivedBonus(100f, 0.3f), DELTA);
    }

    @Test
    void healingReceivedBonusZeroLeavesUnchanged() {
        assertEquals(100f, HealFormulas.applyHealingReceivedBonus(100f, 0f), DELTA);
    }

    @Test
    void healingReceivedBonusNegativeReducesHealing() {
        // Negative bonus (debuff): 100 * (1 - 0.2) = 80
        assertEquals(80f, HealFormulas.applyHealingReceivedBonus(100f, -0.2f), DELTA);
    }

    // ── Heal Crit ──────────────────────────────────────────────────

    @Test
    void healCritMultiplierApplied() {
        assertEquals(150f, HealFormulas.applyHealCritMultiplier(100f, 1.5f), DELTA);
    }

    @Test
    void healCritRollSucceeds() {
        assertTrue(HealFormulas.rollHealCrit(0.5f, 0.3f));
    }

    @Test
    void healCritRollFails() {
        assertFalse(HealFormulas.rollHealCrit(0.5f, 0.7f));
    }

    @Test
    void healCritRollZeroChanceAlwaysFails() {
        assertFalse(HealFormulas.rollHealCrit(0f, 0f));
    }

    // ── Full pipeline simulation ───────────────────────────────────

    @Test
    void fullHealPipelineInSequence() {
        float heal = 100f;

        // 1. Healing power scaling: +50%
        heal = HealFormulas.applyHealingPowerScaling(heal, 0.5f, HealType.SPELL, false);
        assertEquals(150f, heal, DELTA);

        // 2. Healing received bonus: +20%
        heal = HealFormulas.applyHealingReceivedBonus(heal, 0.2f);
        assertEquals(180f, heal, DELTA);

        // 3. Heal crit: 1.5x
        heal = HealFormulas.applyHealCritMultiplier(heal, 1.5f);
        assertEquals(270f, heal, DELTA);
    }
}
