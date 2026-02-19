package net.herotale.herocore.impl.attribute;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.impl.config.CoreConfig;

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
     * Computes all derived secondary attributes from the given primary attribute values
     * using the formulas defined in CoreConfig.
     * 
     * Returns a map of {@code RPGAttribute → computed value} for each derived attribute.
     * Only includes attributes that have non-zero derived values.
     * 
     * @param primaryStats the entity's HeroCoreStatsComponent with primary values
     * @param config the CoreConfig containing all derivation formulas
     * @return map of derived attributes, or empty if none
     */
    public static Map<RPGAttribute, Double> computeDerived(
            HeroCoreStatsComponent primaryStats, CoreConfig config) {

        Map<RPGAttribute, Double> derived = new HashMap<>();

        double str = primaryStats.getStrength();
        double dex = primaryStats.getDexterity();
        double intel = primaryStats.getIntelligence();
        double faith = primaryStats.getFaith();
        double vit = primaryStats.getVitality();
        double res = primaryStats.getResolve();

        var attrDerivation = config.attributeDerivation();
        var defDerivation = config.defenseDerivation();

        // ── VITALITY DERIVATIONS ──────────────────────────────────────
        
        // Max Health = base + (VIT * perPoint)
        double maxHealth = attrDerivation.vitality().healthBase() + 
                          (vit * attrDerivation.vitality().healthPerPoint()) +
                          (res * attrDerivation.resolve().healthPerPoint());
        if (maxHealth > 0.0) {
            derived.put(RPGAttribute.MAX_HEALTH, maxHealth);
        }

        // Health Regen = base + (VIT * perPoint)
        double healthRegen = attrDerivation.vitality().healthRegenBase() +
                            (vit * attrDerivation.vitality().healthRegenPerPoint());
        if (healthRegen > 0.0) {
            derived.put(RPGAttribute.HEALTH_REGEN, healthRegen);
        }

        // Armor (flat) = VIT * perPoint
        double armor = vit * defDerivation.physicalResistanceFlatPerVitality();
        if (armor > 0.0) {
            derived.put(RPGAttribute.ARMOR, armor);
        }

        // Physical Resistance Percent = VIT * perPoint (capped by config)
        double physResPercent = Math.min(
            vit * defDerivation.physicalResistancePercentPerVitality(),
            defDerivation.maxResistancePercent()
        );
        if (physResPercent > 0.0) {
            derived.put(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT, physResPercent);
        }

        // ── STRENGTH DERIVATIONS ──────────────────────────────────────
        
        // Base Attack Damage = STR * perPoint
        double attackDamage = str * attrDerivation.strength().baseAttackDamagePerPoint();
        if (attackDamage > 0.0) {
            derived.put(RPGAttribute.ATTACK_DAMAGE, attackDamage);
        }

        // Block Strength = STR * perPoint
        double blockStrength = str * attrDerivation.strength().blockStrengthPerPoint();
        if (blockStrength > 0.0) {
            derived.put(RPGAttribute.BLOCK_STRENGTH, blockStrength);
        }

        // Shield Strength = STR * perPoint
        double shieldStrength = str * attrDerivation.strength().shieldStrengthPerPoint();
        if (shieldStrength > 0.0) {
            derived.put(RPGAttribute.SHIELD_STRENGTH, shieldStrength);
        }

        // Mining Speed bonus = STR * perPoint (base speed comes from stat default)
        double miningSpeedBonus = str / 500.0; // TODO: make configurable
        if (miningSpeedBonus > 0.0) {
            derived.put(RPGAttribute.MINING_SPEED, miningSpeedBonus);
        }

        // ── DEXTERITY DERIVATIONS ─────────────────────────────────────
        
        // Crit Chance = DEX * perPoint (0.0-1.0, capped at 100%)
        double critChance = Math.min(1.0, dex * attrDerivation.dexterity().critChancePerPoint());
        if (critChance > 0.0) {
            derived.put(RPGAttribute.CRIT_CHANCE, critChance);
        }

        // Attack Speed bonus = DEX * perPoint (base speed comes from stat default)
        double attackSpeedBonus = dex * attrDerivation.dexterity().attackSpeedPercentPerPoint();
        if (attackSpeedBonus > 0.0) {
            derived.put(RPGAttribute.ATTACK_SPEED, attackSpeedBonus);
        }

        // Dodge Rating = DEX * perPoint
        double dodgeRating = dex * attrDerivation.dexterity().dodgeRatingPerPoint();
        if (dodgeRating > 0.0) {
            derived.put(RPGAttribute.DODGE_RATING, dodgeRating);
        }

        // Fall Damage Reduction = DEX * perPoint (capped)
        double fallDmgReduction = Math.min(
            dex * attrDerivation.dexterity().fallDamageReductionPerPoint(),
            attrDerivation.dexterity().fallDamageReductionCap()
        );
        if (fallDmgReduction > 0.0) {
            derived.put(RPGAttribute.FALL_DAMAGE_REDUCTION, fallDmgReduction);
        }

        // Move Speed bonus = DEX * perPoint (base speed comes from stat default)
        double moveSpeedBonus = dex / 500.0; // TODO: make configurable
        if (moveSpeedBonus > 0.0) {
            derived.put(RPGAttribute.MOVE_SPEED, moveSpeedBonus);
        }

        // Projectile Resistance (flat) = DEX * perPoint
        double projResFlat = dex * defDerivation.projectileResistanceFlatPerDexterity();
        if (projResFlat > 0.0) {
            derived.put(RPGAttribute.PROJECTILE_RESISTANCE, projResFlat);
        }

        // Projectile Resistance Percent = DEX * perPoint (capped)
        double projResPercent = Math.min(
            dex * defDerivation.projectileResistancePercentPerDexterity(),
            defDerivation.maxResistancePercent()
        );
        if (projResPercent > 0.0) {
            derived.put(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT, projResPercent);
        }

        // ── INTELLIGENCE DERIVATIONS ──────────────────────────────────
        
        // Spell Power = base + (INT * perPoint)
        double spellPower = intel * attrDerivation.intelligence().spellPowerPercentPerPoint();
        if (spellPower > 0.0) {
            derived.put(RPGAttribute.SPELL_POWER, spellPower);
        }

        // Max Mana = base + (INT * perPoint) + (FAITH * perPoint)
        double maxMana = attrDerivation.intelligence().manaBase() +
                        (intel * attrDerivation.intelligence().manaPerPoint()) +
                        (faith * attrDerivation.faith().manaPerPoint());
        if (maxMana > 0.0) {
            derived.put(RPGAttribute.MAX_MANA, maxMana);
        }

        // Spell Crit Chance = INT * perPoint
        double spellCritChance = intel * attrDerivation.intelligence().spellCritChancePerPoint();
        if (spellCritChance > 0.0) {
            derived.put(RPGAttribute.SPELL_CRIT_CHANCE, spellCritChance);
        }

        // Spell Crit Multiplier bonus = INT * perPoint (base multiplier comes from stat default)
        double spellCritMultBonus = intel * attrDerivation.intelligence().spellCritMultiplierPerPoint();
        if (spellCritMultBonus > 0.0) {
            derived.put(RPGAttribute.SPELL_CRIT_MULTIPLIER, spellCritMultBonus);
        }

        // Magic Resist = INT * perPoint
        double magicResist = intel / 10.0; // TODO: make configurable
        if (magicResist > 0.0) {
            derived.put(RPGAttribute.MAGIC_RESIST, magicResist);
        }

        // Elemental Resist (Ice/Lightning/Arcane) from INT
        double iceResPercent = Math.min(
            intel * defDerivation.iceResistancePercentPerIntelligence(),
            defDerivation.maxResistancePercent()
        );
        if (iceResPercent > 0.0) {
            derived.put(RPGAttribute.ICE_RESISTANCE_PERCENT, iceResPercent);
        }

        double lightningResPercent = Math.min(
            intel * defDerivation.lightningResistancePercentPerIntelligence(),
            defDerivation.maxResistancePercent()
        );
        if (lightningResPercent > 0.0) {
            derived.put(RPGAttribute.LIGHTNING_RESISTANCE_PERCENT, lightningResPercent);
        }

        double arcaneResPercent = Math.min(
            intel * defDerivation.arcaneResistancePercentPerIntelligence(),
            defDerivation.maxResistancePercent()
        );
        if (arcaneResPercent > 0.0) {
            derived.put(RPGAttribute.ARCANE_RESISTANCE_PERCENT, arcaneResPercent);
        }

        // ── FAITH DERIVATIONS ─────────────────────────────────────────
        
        // Healing Power = FAITH * perPoint
        double healingPower = faith * attrDerivation.faith().healingPowerPercentPerPoint();
        if (healingPower > 0.0) {
            derived.put(RPGAttribute.HEALING_POWER, healingPower);
        }

        // Mana Regen = base + (FAITH * perPoint)
        double manaRegen = attrDerivation.faith().manaRegenBase() +
                          (faith * attrDerivation.faith().manaRegenPerPoint());
        if (manaRegen > 0.0) {
            derived.put(RPGAttribute.MANA_REGEN, manaRegen);
        }

        // Heal Crit Chance = FAITH * perPoint
        double healCritChance = faith * attrDerivation.faith().healCritChancePerPoint();
        if (healCritChance > 0.0) {
            derived.put(RPGAttribute.HEAL_CRIT_CHANCE, healCritChance);
        }

        // Heal Crit Multiplier = base 1.5 (same as config healCritBaseMultiplier)
        // No per-point scaling for heal crit multi in current config  
        // Only write if derived from primaries (TODO: make faith add to this?)
        // For now, don't write baseline value to map (like other multipliers)

        // Buff Strength = FAITH * perPoint
        double buffStrength = faith * attrDerivation.faith().buffStrengthPercentPerPoint();
        if (buffStrength > 0.0) {
            derived.put(RPGAttribute.BUFF_STRENGTH, buffStrength);
        }

        // ── RESOLVE DERIVATIONS ───────────────────────────────────────
        
        // CC Resistance = RES * perPoint (capped)
        double ccResist = Math.min(
            res * attrDerivation.resolve().ccResistancePerPoint(),
            attrDerivation.resolve().ccResistanceCap()
        );
        if (ccResist > 0.0) {
            derived.put(RPGAttribute.CC_RESISTANCE, ccResist);
        }

        // Debuff Resistance = RES * perPoint (capped)
        double debuffResist = Math.min(
            res * attrDerivation.resolve().debuffResistancePerPoint(),
            attrDerivation.resolve().debuffResistanceCap()
        );
        if (debuffResist > 0.0) {
            derived.put(RPGAttribute.DEBUFF_RESISTANCE, debuffResist);
        }

        // Threat Generation = 1.0 + (RES * perPoint)
        double threatGenBonus = res * attrDerivation.resolve().threatGenerationPercentPerPoint();
        if (threatGenBonus > 0.0) {
            derived.put(RPGAttribute.THREAT_GENERATION, 1.0 + threatGenBonus);
        }

        // Stamina Regen = RES * perPoint
        double staminaRegen = res * attrDerivation.resolve().staminaRegenPerPoint();
        if (staminaRegen > 0.0) {
            derived.put(RPGAttribute.STAMINA_REGEN, staminaRegen);
        }

        // Magic Resist Percent = RES * perPoint
        double magicResistPercent = res * attrDerivation.resolve().magicResistPercentPerPoint();
        if (magicResistPercent > 0.0) {
            // Add to magic resist as percent
            double baseMagicResist = derived.getOrDefault(RPGAttribute.MAGIC_RESIST, 0.0);
            derived.put(RPGAttribute.MAGIC_RESIST, baseMagicResist + magicResistPercent);
        }

        // Fire Resistance (flat) = RES * perPoint
        double fireResFlat = res * defDerivation.fireResistanceFlatPerResolve();
        if (fireResFlat > 0.0) {
            derived.put(RPGAttribute.FIRE_RESISTANCE, fireResFlat);
        }

        // Fire Resistance Percent = RES * perPoint (capped)
        double fireResPercent = Math.min(
            res * defDerivation.fireResistancePercentPerResolve(),
            defDerivation.maxResistancePercent()
        );
        if (fireResPercent > 0.0) {
            derived.put(RPGAttribute.FIRE_RESISTANCE_PERCENT, fireResPercent);
        }

        // Poison Resistance Percent = RES * perPoint (capped)
        double poisonResPercent = Math.min(
            res * defDerivation.poisonResistancePercentPerResolve(),
            defDerivation.maxResistancePercent()
        );
        if (poisonResPercent > 0.0) {
            derived.put(RPGAttribute.POISON_RESISTANCE_PERCENT, poisonResPercent);
        }

        // ── CRIT DAMAGE MULTIPLIER (combo of STR + INT) ───────────────
        
        // Crit Damage Multiplier = base + derived
        double critDmgBonus = (str + intel) / 500.0; // TODO: make configurable
        if (critDmgBonus > 0.0) {
            derived.put(RPGAttribute.CRIT_DAMAGE_MULTIPLIER, 1.5 + critDmgBonus);
        }

        return derived;
    }

    private AttributeDerivationFormulas() {}
}
