package net.herotale.herocore.system.heal;

import net.herotale.herocore.api.attribute.*;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.heal.*;
import net.herotale.herocore.api.system.HealSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for individual heal systems executed in isolation.
 * <p>
 * In the ECS architecture each system is a standalone event handler —
 * there is no central orchestrator.  The Hytale event bus dispatches
 * systems in topological order declared via {@code @SystemOrder}.
 */
class HealSystemsTest {

    private StatsComponent healerStats;
    private StatsComponent targetStats;

    private final UUID healer = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        healerStats = new StatsComponent();
        targetStats = new StatsComponent();
    }

    // ── helpers ────────────────────────────────────────────────────

    private HealEvent event(double rawAmount, HealType type) {
        return HealEvent.builder()
                .healer(healer)
                .target(target)
                .rawAmount(rawAmount)
                .healType(type)
                .build();
    }

    /** Runs every default heal system in declared order, skipping disabled/cancelled. */
    private void runAllSystems(HealEvent event, HealSystem... systems) {
        for (HealSystem sys : systems) {
            if (event.isCancelled()) break;
            if (!sys.isEnabled()) continue;
            sys.onHeal(event, healerStats, targetStats);
        }
    }

    private HealSystem[] defaultSystems() {
        return new HealSystem[] {
                new HealingPowerScalingSystem(false),
                new HealingReceivedBonusSystem(),
                new HealCritSystem(false)   // crit disabled for deterministic tests
        };
    }

    // ── HealingPowerScalingSystem ─────────────────────────────────

    @Test
    void healingPower_scalesSpellHeals() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.5); // +50%

        HealEvent e = event(100.0, HealType.SPELL);
        new HealingPowerScalingSystem(false).onHeal(e, healerStats, targetStats);
        // 100 * (1 + 0.5) = 150
        assertEquals(150.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void healingPower_doesNotScalePotions() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.5);

        HealEvent e = event(100.0, HealType.POTION);
        new HealingPowerScalingSystem(false).onHeal(e, healerStats, targetStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void healingPower_scalesPassiveHeals() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.3);

        HealEvent e = event(80.0, HealType.PASSIVE);
        new HealingPowerScalingSystem(false).onHeal(e, healerStats, targetStats);
        // 80 * (1 + 0.3) = 104
        assertEquals(104.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void healingPower_regenTickScalingWhenEnabled() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.4);

        HealEvent e = event(50.0, HealType.REGEN_TICK);
        new HealingPowerScalingSystem(true).onHeal(e, healerStats, targetStats);
        // 50 * (1 + 0.4) = 70
        assertEquals(70.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void healingPower_regenTickNotScaledByDefault() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.4);

        HealEvent e = event(50.0, HealType.REGEN_TICK);
        new HealingPowerScalingSystem(false).onHeal(e, healerStats, targetStats);
        assertEquals(50.0, e.getModifiedAmount(), 0.001);
    }

    // ── HealingReceivedBonusSystem ────────────────────────────────

    @Test
    void healingReceivedBonus_applies() {
        targetStats.setBase(RPGAttribute.HEALING_RECEIVED_BONUS, 0.3); // +30%

        HealEvent e = event(100.0, HealType.POTION);
        new HealingReceivedBonusSystem().onHeal(e, healerStats, targetStats);
        // 100 * (1 + 0.3) = 130
        assertEquals(130.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void healingReceivedBonus_noopWithoutTargetStats() {
        HealEvent e = event(100.0, HealType.POTION);
        new HealingReceivedBonusSystem().onHeal(e, healerStats, null);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    // ── HealCritSystem ────────────────────────────────────────────

    @Test
    void healCrit_triggersAtGuaranteedChance() {
        healerStats.setBase(RPGAttribute.HEAL_CRIT_CHANCE, 1.0);       // 100%
        healerStats.setBase(RPGAttribute.HEAL_CRIT_MULTIPLIER, 2.0);

        HealEvent e = event(100.0, HealType.SPELL);
        new HealCritSystem(true, new Random(0)).onHeal(e, healerStats, targetStats);
        assertEquals(200.0, e.getModifiedAmount(), 0.001);
        assertTrue(e.isCrit());
    }

    @Test
    void healCrit_doesNotTriggerAtZeroChance() {
        HealEvent e = event(100.0, HealType.SPELL);
        new HealCritSystem(true, new Random(0)).onHeal(e, healerStats, targetStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
        assertFalse(e.isCrit());
    }

    // ── Full system-order simulation (integration) ──────────────

    @Test
    void allSystems_rawHealUnchanged_noScaling() {
        HealEvent e = event(50.0, HealType.POTION);
        runAllSystems(e, defaultSystems());
        assertEquals(50.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void allSystems_healingPowerAndBonus_stack() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.5);        // +50%
        targetStats.setBase(RPGAttribute.HEALING_RECEIVED_BONUS, 0.3); // +30%

        HealEvent e = event(100.0, HealType.SPELL);
        runAllSystems(e, defaultSystems());
        // 100 * 1.5 = 150 → 150 * 1.3 = 195
        assertEquals(195.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void allSystems_cancelledHealStopsProcessing() {
        HealEvent e = event(100.0, HealType.SPELL);
        e.setCancelled(true);
        runAllSystems(e, defaultSystems());
        assertEquals(100.0, e.getModifiedAmount());
    }

    @Test
    void allSystems_disabledSystemIsSkipped() {
        healerStats.setBase(RPGAttribute.HEALING_POWER, 0.5);

        HealSystem[] systems = defaultSystems();
        // Disable healing power system (index 0)
        systems[0].setEnabled(false);

        HealEvent e = event(100.0, HealType.SPELL);
        runAllSystems(e, systems);
        // Healing power system disabled → no scaling
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    // ── System identity ───────────────────────────────────────────

    @Test
    void systemIds_areCorrect() {
        assertEquals("herocore:healing_power_scaling", new HealingPowerScalingSystem(false).getId());
        assertEquals("herocore:healing_received_bonus", new HealingReceivedBonusSystem().getId());
        assertEquals("herocore:heal_crit", new HealCritSystem(false).getId());
    }
}
