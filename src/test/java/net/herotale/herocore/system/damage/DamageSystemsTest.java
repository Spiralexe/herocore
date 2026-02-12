package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.*;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.*;
import net.herotale.herocore.api.heal.HealEvent;
import net.herotale.herocore.api.system.DamageSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for individual damage systems executed in isolation.
 * <p>
 * In the ECS architecture each system is a standalone event handler —
 * there is no central orchestrator.  The Hytale event bus dispatches
 * systems in topological order declared via {@code @SystemOrder}.
 */
class DamageSystemsTest {

    private StatsComponent attackerStats;
    private StatsComponent victimStats;

    private final UUID attacker = UUID.randomUUID();
    private final UUID victim = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        attackerStats = new StatsComponent();
        victimStats = new StatsComponent();
    }

    // ── helpers ────────────────────────────────────────────────────

    private DamageEvent event(double rawAmount, DamageType type) {
        return DamageEvent.builder()
                .attacker(attacker)
                .victim(victim)
                .rawAmount(rawAmount)
                .damageType(type)
                .build();
    }

    /** Runs every default system in declared order, skipping disabled/cancelled. */
    private void runAllSystems(DamageEvent event, DamageSystem... systems) {
        for (DamageSystem sys : systems) {
            if (event.isCancelled()) break;
            if (!sys.isEnabled()) continue;
            sys.onDamage(event, attackerStats, victimStats);
        }
    }

    private DamageSystem[] defaultSystems() {
        return new DamageSystem[] {
                new AttackDamageBonusSystem(),
                new FallDamageReductionSystem(),
                new ResistanceMitigationSystem(),
                new CriticalHitSystem(1.5, new Random(0)), // fixed seed: no crit at 0 chance
                new LifestealSystem(e -> {}),
                new MinimumDamageSystem(1.0)
        };
    }

    // ── AttackDamageBonusSystem ────────────────────────────────────

    @Test
    void attackDamageBonus_addsScaling() {
        attackerStats.setBase(RPGAttribute.ATTACK_DAMAGE, 0.5); // +50%

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new AttackDamageBonusSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(150.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void attackDamageBonus_noopWithoutAttackerStats() {
        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new AttackDamageBonusSystem().onDamage(e, null, victimStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    // ── FallDamageReductionSystem ─────────────────────────────────

    @Test
    void fallDamageReduction_reducesFallDamage() {
        victimStats.setBase(RPGAttribute.FALL_DAMAGE_REDUCTION, 0.5);

        DamageEvent e = event(100.0, DamageType.FALL);
        new FallDamageReductionSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(50.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void fallDamageReduction_ignoresNonFall() {
        victimStats.setBase(RPGAttribute.FALL_DAMAGE_REDUCTION, 0.5);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new FallDamageReductionSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    // ── ResistanceMitigationSystem ────────────────────────────────

    @Test
    void physicalDamage_skippedByMitigationSystem() {
        // Physical is handled by Hytale native ArmorDamageReduction via DefenseBridge.
        // ResistanceMitigationSystem must NOT touch it.
        victimStats.setBase(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT, 0.5);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001,
                "Physical damage must pass through unmodified — Hytale native handles it");
    }

    @Test
    void trueDamageSkipsResistance() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 0.9);

        DamageEvent e = event(50.0, DamageType.TRUE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(50.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void fireResistancePercent_appliesDirectly() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 0.4);

        DamageEvent e = event(100.0, DamageType.FIRE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(60.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void fireResistance_fallsBackToLegacyAttribute() {
        victimStats.setBase(RPGAttribute.ELEMENTAL_RESIST_FIRE, 40.0);

        DamageEvent e = event(100.0, DamageType.FIRE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(60.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void projectileResistance_reducesProjectileDamage() {
        victimStats.setBase(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT, 0.25);

        DamageEvent e = event(100.0, DamageType.PROJECTILE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(75.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void magicalResistance_reducesMagicalDamage() {
        victimStats.setBase(RPGAttribute.MAGIC_RESIST, 0.35);

        DamageEvent e = event(100.0, DamageType.MAGICAL);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(65.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void iceResistancePercent_appliesDirectly() {
        victimStats.setBase(RPGAttribute.ICE_RESISTANCE_PERCENT, 0.3);

        DamageEvent e = event(100.0, DamageType.ICE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(70.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void iceResistance_fallsBackToLegacy() {
        victimStats.setBase(RPGAttribute.ELEMENTAL_RESIST_ICE, 50.0);

        DamageEvent e = event(100.0, DamageType.ICE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(50.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void lightningResistancePercent_appliesDirectly() {
        victimStats.setBase(RPGAttribute.LIGHTNING_RESISTANCE_PERCENT, 0.2);

        DamageEvent e = event(100.0, DamageType.LIGHTNING);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(80.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void poisonResistancePercent_appliesDirectly() {
        victimStats.setBase(RPGAttribute.POISON_RESISTANCE_PERCENT, 0.45);

        DamageEvent e = event(100.0, DamageType.POISON);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(55.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void arcaneResistancePercent_appliesDirectly() {
        victimStats.setBase(RPGAttribute.ARCANE_RESISTANCE_PERCENT, 0.6);

        DamageEvent e = event(100.0, DamageType.ARCANE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(40.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void magicPenetration_reducesElementalResistance() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 0.5);
        attackerStats.setBase(RPGAttribute.MAGIC_PENETRATION, 0.2);

        DamageEvent e = event(100.0, DamageType.FIRE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        // Effective = 0.5 - 0.2 = 0.3 → 70 damage
        assertEquals(70.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void resistanceCapsAt90PercentForElemental() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 2.0);

        DamageEvent e = event(100.0, DamageType.FIRE);
        new ResistanceMitigationSystem().onDamage(e, attackerStats, victimStats);
        assertEquals(10.0, e.getModifiedAmount(), 0.001);
    }

    // ── CriticalHitSystem ─────────────────────────────────────────

    @Test
    void critDoesNotTriggerAtZeroChance() {
        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new CriticalHitSystem(1.5, new Random(0)).onDamage(e, attackerStats, victimStats);
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
        assertFalse(e.getFlags().contains(DamageFlag.CRIT));
    }

    @Test
    void critTriggersAtGuaranteedChance() {
        attackerStats.setBase(RPGAttribute.CRIT_CHANCE, 1.0);   // 100%
        attackerStats.setBase(RPGAttribute.CRIT_DAMAGE_MULTIPLIER, 2.0);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        new CriticalHitSystem(1.5, new Random(0)).onDamage(e, attackerStats, victimStats);
        assertEquals(200.0, e.getModifiedAmount(), 0.001);
        assertTrue(e.getFlags().contains(DamageFlag.CRIT));
    }

    // ── MinimumDamageSystem ───────────────────────────────────────

    @Test
    void minimumDamageEnforced() {
        DamageEvent e = event(0.3, DamageType.PHYSICAL);
        new MinimumDamageSystem(1.0).onDamage(e, attackerStats, victimStats);
        assertTrue(e.getModifiedAmount() >= 1.0);
    }

    // ── LifestealSystem ───────────────────────────────────────────

    @Test
    void lifestealEmitsHealEvent() {
        AtomicReference<HealEvent> captured = new AtomicReference<>();
        LifestealSystem lifesteal = new LifestealSystem(captured::set);

        attackerStats.setBase(RPGAttribute.LIFESTEAL, 0.2);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        lifesteal.onDamage(e, attackerStats, victimStats);

        assertNotNull(captured.get(), "Lifesteal should emit a HealEvent");
        assertEquals(20.0, captured.get().getRawAmount(), 0.001);
    }

    @Test
    void lifestealNoopWithoutAttribute() {
        AtomicReference<HealEvent> captured = new AtomicReference<>();
        LifestealSystem lifesteal = new LifestealSystem(captured::set);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        lifesteal.onDamage(e, attackerStats, victimStats);

        assertNull(captured.get(), "No heal event when lifesteal is 0");
    }

    // ── Full system-order simulation (integration) ──────────────

    @Test
    void allSystems_rawAmountUntouched_noCritNoArmor() {
        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        runAllSystems(e, defaultSystems());
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void allSystems_physicalPassesThroughMitigation() {
        // Physical is NOT mitigated by ResistanceMitigationSystem
        // (Hytale native handles it) so damage flows through unchanged
        victimStats.setBase(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT, 0.9);

        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        runAllSystems(e, defaultSystems());
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    @Test
    void allSystems_fireResistanceReducesThenMinApplies() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 2.0); // caps at 90%

        DamageEvent e = event(0.5, DamageType.FIRE);
        runAllSystems(e, defaultSystems());
        // 0.5 * 0.1 = 0.05 → clamped to 1.0 minimum
        assertTrue(e.getModifiedAmount() >= 1.0);
    }

    @Test
    void allSystems_cancelledEventStopsProcessing() {
        DamageEvent e = event(100.0, DamageType.PHYSICAL);
        e.setCancelled(true);
        runAllSystems(e, defaultSystems());
        // No system touched the amount
        assertEquals(100.0, e.getModifiedAmount());
    }

    @Test
    void allSystems_disabledSystemIsSkipped() {
        victimStats.setBase(RPGAttribute.FIRE_RESISTANCE_PERCENT, 0.5);

        DamageSystem[] systems = defaultSystems();
        // Disable resistance mitigation (index 2)
        systems[2].setEnabled(false);

        DamageEvent e = event(100.0, DamageType.FIRE);
        runAllSystems(e, systems);
        // Resistance system disabled → no reduction
        assertEquals(100.0, e.getModifiedAmount(), 0.001);
    }

    // ── System identity ───────────────────────────────────────────

    @Test
    void systemIds_areCorrect() {
        assertEquals("herocore:attack_damage_bonus", new AttackDamageBonusSystem().getId());
        assertEquals("herocore:fall_damage_reduction", new FallDamageReductionSystem().getId());
        assertEquals("herocore:resistance_mitigation", new ResistanceMitigationSystem().getId());
        assertEquals("herocore:critical_hit", new CriticalHitSystem(1.5).getId());
        assertEquals("herocore:lifesteal", new LifestealSystem(e -> {}).getId());
        assertEquals("herocore:minimum_damage", new MinimumDamageSystem(1.0).getId());
    }
}
