package net.herotale.herocore.impl.config;

import java.util.Map;

/**
 * Immutable configuration loaded from hero-core-defaults.json.
 */
public record CoreConfig(
        ModifierStackingConfig modifierStacking,
        DamageConfig damage,
        HealConfig heal,
        ResourceRegenConfig resourceRegen,
        LevelingConfig leveling,
        AttributeDerivationConfig attributeDerivation,
        Map<String, Double> attributeDefaults,
        Map<String, Boolean> systemOverrides
) {
    public record ModifierStackingConfig(
            double maxPercentAdditive,
            double multiplicativeCap,
            String overridePriority
    ) {}

    public record DamageConfig(
            double critDamageBaseMultiplier,
            double minimumDamage,
            double maxResistanceReduction,
            Map<String, String> resistanceMapping
    ) {}

    public record HealConfig(
            double healCritBaseMultiplier,
            boolean healingPowerScalesRegenTick,
            boolean healingPowerScalesPassive
    ) {}

    public record ResourceRegenConfig(
            long tickIntervalMs,
            double outOfCombatBonusMultiplier,
            long combatTimeoutMs
    ) {}

    public record LevelingConfig(
            int defaultMaxLevel,
            Map<String, Double> sourceWeights
    ) {}

    public record AttributeDerivationConfig(
            VitalityDerivation vitality,
            StrengthDerivation strength,
            DexterityDerivation dexterity,
            IntelligenceDerivation intelligence,
            FaithDerivation faith,
            ResolveDerivation resolve
    ) {}

    public record VitalityDerivation(
            double healthPerPoint,
            double healthBase,
            double healthRegenPerPoint,
            double healthRegenBase,
            double armorPercentPerPoint,
            double fallDamageReductionPerPoint,
            double fallDamageReductionCap
    ) {}

    public record StrengthDerivation(
            double baseAttackDamagePerPoint,
            double blockStrengthPerPoint
    ) {}

    public record DexterityDerivation(
            double critChancePerPoint,
            double attackSpeedPercentPerPoint,
            double dodgeRatingPerPoint
    ) {}

    public record IntelligenceDerivation(
            double spellPowerPercentPerPoint,
            double manaPerPoint,
            double manaBase,
            double spellCritChancePerPoint,
            double spellCritMultiplierPerPoint
    ) {}

    public record FaithDerivation(
            double healingPowerPercentPerPoint,
            double manaPerPoint,
            double manaBase,
            double manaRegenPerPoint,
            double manaRegenBase,
            double healCritChancePerPoint,
            double shieldStrengthPerPoint,
            double buffStrengthPercentPerPoint
    ) {}

    public record ResolveDerivation(
            double ccResistancePerPoint,
            double ccResistanceCap,
            double debuffResistancePerPoint,
            double debuffResistanceCap,
            double threatGenerationPercentPerPoint,
            double staminaRegenPerPoint,
            double magicResistPercentPerPoint
    ) {}
}
