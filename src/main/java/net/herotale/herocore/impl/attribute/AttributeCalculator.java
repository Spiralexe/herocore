package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.AttributeModifier;
import net.herotale.herocore.api.attribute.ModifierType;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.impl.config.CoreConfig;

import java.util.List;

/**
 * Stateless math utility for attribute computation.
 * <p>
 * Implements the canonical stacking formula:
 * <pre>
 * FinalValue = (Base + Σ FLAT) * (1 + Σ PERCENT_ADDITIVE) * Π (1 + each PERCENT_MULTIPLICATIVE)
 * Then apply OVERRIDE.
 * </pre>
 * <p>
 * Used internally by {@link StatsComponent}. This class has no state of its own
 * and is safe to share across threads.
 */
public final class AttributeCalculator {

    private final double maxPercentAdditive;
    private final double multiplicativeCap;
    private final CoreConfig.AttributeDerivationConfig derivation;
    private final CoreConfig.DefenseDerivationConfig defenseDerivation;

    public AttributeCalculator(double maxPercentAdditive, double multiplicativeCap,
                               CoreConfig.AttributeDerivationConfig derivation,
                               CoreConfig.DefenseDerivationConfig defenseDerivation) {
        this.maxPercentAdditive = maxPercentAdditive;
        this.multiplicativeCap = multiplicativeCap;
        this.derivation = derivation;
        this.defenseDerivation = defenseDerivation;
    }

    public AttributeCalculator(double maxPercentAdditive, double multiplicativeCap,
                               CoreConfig.AttributeDerivationConfig derivation) {
        this(maxPercentAdditive, multiplicativeCap, derivation, null);
    }

    public AttributeCalculator(double maxPercentAdditive, double multiplicativeCap) {
        this(maxPercentAdditive, multiplicativeCap, null, null);
    }

    public AttributeCalculator() {
        this(3.0, 5.0, null, null);
    }

    public boolean hasDerivations() {
        return derivation != null || defenseDerivation != null;
    }

    /**
     * Compute the final value for a given base and list of modifiers.
     *
     * @param base      the base value
     * @param modifiers the list of active modifiers
     * @return the computed final value
     */
    public double compute(double base, List<AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return base;
        }

        double flatSum = 0.0;
        double percentAdditiveSum = 0.0;
        double multiplicativeProduct = 1.0;
        Double override = null;

        for (AttributeModifier mod : modifiers) {
            switch (mod.getType()) {
                case FLAT -> flatSum += mod.getValue();
                case PERCENT_ADDITIVE -> percentAdditiveSum += mod.getValue();
                case PERCENT_MULTIPLICATIVE -> multiplicativeProduct *= (1.0 + mod.getValue());
                case OVERRIDE -> override = mod.getValue();
            }
        }

        // Apply caps
        percentAdditiveSum = Math.min(percentAdditiveSum, maxPercentAdditive);
        multiplicativeProduct = Math.min(multiplicativeProduct, multiplicativeCap);

        // Step 1: Base + flat
        double result = base + flatSum;
        // Step 2: Percent additive
        result *= (1.0 + percentAdditiveSum);
        // Step 3: Percent multiplicative
        result *= multiplicativeProduct;
        // Step 4: Override
        if (override != null) {
            result = override;
        }

        return result;
    }

        /**
         * Compute derived secondary attributes from primaries.
         * Called only after all primary modifiers are applied.
         */
        public void computeDerivedAttributes(StatsComponent stats) {
        if (derivation == null) {
            return;
        }

        // === VITALITY ===
        double vitality = stats.getValue(RPGAttribute.VITALITY);
        double maxHealth = derivation.vitality().healthBase()
            + (vitality * derivation.vitality().healthPerPoint());
        stats.setDerivedBase(RPGAttribute.MAX_HEALTH, maxHealth);

        double healthRegen = derivation.vitality().healthRegenBase()
            + (vitality * derivation.vitality().healthRegenPerPoint());
        stats.setDerivedBase(RPGAttribute.HEALTH_REGEN, healthRegen);

        double armorPercent = vitality * derivation.vitality().armorPercentPerPoint();
        stats.addDerivedModifier(RPGAttribute.ARMOR, armorPercent, ModifierType.PERCENT_ADDITIVE);

        // === STRENGTH ===
        double strength = stats.getValue(RPGAttribute.STRENGTH);
        double basePhysicalDamage = strength * derivation.strength().baseAttackDamagePerPoint();
        stats.setDerivedBase(RPGAttribute.ATTACK_DAMAGE, basePhysicalDamage);

        double blockStrength = strength * derivation.strength().blockStrengthPerPoint();
        stats.setDerivedBase(RPGAttribute.BLOCK_STRENGTH, blockStrength);

        double shieldStrength = strength * derivation.strength().shieldStrengthPerPoint();
        stats.setDerivedBase(RPGAttribute.SHIELD_STRENGTH, shieldStrength);

        // === DEXTERITY ===
        double dexterity = stats.getValue(RPGAttribute.DEXTERITY);
        double physCritChance = dexterity * derivation.dexterity().critChancePerPoint();
        stats.setDerivedBase(RPGAttribute.CRIT_CHANCE, physCritChance);

        double attackSpeed = dexterity * derivation.dexterity().attackSpeedPercentPerPoint();
        stats.addDerivedModifier(RPGAttribute.ATTACK_SPEED, attackSpeed, ModifierType.PERCENT_ADDITIVE);

        double dodgeRating = dexterity * derivation.dexterity().dodgeRatingPerPoint();
        stats.setDerivedBase(RPGAttribute.DODGE_RATING, dodgeRating);

        double fallReduction = dexterity * derivation.dexterity().fallDamageReductionPerPoint();
        fallReduction = Math.min(fallReduction, derivation.dexterity().fallDamageReductionCap());
        stats.setDerivedBase(RPGAttribute.FALL_DAMAGE_REDUCTION, fallReduction);

        // === INTELLIGENCE ===
        double intelligence = stats.getValue(RPGAttribute.INTELLIGENCE);
        double spellPower = intelligence * derivation.intelligence().spellPowerPercentPerPoint();
        stats.setDerivedBase(RPGAttribute.SPELL_POWER, spellPower);

        double maxManaFromInt = derivation.intelligence().manaBase()
            + (intelligence * derivation.intelligence().manaPerPoint());
        stats.setDerivedBase(RPGAttribute.MAX_MANA, maxManaFromInt);

        double spellCritChance = intelligence * derivation.intelligence().spellCritChancePerPoint();
        stats.setDerivedBase(RPGAttribute.SPELL_CRIT_CHANCE, spellCritChance);

        double spellCritMultiplier = intelligence * derivation.intelligence().spellCritMultiplierPerPoint();
        stats.addDerivedModifier(RPGAttribute.SPELL_CRIT_MULTIPLIER, spellCritMultiplier, ModifierType.FLAT);

        // === FAITH ===
        double faith = stats.getValue(RPGAttribute.FAITH);
        double healingPower = faith * derivation.faith().healingPowerPercentPerPoint();
        stats.setDerivedBase(RPGAttribute.HEALING_POWER, healingPower);

        double maxManaFromFaith = derivation.faith().manaBase()
            + (faith * derivation.faith().manaPerPoint());
        stats.addDerivedModifier(RPGAttribute.MAX_MANA, maxManaFromFaith, ModifierType.FLAT);

        double manaRegen = derivation.faith().manaRegenBase()
            + (faith * derivation.faith().manaRegenPerPoint());
        stats.setDerivedBase(RPGAttribute.MANA_REGEN, manaRegen);

        double healCritChance = faith * derivation.faith().healCritChancePerPoint();
        stats.setDerivedBase(RPGAttribute.HEAL_CRIT_CHANCE, healCritChance);

        double buffStrength = faith * derivation.faith().buffStrengthPercentPerPoint();
        stats.setDerivedBase(RPGAttribute.BUFF_STRENGTH, buffStrength);

        // === RESOLVE ===
        double resolve = stats.getValue(RPGAttribute.RESOLVE);
        double resolveHealth = resolve * derivation.resolve().healthPerPoint();
        stats.addDerivedModifier(RPGAttribute.MAX_HEALTH, resolveHealth, ModifierType.FLAT);

        double ccResist = resolve * derivation.resolve().ccResistancePerPoint();
        ccResist = Math.min(ccResist, derivation.resolve().ccResistanceCap());
        stats.setDerivedBase(RPGAttribute.CC_RESISTANCE, ccResist);

        double debuffResist = resolve * derivation.resolve().debuffResistancePerPoint();
        debuffResist = Math.min(debuffResist, derivation.resolve().debuffResistanceCap());
        stats.setDerivedBase(RPGAttribute.DEBUFF_RESISTANCE, debuffResist);

        double threatGen = 1.0 + (resolve * derivation.resolve().threatGenerationPercentPerPoint());
        stats.setDerivedBase(RPGAttribute.THREAT_GENERATION, threatGen);

        double staminaRegenBonus = resolve * derivation.resolve().staminaRegenPerPoint();
        stats.addDerivedModifier(RPGAttribute.STAMINA_REGEN, staminaRegenBonus, ModifierType.FLAT);

        double magicResistBonus = resolve * derivation.resolve().magicResistPercentPerPoint();
        stats.addDerivedModifier(RPGAttribute.MAGIC_RESIST, magicResistBonus, ModifierType.PERCENT_ADDITIVE);

        // === DEFENSE DERIVATION (Hytale-native resistance attributes) ===
        computeDefenseDerivedAttributes(stats);
        }

    /**
     * Derives Hytale-native defense resistance attributes from primary stats.
     * <p>
     * These attributes are designed to be synced to Hytale's {@code ArmorDamageReduction}
     * system via {@code DefenseBridge}. They support both flat (ADDITIVE) and percent
     * (MULTIPLICATIVE) resistance per {@code DamageCause}.
     * <p>
     * Defense values are additive from multiple sources: level progression, gear,
     * buffs, and primary attribute derivation. The {@code DefenseBridge} converts
     * the final computed value into Hytale-native {@code StaticModifier}s.
     */
    public void computeDefenseDerivedAttributes(StatsComponent stats) {
        if (defenseDerivation == null) {
            return;
        }

        double cap = defenseDerivation.maxResistancePercent();

        // Physical resistance from VITALITY
        double vitality = stats.getValue(RPGAttribute.VITALITY);
        double physFlat = vitality * defenseDerivation.physicalResistanceFlatPerVitality();
        stats.addDerivedModifier(RPGAttribute.PHYSICAL_RESISTANCE, physFlat, ModifierType.FLAT);

        double physPercent = vitality * defenseDerivation.physicalResistancePercentPerVitality();
        physPercent = Math.min(physPercent, cap);
        stats.addDerivedModifier(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT, physPercent, ModifierType.FLAT);

        // Projectile resistance from DEXTERITY
        double dexterity = stats.getValue(RPGAttribute.DEXTERITY);
        double projFlat = dexterity * defenseDerivation.projectileResistanceFlatPerDexterity();
        stats.addDerivedModifier(RPGAttribute.PROJECTILE_RESISTANCE, projFlat, ModifierType.FLAT);

        double projPercent = dexterity * defenseDerivation.projectileResistancePercentPerDexterity();
        projPercent = Math.min(projPercent, cap);
        stats.addDerivedModifier(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT, projPercent, ModifierType.FLAT);

        // Fire resistance from RESOLVE
        double resolve = stats.getValue(RPGAttribute.RESOLVE);
        double fireFlat = resolve * defenseDerivation.fireResistanceFlatPerResolve();
        stats.addDerivedModifier(RPGAttribute.FIRE_RESISTANCE, fireFlat, ModifierType.FLAT);

        double firePercent = resolve * defenseDerivation.fireResistancePercentPerResolve();
        firePercent = Math.min(firePercent, cap);
        stats.addDerivedModifier(RPGAttribute.FIRE_RESISTANCE_PERCENT, firePercent, ModifierType.FLAT);
    }
}
