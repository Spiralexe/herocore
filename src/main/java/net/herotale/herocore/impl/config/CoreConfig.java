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
        DefenseDerivationConfig defenseDerivation,
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
            double armorPercentPerPoint
    ) {}

    public record StrengthDerivation(
            double baseAttackDamagePerPoint,
            double blockStrengthPerPoint,
            double shieldStrengthPerPoint
    ) {}

    public record DexterityDerivation(
            double critChancePerPoint,
            double attackSpeedPercentPerPoint,
            double dodgeRatingPerPoint,
            double fallDamageReductionPerPoint,
            double fallDamageReductionCap
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
            double buffStrengthPercentPerPoint
    ) {}

    public record ResolveDerivation(
            double healthPerPoint,
            double ccResistancePerPoint,
            double ccResistanceCap,
            double debuffResistancePerPoint,
            double debuffResistanceCap,
            double threatGenerationPercentPerPoint,
            double staminaRegenPerPoint,
            double magicResistPercentPerPoint
    ) {}

    /**
     * Configuration for defense/resistance derivation from primary attributes.
     * <p>
     * These values feed into HeroCore's resistance attributes
     * ({@code PHYSICAL_RESISTANCE}, {@code PROJECTILE_RESISTANCE}, etc.)
     * which are written directly to {@code EntityStatMap} by
     * {@code AttributeDerivationSystem} as {@code StaticModifier} entries.
     * <p>
     * The {@code physicalResistancePerLevelPercent} field enables additive
     * defense scaling per player level (e.g. +1% physical resistance per level).
     */
    public record DefenseDerivationConfig(
            /** Flat physical resistance per point of VITALITY. */
            double physicalResistanceFlatPerVitality,
            /** Percent physical resistance per point of VITALITY (0.001 = 0.1% per point). */
            double physicalResistancePercentPerVitality,
            /** Percent physical resistance bonus per player level (0.01 = 1% per level). */
            double physicalResistancePerLevelPercent,
            /** Flat projectile resistance per point of DEXTERITY. */
            double projectileResistanceFlatPerDexterity,
            /** Percent projectile resistance per point of DEXTERITY. */
            double projectileResistancePercentPerDexterity,
            /** Flat fire resistance per point of RESOLVE. */
            double fireResistanceFlatPerResolve,
            /** Percent fire resistance per point of RESOLVE. */
            double fireResistancePercentPerResolve,
            /** Percent ice resistance per point of INTELLIGENCE. */
            double iceResistancePercentPerIntelligence,
            /** Percent lightning resistance per point of INTELLIGENCE. */
            double lightningResistancePercentPerIntelligence,
            /** Percent poison resistance per point of RESOLVE. */
            double poisonResistancePercentPerResolve,
            /** Percent arcane resistance per point of INTELLIGENCE. */
            double arcaneResistancePercentPerIntelligence,
            /** Maximum percent resistance cap (prevents 100% damage immunity). */
            double maxResistancePercent
    ) {}
}
