package net.herotale.herocore.impl.system;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.impl.HeroCoreComponentRegistry;
import net.herotale.herocore.impl.HeroCoreModifiers;
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
public class AttributeDerivationSystem extends EntityTickingSystem<EntityStore> {

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
     * 2. Applies the derivation formulas
     * 3. Writes the results as flat or percent modifiers to EntityStatMap
     * 
     * Examples:
     * - Vitality + Resolve → Physical Resistance
     * - Intelligence + Faith → Spell Power
     * - Dexterity → Attack Speed, Movement Speed
     * - etc. (per game balance)
     */
    private void applyDerivedAttributeModifiers(EntityStatMap entityStatMap,
                                                  HeroCoreStatsComponent primaryStats) {
        // Use AttributeDerivationFormulas to compute all secondary attributes
        // from primary attributes
        var derived = AttributeDerivationFormulas.computeDerived(primaryStats);

        // Apply each derived attribute as a modifier to EntityStatMap
        // The modifier key ensures we can update/remove each one independently
        
        if (derived.containsKey(RPGAttribute.PHYSICAL_RESISTANCE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_PHYSICAL_RESISTANCE,
                    HeroCoreStatTypes.getPhysicalResistance(),
                    derived.get(RPGAttribute.PHYSICAL_RESISTANCE), false);
        }

        if (derived.containsKey(RPGAttribute.SPELL_POWER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_SPELL_POWER,
                    HeroCoreStatTypes.getSpellPower(),
                    derived.get(RPGAttribute.SPELL_POWER), false);
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

        if (derived.containsKey(RPGAttribute.ATTACK_DAMAGE)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ATTACK_DAMAGE,
                    HeroCoreStatTypes.getAttackDamage(),
                    derived.get(RPGAttribute.ATTACK_DAMAGE), false);
        }

        if (derived.containsKey(RPGAttribute.MOVE_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MOVE_SPEED,
                    HeroCoreStatTypes.getMoveSpeed(),
                    derived.get(RPGAttribute.MOVE_SPEED), false);
        }

        if (derived.containsKey(RPGAttribute.ATTACK_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_ATTACK_SPEED,
                    HeroCoreStatTypes.getAttackSpeed(),
                    derived.get(RPGAttribute.ATTACK_SPEED), false);
        }

        if (derived.containsKey(RPGAttribute.MAGIC_RESIST)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MAGIC_RESIST,
                    HeroCoreStatTypes.getMagicResist(),
                    derived.get(RPGAttribute.MAGIC_RESIST), false);
        }

        if (derived.containsKey(RPGAttribute.HEALING_POWER)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_HEALING_POWER,
                    HeroCoreStatTypes.getHealingPower(),
                    derived.get(RPGAttribute.HEALING_POWER), false);
        }

        if (derived.containsKey(RPGAttribute.MINING_SPEED)) {
            addModifier(entityStatMap, HeroCoreModifiers.DERIVED_MINING_SPEED,
                    HeroCoreStatTypes.getMiningSpeed(),
                    derived.get(RPGAttribute.MINING_SPEED), false);
        }
    }

    /**
     * Helper: adds or updates a StaticModifier in EntityStatMap.
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
            entityStatMap.removeModifier(statTypeIndex, modifierKey);
            return;
        }

        StaticModifier modifier = createStaticModifier((float) value, isPercent);
        entityStatMap.putModifier(statTypeIndex, modifierKey, modifier);
    }

    /**
     * Creates a StaticModifier with the given value and type.
     * 
     * @param value the numeric value of the modifier
     * @param isPercent true if this is a percentage/multiplicative modifier, false for additive
     * @return a new StaticModifier configured for EntityStatMap
     */
    private StaticModifier createStaticModifier(float value, boolean isPercent) {
        // All derived modifiers use ADDITIVE stacking — even speed modifiers.
        // Speed stats have a base of 1.0; the derived formula outputs the bonus
        // (e.g. 0.2 for +20% speed). ADDITIVE adds to the base directly.
        // MULTIPLICATIVE would compound with other modifiers incorrectly.
        return new StaticModifier(Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.ADDITIVE, value);
    }
}
