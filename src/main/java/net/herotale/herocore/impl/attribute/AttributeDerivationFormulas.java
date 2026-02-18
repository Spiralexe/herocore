package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Stateless formulas for deriving secondary attributes from primary attributes.
 * 
 * Maps primary attributes (Strength, Vitality, etc.) to secondary attributes
 * (Physical Defense, Health scaling, etc.) using balance-configurable formulas.
 * 
 * <b>No state or modifiers here.</b> This is pure math on base values.
 * The AttributeDerivationSystem applies these results to EntityStatMap
 * via StaticModifier entries, which then handles stacking, bonuses, and caps.
 */
public class AttributeDerivationFormulas {

    /**
     * Computes all derived secondary attributes from the given primary attribute values.
     * 
     * Returns a map of {@code RPGAttribute → computed value} for each derived attribute.
     * Only includes attributes that have non-zero derived values.
     * 
     * @param primaryStats the entity's HeroCoreStatsComponent with primary values
     * @return map of derived attributes, or empty if none
     */
    public static Map<RPGAttribute, Double> computeDerived(
            HeroCoreStatsComponent primaryStats) {

        Map<RPGAttribute, Double> derived = new HashMap<>();

        double str = primaryStats.getStrength();
        double dex = primaryStats.getDexterity();
        double intel = primaryStats.getIntelligence();
        double faith = primaryStats.getFaith();
        double vit = primaryStats.getVitality();
        double res = primaryStats.getResolve();

        // ── Physical Resistance (Armor) ───────────────────────────────
        // Derived from Vitality + Resolve
        // Formula: base = (VIT + RES) / 2
        double physicalResistance = (vit + res) / 2.0;
        if (physicalResistance > 0.0) {
            derived.put(RPGAttribute.PHYSICAL_RESISTANCE, physicalResistance);
        }

        // ── Spell Power ───────────────────────────────────────────────
        // Derived from Intelligence + Faith
        // Formula: base = (INT + FAITH) / 2
        double spellPower = (intel + faith) / 2.0;
        if (spellPower > 0.0) {
            derived.put(RPGAttribute.SPELL_POWER, spellPower);
        }

        // ── Crit Chance ───────────────────────────────────────────────
        // Derived from Dexterity
        // Formula: base = DEX / 200 (so 100 DEX = 50% crit, capped at 100%)
        // Stored as flat value 0.0-1.0 (100% = 1.0)
        double critChance = Math.min(1.0, dex / 200.0);
        if (critChance > 0.0) {
            derived.put(RPGAttribute.CRIT_CHANCE, critChance);
        }

        // ── Crit Damage Multiplier ────────────────────────────────────
        // Derived from Strength + Intelligence
        // Formula: base = 1.5 + (STR + INT) / 500
        // (minimum 1.5x, increases with stats)
        double critDmg = 1.5 + (str + intel) / 500.0;
        if (critDmg > 1.5) {
            derived.put(RPGAttribute.CRIT_DAMAGE_MULTIPLIER, critDmg);
        }

        // ── Attack Damage ─────────────────────────────────────────────
        // Derived from Strength
        // Formula: base = STR / 10
        // (so 100 STR = 10 base damage bonus)
        double attackDamage = str / 10.0;
        if (attackDamage > 0.0) {
            derived.put(RPGAttribute.ATTACK_DAMAGE, attackDamage);
        }

        // ── Move Speed ─────────────────────────────────────────────────
        // Derived from Dexterity
        // Formula: base = 1.0 + (DEX / 500)
        // (100 DEX = 1.2x speed)
        double moveSpeed = 1.0 + (dex / 500.0);
        if (moveSpeed > 1.0) {
            derived.put(RPGAttribute.MOVE_SPEED, moveSpeed);
        }

        // ── Attack Speed ──────────────────────────────────────────────
        // Derived from Dexterity
        // Formula: base = 1.0 + (DEX / 300)
        // (100 DEX = 1.33x attack speed)
        double attackSpeed = 1.0 + (dex / 300.0);
        if (attackSpeed > 1.0) {
            derived.put(RPGAttribute.ATTACK_SPEED, attackSpeed);
        }

        // ── Magic Resist ──────────────────────────────────────────────
        // Derived from Intelligence
        // Formula: base = INT / 10
        // (100 INT = 10 magic resist)
        double magicResist = intel / 10.0;
        if (magicResist > 0.0) {
            derived.put(RPGAttribute.MAGIC_RESIST, magicResist);
        }

        // ── Healing Power ─────────────────────────────────────────────
        // Derived from Faith
        // Formula: base = FAITH / 10
        // (100 FAITH = 10 healing power bonus)
        double healingPower = faith / 10.0;
        if (healingPower > 0.0) {
            derived.put(RPGAttribute.HEALING_POWER, healingPower);
        }

        // ── Mining Speed ──────────────────────────────────────────────
        // Derived from Strength
        // Formula: base = 1.0 + (STR / 500)
        // (100 STR = 1.2x mining speed)
        double miningSpeed = 1.0 + (str / 500.0);
        if (miningSpeed > 1.0) {
            derived.put(RPGAttribute.MINING_SPEED, miningSpeed);
        }

        return derived;
    }

    private AttributeDerivationFormulas() {}
}
