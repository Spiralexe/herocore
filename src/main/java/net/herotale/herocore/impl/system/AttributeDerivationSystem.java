package net.herotale.herocore.impl.system;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.logging.Level;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.impl.HeroCoreComponentRegistry;
import net.herotale.herocore.impl.HeroCoreModifiers;
import net.herotale.herocore.impl.HeroCorePlugin;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.attribute.AttributeDerivationFormulas;

/**
 * Tick system that reads primary attributes from {@link HeroCoreStatsComponent},
 * computes derived secondary attributes, and writes them directly into the entity's
 * {@link EntityStatMap} as {@link com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier} entries.
 * 
 * <b>Core responsibility:</b> Keep EntityStatMap in sync with derived attribute values.
 * 
 * <b>Execution order:</b> Runs BEFORE EntityStatsSystems.Recalculate (configured via SystemGroup ordering).
 * This ensures that:
 * 1. HeroCore derives secondary values and adds them to EntityStatMap
 * 2. Hytale's built-in stat recalculation processes all modifiers
 * 3. Final computed values are ready for gameplay systems to read
 * 
 * <b>No bridge pattern:</b> EntityStatMap is the single source of truth for all stat values.
 * Gameplay systems always read from EntityStatMap, never from StatsComponent.
 * 
 * <b>Architecture:</b>
 * <pre>
 *  ┌─────────────────────────────────────────┐
 *  │  HeroCoreStatsComponent (per entity)    │
 *  │  - Strength, Dexterity, etc. (primary) │─ Base attribute values
 *  └──────────────┬──────────────────────────┘  (from leveling, gear, buffs)
 *                 │
 *                 │ AttributeDerivationSystem.update()
 *                 │ - Read primary attributes
 *                 │ - Compute derived values using formulas
 *                 │ - Apply modifiers
 *                 │
 *  ┌──────────────▼──────────────────────────┐
 *  │     EntityStatMap (per entity)          │
 *  │  - Static Modifiers for all stats       │─ Single source of truth
 *  │  - Health, Mana, Stamina, CritChance... │  for all stat values
 *  │  - Physical Defense, Spell Power, etc.  │
 *  └──────────────┬──────────────────────────┘
 *                 │
 *                 │ EntityStatsSystems.Recalculate (native Hytale)
 *                 │ - Computes final values from all modifiers
 *                 │ - Enforces min/max bounds
 *                 │
 *  ┌──────────────▼──────────────────────────┐
 *  │  Final computed values                  │
 *  │  (ready for gameplay systems to read)   │
 *  └─────────────────────────────────────────┘
 * </pre>
 * 
 * @see HeroCoreStatsComponent
 * @see AttributeDerivationFormulas
 * @see EntityStatMap
 */
public class AttributeDerivationSystem extends EntityTickingSystem<EntityStore>
        implements EntityStatsSystems.StatModifyingSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Modifier keys from HeroCoreModifiers — HC_ prefix for global uniqueness

    public AttributeDerivationSystem() {
        // No fields needed — derivation is pure math in AttributeDerivationFormulas
    }

    /**
     * Query filter: only tick entities that have HeroCoreStatsComponent.
     * ComponentType implements Query directly (no Query.has() needed).
     */
    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreComponentRegistry.HERO_CORE_STATS;
    }

    /**
     * Called once per world tick for each entity matched by getQuery().
     * Reads primary attributes from HeroCoreStatsComponent and writes
     * derived values as StaticModifier entries into EntityStatMap.
     */
    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Get HeroCore primary attributes from the chunk
        HeroCoreStatsComponent primaryStats = chunk.getComponent(index,
                HeroCoreComponentRegistry.HERO_CORE_STATS);
        if (primaryStats == null) {
            return; // Entity doesn't have HeroCore stats
        }

        // Get the entity's native stat map via Ref lookup
        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        EntityStatMap entityStatMap = store.getComponent(entityRef,
                EntityStatsModule.get().getEntityStatMapComponentType());
        if (entityStatMap == null) {
            return; // Entity has HeroCore stats but no EntityStatMap
        }

        // Compute and apply all derived attributes
        applyDerivedAttributeModifiers(entityStatMap, primaryStats);
    }

    /**
     * Computes derived attribute values from primary stats and writes them
     * as StaticModifier entries into the EntityStatMap.
     * 
     * This is the core derivation logic. It:
     * 1. Takes primary attribute values
     * 2. Applies the derivation formulas (from config)
     * 3. Writes the results as flat or percent modifiers to EntityStatMap
     * 
     * Examples:
     * - Vitality + Resolve → Physical Resistance, Armor, Health
     * - Intelligence + Faith → Spell Power, Mana
     * - Dexterity → Attack Speed, Movement Speed, Dodge
     * - etc. (per game balance in CoreConfig)
     */
    private void applyDerivedAttributeModifiers(EntityStatMap entityStatMap,
                                                  HeroCoreStatsComponent primaryStats) {
        // Use AttributeDerivationFormulas to compute all secondary attributes
        // from primary attributes, passing in the config for formulas
        var derived = AttributeDerivationFormulas.computeDerived(primaryStats, 
                                                                 HeroCorePlugin.get().getConfig());

        // Apply each derived attribute as a modifier to EntityStatMap
        // The modifier key ensures we can update/remove each one independently
        
        // ── Combat — Physical ──────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.ATTACK_DAMAGE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ATTACK_DAMAGE,
                    HeroCoreStatTypes.getAttackDamage(),
                    derived.get(RPGAttribute.ATTACK_DAMAGE), false);
        }

        if (derived.containsKey(RPGAttribute.CRIT_CHANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_CRIT_CHANCE,
                    HeroCoreStatTypes.getCritChance(),
                    derived.get(RPGAttribute.CRIT_CHANCE), false);
        }

        if (derived.containsKey(RPGAttribute.CRIT_DAMAGE_MULTIPLIER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_CRIT_DAMAGE_MULTIPLIER,
                    HeroCoreStatTypes.getCritDamageMultiplier(),
                    derived.get(RPGAttribute.CRIT_DAMAGE_MULTIPLIER), false);
        }

        if (derived.containsKey(RPGAttribute.ATTACK_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ATTACK_SPEED,
                    HeroCoreStatTypes.getAttackSpeed(),
                    derived.get(RPGAttribute.ATTACK_SPEED), false);
        }

        if (derived.containsKey(RPGAttribute.BLOCK_STRENGTH)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_BLOCK_STRENGTH,
                    HeroCoreStatTypes.getBlockStrength(),
                    derived.get(RPGAttribute.BLOCK_STRENGTH), false);
        }

        if (derived.containsKey(RPGAttribute.DODGE_RATING)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_DODGE_RATING,
                    HeroCoreStatTypes.getDodgeRating(),
                    derived.get(RPGAttribute.DODGE_RATING), false);
        }

        // ── Defense — Armor & Resistances ──────────────────────────────
        
        if (derived.containsKey(RPGAttribute.ARMOR)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ARMOR,
                    HeroCoreStatTypes.getArmor(),
                    derived.get(RPGAttribute.ARMOR), false);
        }

        if (derived.containsKey(RPGAttribute.PHYSICAL_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_PHYSICAL_RESISTANCE,
                    HeroCoreStatTypes.getPhysicalResistance(),
                    derived.get(RPGAttribute.PHYSICAL_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_PHYSICAL_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getPhysicalResistancePercent(),
                    derived.get(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.PROJECTILE_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_PROJECTILE_RESISTANCE,
                    HeroCoreStatTypes.getProjectileResistance(),
                    derived.get(RPGAttribute.PROJECTILE_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_PROJECTILE_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getProjectileResistancePercent(),
                    derived.get(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.FIRE_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_FIRE_RESISTANCE,
                    HeroCoreStatTypes.getFireResistance(),
                    derived.get(RPGAttribute.FIRE_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.FIRE_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_FIRE_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getFireResistancePercent(),
                    derived.get(RPGAttribute.FIRE_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.ICE_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ICE_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getIceResistancePercent(),
                    derived.get(RPGAttribute.ICE_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.LIGHTNING_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_LIGHTNING_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getLightningResistancePercent(),
                    derived.get(RPGAttribute.LIGHTNING_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.POISON_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_POISON_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getPoisonResistancePercent(),
                    derived.get(RPGAttribute.POISON_RESISTANCE_PERCENT), false);
        }

        if (derived.containsKey(RPGAttribute.ARCANE_RESISTANCE_PERCENT)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ARCANE_RESISTANCE_PERCENT,
                    HeroCoreStatTypes.getArcaneResistancePercent(),
                    derived.get(RPGAttribute.ARCANE_RESISTANCE_PERCENT), false);
        }

        // ── Combat — Magical ────────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.SPELL_POWER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_SPELL_POWER,
                    HeroCoreStatTypes.getSpellPower(),
                    derived.get(RPGAttribute.SPELL_POWER), false);
        }

        if (derived.containsKey(RPGAttribute.SPELL_CRIT_CHANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_SPELL_CRIT_CHANCE,
                    HeroCoreStatTypes.getSpellCritChance(),
                    derived.get(RPGAttribute.SPELL_CRIT_CHANCE), false);
        }

        if (derived.containsKey(RPGAttribute.SPELL_CRIT_MULTIPLIER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_SPELL_CRIT_MULTIPLIER,
                    HeroCoreStatTypes.getSpellCritMultiplier(),
                    derived.get(RPGAttribute.SPELL_CRIT_MULTIPLIER), false);
        }

        if (derived.containsKey(RPGAttribute.MAGIC_RESIST)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MAGIC_RESIST,
                    HeroCoreStatTypes.getMagicResist(),
                    derived.get(RPGAttribute.MAGIC_RESIST), false);
        }

        // ── Combat — Healing ────────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.HEALING_POWER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_HEALING_POWER,
                    HeroCoreStatTypes.getHealingPower(),
                    derived.get(RPGAttribute.HEALING_POWER), false);
        }

        if (derived.containsKey(RPGAttribute.HEAL_CRIT_CHANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_HEAL_CRIT_CHANCE,
                    HeroCoreStatTypes.getHealCritChance(),
                    derived.get(RPGAttribute.HEAL_CRIT_CHANCE), false);
        }

        if (derived.containsKey(RPGAttribute.HEAL_CRIT_MULTIPLIER)) {
            // Note: heal crit multiplier is a constant in config, not derived per-entity
            // But we still write it to the stat map for consistency
            addModifier(entityStatMap, "HC_derived_heal_crit_multiplier",
                    HeroCoreStatTypes.getHealCritMultiplier(),
                    derived.get(RPGAttribute.HEAL_CRIT_MULTIPLIER), false);
        }

        if (derived.containsKey(RPGAttribute.SHIELD_STRENGTH)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_SHIELD_STRENGTH,
                    HeroCoreStatTypes.getShieldStrength(),
                    derived.get(RPGAttribute.SHIELD_STRENGTH), false);
        }

        if (derived.containsKey(RPGAttribute.BUFF_STRENGTH)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_BUFF_STRENGTH,
                    HeroCoreStatTypes.getBuffStrength(),
                    derived.get(RPGAttribute.BUFF_STRENGTH), false);
        }

        // ── Resources ───────────────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.MAX_HEALTH)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MAX_HEALTH,
                    HeroCoreStatTypes.getMaxHealth(),
                    derived.get(RPGAttribute.MAX_HEALTH), false);
        }

        if (derived.containsKey(RPGAttribute.HEALTH_REGEN)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_HEALTH_REGEN,
                    HeroCoreStatTypes.getHealthRegen(),
                    derived.get(RPGAttribute.HEALTH_REGEN), false);
        }

        if (derived.containsKey(RPGAttribute.MAX_MANA)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MAX_MANA,
                    HeroCoreStatTypes.getMaxMana(),
                    derived.get(RPGAttribute.MAX_MANA), false);
        }

        if (derived.containsKey(RPGAttribute.MANA_REGEN)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MANA_REGEN,
                    HeroCoreStatTypes.getManaRegen(),
                    derived.get(RPGAttribute.MANA_REGEN), false);
        }

        if (derived.containsKey(RPGAttribute.STAMINA_REGEN)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_STAMINA_REGEN,
                    HeroCoreStatTypes.getStaminaRegen(),
                    derived.get(RPGAttribute.STAMINA_REGEN), false);
        }

        // ── Mobility / World ────────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.MOVE_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MOVE_SPEED,
                    HeroCoreStatTypes.getMoveSpeed(),
                    derived.get(RPGAttribute.MOVE_SPEED), false);
        }

        if (derived.containsKey(RPGAttribute.MINING_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MINING_SPEED,
                    HeroCoreStatTypes.getMiningSpeed(),
                    derived.get(RPGAttribute.MINING_SPEED), false);
        }

        if (derived.containsKey(RPGAttribute.FALL_DAMAGE_REDUCTION)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_FALL_DAMAGE_REDUCTION,
                    HeroCoreStatTypes.getFallDamageReduction(),
                    derived.get(RPGAttribute.FALL_DAMAGE_REDUCTION), false);
        }

        // ── Control / Threat ────────────────────────────────────────────
        
        if (derived.containsKey(RPGAttribute.CC_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_CC_RESISTANCE,
                    HeroCoreStatTypes.getCCResistance(),
                    derived.get(RPGAttribute.CC_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.DEBUFF_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_DEBUFF_RESISTANCE,
                    HeroCoreStatTypes.getDebuffResistance(),
                    derived.get(RPGAttribute.DEBUFF_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.THREAT_GENERATION)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_THREAT_GENERATION,
                    HeroCoreStatTypes.getThreatGeneration(),
                    derived.get(RPGAttribute.THREAT_GENERATION), false);
        }
    }

    /**
     * Helper: sets a derived stat value directly in EntityStatMap.
     * 
     * @param entityStatMap the entity's stat map
     * @param modifierName  unique name (e.g., "physical_defense_flat") for removal/update
     * @param statTypeIndex the stat type index (from HeroCoreStatTypes)
     * @param value         the value to add/modify
     * @param isPercent     true if this is a percentage modifier (multiplicative stacking)
     */
    private void addModifier(EntityStatMap entityStatMap, String modifierKey,
                             int statTypeIndex, double value, boolean isPercent) {
        if (statTypeIndex == Integer.MIN_VALUE) {
            LOGGER.at(Level.WARNING).log("Stat type index not resolved for %s", modifierKey);
            return;
        }

        if (Math.abs(value) < 1e-10) {
            entityStatMap.resetStatValue(EntityStatMap.Predictable.SELF, statTypeIndex);
            return;
        }
        entityStatMap.setStatValue(EntityStatMap.Predictable.SELF, statTypeIndex, (float) value);
    }
}
