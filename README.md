# HeroCore

**RPG attribute, combat, and progression framework for Hytale.**

HeroCore is a schema and formula library that extends Hytale's native ECS (Entity Component System). It provides primary attributes, derived stat calculations, a multi-stage damage/heal pipeline, status effects, leveling, and more — all wired through Hytale's native `EntityStatMap`, `StaticModifier`, and ECS event systems. No bridge classes, no parallel stat engines, no custom event buses.

**Version:** 0.2.0  
**Server:** Hytale 2026.02.17+  
**Java:** 25  
**Author:** Kainzo  

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Plugin Lifecycle](#plugin-lifecycle)
3. [ECS Components](#ecs-components)
4. [Attribute System](#attribute-system)
5. [Stat Derivation](#stat-derivation)
6. [Damage Pipeline](#damage-pipeline)
7. [Heal Pipeline](#heal-pipeline)
8. [Combat State & Timeout](#combat-state--timeout)
9. [Status Effects](#status-effects)
10. [Leveling & Progression](#leveling--progression)
11. [Configuration](#configuration)
12. [Downstream Integration Guide](#downstream-integration-guide)
13. [API Reference](#api-reference)
14. [Building](#building)

---

## Architecture Overview

```
HeroCorePlugin (JavaPlugin)
│
├── Schema Layer ── definitions HeroCore owns
│   ├── RPGAttribute enum         62 attributes (primary + secondary)
│   ├── DamageType enum           PHYSICAL, MAGICAL, FIRE, ICE, etc.
│   ├── HealType enum             SPELL, POTION, REGEN_TICK, PASSIVE, ENVIRONMENTAL
│   ├── DamageFlag enum           CRIT, DOT, AOE, MELEE, RANGED, etc.
│   └── CoreConfig                all tuning knobs (damage, heal, derivation, regen)
│
├── Math Layer ── pure functions, unit-testable, zero side effects
│   ├── AttributeDerivationFormulas   primary → secondary bonus amounts
│   ├── DamageFormulas                resistance curves, crit rolls, lifesteal
│   └── HealFormulas                  healing power scaling, heal crit
│
├── ECS Components ── registered in setup(), stored on entities
│   ├── HeroCoreStatsComponent        primary bases (STR/DEX/INT/FAITH/VIT/RES)    PERSISTENT
│   ├── HeroCoreProgressionComponent  level, XP, xpToNextLevel                     PERSISTENT
│   ├── CombatStateComponent          in-combat flag + dt accumulator               transient
│   └── StatusEffectIndexComponent    active effect IDs + remainingSeconds           transient
│
├── ECS Systems ── all extend native Hytale system types
│   ├── HeroCoreSetupSystem           HolderSystem — ensures components on entity add
│   ├── AttributeDerivationSystem     EntityTickingSystem — writes StaticModifiers (BEFORE Recalculate)
│   ├── CombatTimeoutSystem           DelayedEntitySystem — exits combat after timeout
│   └── StatusEffectTickSystem        DelayedEntitySystem — expires timed effects
│
├── Damage Pipeline ── EntityEventSystem chain (SystemDependency ordering)
│   AttackDamageBonusSystem → FallDamageReductionSystem → ResistanceMitigationSystem
│   → CriticalHitSystem → LifestealSystem → MinimumDamageSystem → DamageApplicationSystem
│
├── Heal Pipeline ── EntityEventSystem chain
│   HealingPowerScalingSystem → HealingReceivedBonusSystem → HealCritSystem
│
├── Registries ── API facades for downstream plugins
│   ├── LevelingRegistry          XP granting, level queries, profile management
│   ├── MobRegistry               mob/NPC profile registration and lookup
│   └── ZoneModifierRegistry      zone-scoped modifiers (v0.3 planned)
│
└── Native Hytale (used, not replaced)
    ├── EntityStatMap           single source of truth for all stat values
    ├── StaticModifier          all modifier application
    ├── RegeneratingValue       regen (configured via EntityStatType JSON assets)
    ├── BuilderCodec            component persistence
    └── CommandBuffer.invoke()  ECS event dispatch
```

### Design Principles

1. **No parallel infrastructure.** HeroCore writes `StaticModifier` entries into `EntityStatMap`. There is no second stat container, no bridge syncing values between two systems.
2. **`Ref<EntityStore>` everywhere in runtime.** UUID is for persistence only. All methods, events, and systems pass live entity handles.
3. **All systems extend native Hytale types.** `EntityTickingSystem`, `DelayedEntitySystem`, `EntityEventSystem`, `HolderSystem` — no custom system abstraction.
4. **Pure math is separated from ECS wiring.** `DamageFormulas`, `HealFormulas`, `AttributeDerivationFormulas` are static pure functions. Unit tests don't need a server instance.
5. **All persistence uses `BuilderCodec`.** No Gson serialization on join/quit events. Components are serialized as part of the entity store's save cycle.

---

## Plugin Lifecycle

`HeroCorePlugin` extends `JavaPlugin` and follows the standard Hytale module pattern:

| Phase | What Happens |
|-------|-------------|
| `preLoad()` | Loads `CoreConfig` from `mods/herocore/config.json` (falls back to built-in defaults) |
| `setup()` | Registers all 4 components, 4 ECS event types, and all systems via `getEntityStoreRegistry()` |
| `start()` | Resolves custom stat type indices via `HeroCoreStatTypes.update()` and validates them |
| `shutdown()` | Logs clean shutdown |

### Manifest

```json
{
    "Group": "Herotale",
    "Name": "Herocore",
    "Version": "0.2.0",
    "Main": "net.herotale.herocore.impl.HeroCorePlugin",
    "IncludesAssetPack": true
}
```

---

## ECS Components

All components are registered via `HeroCoreComponentRegistry.registerComponents()` in `setup()`. Each component follows the standard pattern: `implements Component<EntityStore>`, copy constructor, `clone()`, static `ComponentType` handle.

### HeroCoreStatsComponent (persistent)

Holds the six primary RPG attribute base values. These are the inputs to the `AttributeDerivationSystem`.

| Field | Type | Description |
|-------|------|-------------|
| `strength` | `float` | Physical damage, mining speed, block strength |
| `dexterity` | `float` | Crit chance, attack speed, dodge, move speed |
| `intelligence` | `float` | Spell power, mana, spell crit, magic resist |
| `faith` | `float` | Healing power, mana, mana regen, heal crit, buff strength |
| `vitality` | `float` | Max health, health regen, armor, physical resistance |
| `resolve` | `float` | CC resistance, debuff resistance, stamina regen, threat |

**Codec keys:** `HC_Strength`, `HC_Dexterity`, `HC_Intelligence`, `HC_Faith`, `HC_Vitality`, `HC_Resolve`

### HeroCoreProgressionComponent (persistent)

Tracks player level and XP progression.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `level` | `int` | 1 | Current level |
| `currentXP` | `float` | 0 | Accumulated XP toward next level |
| `xpToNextLevel` | `float` | 100 | XP threshold for next level-up |

**Codec keys:** `HC_Level`, `HC_CurrentXP`, `HC_XPToNextLevel`

### CombatStateComponent (transient)

Tracks whether an entity is in combat using `dt`-based timing (no wall clock). Reset on `enterCombat()`, accumulated via `tickElapsed(dt)`.

| Method | Description |
|--------|-------------|
| `enterCombat()` | Sets in-combat, resets timer to 0 |
| `tickElapsed(float dt)` | Accumulates time since last damage (only while in combat) |
| `isInCombat()` | `true` while in combat |
| `getSecondsSinceLastDamage()` | Elapsed seconds since `enterCombat()` |

### StatusEffectIndexComponent (transient)

Thin index that `StatusEffectTickSystem` reads to expire effects. The actual stat modifications live in `EntityStatMap` as `StaticModifier` entries. When an effect expires, `StatusEffectTickSystem` uses the tracked `ModifierRef` list to remove all associated modifiers automatically.

```java
public static class EffectEntry {
    public final int stacks;
    public float remainingSeconds; // decremented by dt each tick

    // Modifier tracking for automatic cleanup on expiry
    public void trackModifier(int statIndex, String modifierKey);
    public List<ModifierRef> getModifierRefs();

    public record ModifierRef(int statIndex, String modifierKey) {}
}
```

| Method | Description |
|--------|-------------|
| `addEffect(id, stacks, durationSeconds)` | Register a new effect |
| `getEffect(id)` | Get an `EffectEntry` by ID (for calling `trackModifier()` after applying modifiers) |
| `removeEffect(id)` | Remove an effect by ID |
| `getActiveEffects()` | Map of all active effects |

---

## Attribute System

### RPGAttribute Enum

`RPGAttribute` is the authoritative taxonomy of all RPG attributes. It defines 62 attributes across categories:

| Category | Examples |
|----------|---------|
| **Primary** | `STRENGTH`, `DEXTERITY`, `INTELLIGENCE`, `FAITH`, `VITALITY`, `RESOLVE` |
| **Combat — Physical** | `MAX_HEALTH`, `ARMOR`, `ATTACK_DAMAGE`, `ATTACK_SPEED`, `CRIT_CHANCE`, `LIFESTEAL` |
| **Combat — Magical** | `SPELL_POWER`, `SPELL_CRIT_CHANCE`, `MAGIC_PENETRATION`, `MAGIC_RESIST` |
| **Elemental** | `ELEMENTAL_DAMAGE_FIRE`, `ELEMENTAL_RESIST_ICE`, `ICE_RESISTANCE_PERCENT`, etc. |
| **Healing** | `HEALING_POWER`, `HEALING_RECEIVED_BONUS`, `HEAL_CRIT_CHANCE` |
| **Resources** | `MAX_MANA`, `MAX_STAMINA`, `MANA_REGEN`, `HEALTH_REGEN` |
| **Mobility** | `MOVE_SPEED`, `JUMP_HEIGHT`, `MINING_SPEED` |
| **Progression** | `XP_GAIN_MULTIPLIER`, `RARE_DROP_CHANCE` |

### HeroCoreStatTypes

Resolves custom RPG stat type indices at runtime from `EntityStatType` JSON assets. Mirrors the pattern from `DefaultEntityStatTypes`.

```java
// After start(), these return valid int indices
HeroCoreStatTypes.getCritChance();       // herocore:crit_chance
HeroCoreStatTypes.getAttackDamage();     // herocore:attack_damage
HeroCoreStatTypes.getSpellPower();       // herocore:spell_power
HeroCoreStatTypes.getPhysicalResistance(); // herocore:physical_resistance
HeroCoreStatTypes.getMagicResist();      // herocore:magic_resist
HeroCoreStatTypes.getAttackSpeed();      // herocore:attack_speed
HeroCoreStatTypes.getMoveSpeed();        // herocore:move_speed
HeroCoreStatTypes.getMiningSpeed();      // herocore:mining_speed
HeroCoreStatTypes.getHealingPower();     // herocore:healing_power
HeroCoreStatTypes.getCritDamageMultiplier(); // herocore:crit_damage_multiplier

// Dynamic lookup by asset ID string
HeroCoreStatTypes.getIndex("herocore:lifesteal");

// Helper: read stat value from an entity ref
float value = HeroCoreStatTypes.getStatValue(entityRef, statIndex);
```

### HeroCoreModifiers

All modifier keys used by HeroCore are constants in `HeroCoreModifiers`. Zero inline strings in modifier calls.

```java
// Derived stats (written by AttributeDerivationSystem)
HeroCoreModifiers.DERIVED_MAX_HEALTH       // "HC_derived_max_health"
HeroCoreModifiers.DERIVED_CRIT_CHANCE      // "HC_derived_crit_chance"
HeroCoreModifiers.DERIVED_SPELL_POWER      // "HC_derived_spell_power"
HeroCoreModifiers.DERIVED_ATTACK_DAMAGE    // "HC_derived_attack_damage"
HeroCoreModifiers.DERIVED_MOVE_SPEED       // "HC_derived_move_speed"
HeroCoreModifiers.DERIVED_ARMOR            // "HC_derived_armor"
// ... 30+ constants total

// Dynamic key builders
HeroCoreModifiers.effect("burn", "health");  // "HC_effect_burn_health"
HeroCoreModifiers.gear("chest_iron", "armor"); // "HC_gear_chest_iron_armor"
HeroCoreModifiers.derived("custom_stat");    // "HC_derived_custom_stat"
```

---

## Stat Derivation

### How It Works

`AttributeDerivationSystem` is an `EntityTickingSystem` that runs every tick. It reads the six primary attributes from `HeroCoreStatsComponent` and writes derived stat bonuses as `StaticModifier` entries into the entity's `EntityStatMap`.

It implements `EntityStatsSystems.StatModifyingSystem` (marker interface) and declares `BEFORE EntityStatsSystems.Recalculate` via `SystemDependency`. This ensures:
1. HeroCore writes derived modifiers into `EntityStatMap`
2. Hytale's native `Recalculate` system processes all modifiers
3. Final computed values are ready for gameplay systems to read

**Downstream stat-modifying systems must also declare `BEFORE EntityStatsSystems.Recalculate`** to avoid race conditions:
```java
@Nonnull
@Override
public Set<Dependency<EntityStore>> getDependencies() {
    return Set.of(
        new SystemDependency<>(Order.BEFORE, EntityStatsSystems.Recalculate.class)
    );
}
```

```
HeroCoreStatsComponent (primary bases)
        │
        ▼
AttributeDerivationFormulas.computeDerived()  ← pure math
        │
        ▼
EntityStatMap.putModifier()  ← writes named modifiers
        │
        ▼
EntityStatsSystems.Recalculate  ← native Hytale (runs AFTER)
```

**All modifiers use `ADDITIVE` stacking on `ModifierTarget.MAX`.** This is intentional — percent-based stats should stack additively. The asset's `max` field handles capping.

### Derivation Formulas

| Derived Stat | Formula | Source Attributes |
|-------------|---------|-------------------|
| Physical Resistance | `(VIT + RES) / 2` | Vitality, Resolve |
| Spell Power | `(INT + FAITH) / 2` | Intelligence, Faith |
| Crit Chance | `min(1.0, DEX / 200)` | Dexterity |
| Crit Damage Multiplier | `1.5 + (STR + INT) / 500` | Strength, Intelligence |
| Attack Damage | `STR / 10` | Strength |
| Move Speed | `1.0 + DEX / 500` | Dexterity |
| Attack Speed | `1.0 + DEX / 300` | Dexterity |
| Magic Resist | `INT / 10` | Intelligence |
| Healing Power | `FAITH / 10` | Faith |
| Mining Speed | `1.0 + STR / 500` | Strength |

These formulas are in `AttributeDerivationFormulas.computeDerived()` — a pure static function. All formulas are covered by 13 unit tests.

---

## Damage Pipeline

Damage flows through a chain of `EntityEventSystem<EntityStore, HeroCoreDamageEvent>` implementations, ordered via `SystemDependency`. Each stage reads stat values from `EntityStatMap` and modifies the event's `modifiedAmount`.

### Pipeline Stages (in execution order)

| # | System | What It Does |
|---|--------|-------------|
| 1 | `AttackDamageBonusSystem` | Scales damage by attacker's `attack_damage` stat |
| 2 | `FallDamageReductionSystem` | Reduces fall damage by victim's `fall_damage_reduction` stat (only activates for `DamageType.FALL` — skips all other damage types). **HeroCore addition** — not in the base refactor guide pipeline. Declares `AFTER AttackDamageBonusSystem` via `SystemDependency`. |
| 3 | `ResistanceMitigationSystem` | Applies elemental/magic resistance (not physical — that's native) |
| 4 | `CriticalHitSystem` | Rolls crit from `crit_chance`, multiplies by `crit_damage_multiplier` |
| 5 | `LifestealSystem` | Heals attacker for `lifesteal` % of damage (dispatches `HeroCoreHealEvent`) |
| 6 | `MinimumDamageSystem` | Enforces configurable minimum damage floor |
| 7 | `DamageApplicationSystem` | Writes final damage to `EntityStatMap` via `subtractStatValue()` |

### HeroCoreDamageEvent

```java
public class HeroCoreDamageEvent extends CancellableEcsEvent {
    @Nullable Ref<EntityStore> attacker;  // null = environmental
    DamageType damageType;                // PHYSICAL, FIRE, etc.
    float rawAmount;                      // original damage
    float modifiedAmount;                 // current (mutated by pipeline)
    boolean isCrit;                       // set by CriticalHitSystem
}
```

### Dispatching Damage

From any ECS system with access to a `CommandBuffer`:

```java
// Dispatch damage to a target entity — flows through entire pipeline
cb.invoke(targetRef, new HeroCoreDamageEvent(attackerRef, DamageType.PHYSICAL, 25f));

// Environmental damage (no attacker)
cb.invoke(targetRef, new HeroCoreDamageEvent(null, DamageType.FIRE, 10f));
```

`CommandBuffer.invoke()` is **immediate** — the event flows through all pipeline stages synchronously.

### Cancelling Damage

Any pipeline stage (or a downstream plugin's system) can cancel:

```java
event.setCancelled(true); // all subsequent stages check isCancelled()
```

---

## Heal Pipeline

Same pattern as damage. Three `EntityEventSystem<EntityStore, HeroCoreHealEvent>` stages:

| # | System | What It Does |
|---|--------|-------------|
| 1 | `HealingPowerScalingSystem` | Scales healing by healer's `healing_power` stat (SPELL/PASSIVE heals, optionally REGEN_TICK) |
| 2 | `HealingReceivedBonusSystem` | Applies target's `healing_received_bonus` stat |
| 3 | `HealCritSystem` | Rolls heal crit from `heal_crit_chance`, multiplies by `heal_crit_multiplier` |

### HeroCoreHealEvent

```java
public class HeroCoreHealEvent extends CancellableEcsEvent {
    @Nullable Ref<EntityStore> healer;
    HealType healType;       // SPELL, POTION, REGEN_TICK, PASSIVE, ENVIRONMENTAL
    float rawAmount;
    float modifiedAmount;
    boolean isCrit;
}
```

### Dispatching Heals

```java
cb.invoke(targetRef, new HeroCoreHealEvent(healerRef, HealType.SPELL, 50f));
```

---

## Combat State & Timeout

### CombatStateComponent

Entities enter combat when they take damage (`DamageApplicationSystem` calls `combat.enterCombat()`). The timer accumulates via world tick `dt` — no wall clock.

### CombatTimeoutSystem

A `DelayedEntitySystem` that fires every 0.5 seconds (intentionally slower than `StatusEffectTickSystem`'s 0.25s — combat timeout doesn't need sub-second precision, and 0.5s reduces unnecessary iteration). Iterates entities with `CombatStateComponent`, checks if `secondsSinceLastDamage > combatTimeoutSeconds` (configurable, default 8.0s from config). When timeout elapses:

1. Sets `inCombat = false`
2. Dispatches `CombatExitEvent` via `cb.invoke()` — downstream plugins can react to this

### CombatExitEvent

```java
public class CombatExitEvent extends EcsEvent {
    // Marker event — no fields. Dispatched when combat timeout expires.
}
```

---

## Status Effects

### How Effects Work

1. **Apply:** Write `StaticModifier` entries into `EntityStatMap`, record the effect in `StatusEffectIndexComponent`, and call `trackModifier()` to register each modifier for automatic cleanup
2. **Tick:** `StatusEffectTickSystem` decrements `remainingSeconds` by `dt` every 0.25s (intentionally fast — status effects need sub-second precision)
3. **Expire:** When `remainingSeconds <= 0`, `StatusEffectTickSystem` removes all tracked modifiers from `EntityStatMap` (via the `ModifierRef` list) and removes the index entry. **The tick system owns full cleanup** — callers only need to call `trackModifier()` at apply time

### Applying an Effect (downstream code)

```java
// Write the stat modifier
EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
String modKey = HeroCoreModifiers.effect("burn", "health_regen");
statMap.putModifier(healthRegenIndex, modKey,
    new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, -5f));

// Record in the index so it auto-expires
StatusEffectIndexComponent index = store.getComponent(ref, StatusEffectIndexComponent.getComponentType());
index.addEffect("burn", 1, 10f); // 1 stack, 10 seconds

// Register the modifier for automatic cleanup on expiry
StatusEffectIndexComponent.EffectEntry entry = index.getEffect("burn");
entry.trackModifier(healthRegenIndex, modKey);
// StatusEffectTickSystem will remove this modifier when the effect expires
```

### StatusEffectDefinition (API data record)

```java
// In api/status/StatusEffect.java — builder pattern
StatusEffect effect = StatusEffect.builder()
    .id("herocore:burn")
    .durationSeconds(5f) // seconds — consistent with ECS dt-based timing
    .source("fire_trap")
    .stacks(3)
    .build();
```

---

## Leveling & Progression

### XP Curves

HeroCore provides three XP curve implementations via `XpCurve`:

| Type | Factory | Description |
|------|---------|-------------|
| Exponential | `XpCurve.exponential(baseXp, growthRate)` | Standard RPG curve |
| Linear | `XpCurve.linear(xpPerLevel)` | Flat XP per level |
| Discrete | `XpCurve.discrete(long[] thresholds)` | Custom thresholds |

Presets via `XpCurveFactory`:
- `XpCurveFactory.standard()` — base 100, growth 1.15
- `XpCurveFactory.fast()` — base 50, growth 1.10
- `XpCurveFactory.slow()` — base 200, growth 1.25
- `XpCurveFactory.flatCurve()` — 1000 XP per level

### LevelingProfile

```java
LevelingProfile profile = LevelingProfile.builder()
    .id("combat")
    .maxLevel(60)
    .xpCurve(XpCurve.exponential(100, 1.15))
    .build();
```

### LevelingRegistry

Register profiles and grant XP:

```java
LevelingRegistry reg = HeroCore.get().getLevelingRegistry();
reg.register(profile);

// In an ECS system with access to Ref, Store, and CommandBuffer:
reg.grantXP(entityRef, store, cb, "combat", 150.0, XPSource.KILL);
int level = reg.getLevel(entityRef, store, "combat");
```

### Level Events

```java
// EcsEvent — dispatched through the ECS event system
public class LevelUpEvent extends EcsEvent {
    int oldLevel, newLevel;
}
public class LevelDownEvent extends EcsEvent {
    int oldLevel, newLevel;
}
```

`LevelUpEvent` is dispatched when XP gain causes the entity's computed level to increase (via `LevelingRegistry.grantXP()`). `LevelDownEvent` is dispatched when XP is explicitly set lower via `LevelingRegistry.setXP()` and the recalculated level is lower than the previous level. Both events carry the old and new level values.

---

## Configuration

Configuration loads from `mods/herocore/config.json`. If not present, defaults are copied from the built-in `hero-core-defaults.json`.

### Config Sections

| Section | Key Fields |
|---------|-----------|
| `modifierStacking` | `maxPercentAdditive`, `multiplicativeCap` |
| `damage` | `critDamageBaseMultiplier` (default 2.0), `minimumDamage` (default 1.0), `maxResistanceReduction` (default 0.9) |
| `heal` | `healCritBaseMultiplier`, `healingPowerScalesRegenTick`, `healingPowerScalesPassive` |
| `resourceRegen` | `tickIntervalSeconds` (default 2.0), `outOfCombatBonusMultiplier`, `combatTimeoutSeconds` (default 8.0) |
| `leveling` | `defaultMaxLevel` (60), `sourceWeights` (per XPSource multipliers) |
| `attributeDerivation` | Per-attribute derivation coefficients (vitality, strength, dex, intel, faith, resolve) |
| `defenseDerivation` | Per-damage-type resistance derivation coefficients |

---

## Heroes RPG Mod Implementation Guide

This section is a practical checklist for wiring HeroCore into a full RPG mod: leveling and XP, primary attributes, derived/secondary stats, and equipment integration.

### 1) Leveling and XP

1. Define one or more `LevelingProfile` entries (combat, crafting, gathering, etc.).
2. Register profiles in `LevelingRegistry` during plugin `setup()` or `start()`.
3. Grant XP from gameplay events using `LevelingRegistry.grantXP()`.

```java
LevelingRegistry leveling = HeroCore.get().getLevelingRegistry();
leveling.register(LevelingProfile.builder()
    .id("combat")
    .maxLevel(60)
    .xpCurve(XpCurve.exponential(100, 1.15))
    .build());

// Award XP in an ECS system with Store + CommandBuffer
leveling.grantXP(entityRef, store, cb, "combat", 120.0, XPSource.KILL);
```

React to progression with ECS events:

```java
reg.registerSystem(new EntityEventSystem<EntityStore, LevelUpEvent>(LevelUpEvent.class) {
    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    protected void handle(int index, ArchetypeChunk<EntityStore> chunk,
                          Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                          LevelUpEvent event) {
        HeroCoreStatsComponent stats = chunk.getComponent(index, HeroCoreStatsComponent.getComponentType());
        stats.setStrength(stats.getStrength() + 1f);
        stats.setVitality(stats.getVitality() + 2f);
    }
});
```

### 2) Primary Attributes and Secondary Stats

- **Primary attributes** live in `HeroCoreStatsComponent` (STR/DEX/INT/FAITH/VIT/RES).
- **Secondary stats** are derived and written into `EntityStatMap` by `AttributeDerivationSystem` every tick.
- Derived values are computed by `AttributeDerivationFormulas` and applied as `StaticModifier` entries.

If you modify primary attributes, do not recalculate secondary stats manually. The next tick will update the modifiers:

```java
HeroCoreStatsComponent stats = store.getComponent(ref, HeroCoreStatsComponent.getComponentType());
stats.setDexterity(stats.getDexterity() + 5f);
```

Reading secondary stats always goes through `EntityStatMap`:

```java
EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
int critIdx = HeroCoreStatTypes.getCritChance();
float critChance = statMap.get(critIdx).get();
```

### 3) Equipment, Inventory, and Item Events

HeroCore does not implement inventory or equipment events directly. Use Hytale's inventory or equipment events (or your own mod's item system) and translate those into stat modifiers.

Recommended pattern:

1. Listen for item equip/unequip or inventory change events (outside HeroCore).
2. Apply or remove modifiers using `HeroCoreModifiers.gear()` keys.
3. Store any equipped state in your own component if needed, not in HeroCore.

```java
// Example: apply a chestplate armor bonus
String key = HeroCoreModifiers.gear("chestplate_iron", "armor");
int armorIdx = HeroCoreStatTypes.getPhysicalResistance();
statMap.putModifier(armorIdx, key,
    new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, 12f));

// Remove on unequip
statMap.removeModifier(armorIdx, key);
```

If your project already has an inventory event layer, this is the bridge point to convert items into stat effects.

---

## Downstream Integration Guide

### Dependency Declaration

In your downstream plugin's `manifest.json`:

```json
{
    "Dependencies": {
        "Herotale:Herocore": "*"
    }
}
```

### Reading Stats

All stats live in `EntityStatMap`. HeroCore provides stat index resolvers:

```java
// Read a HeroCore-defined stat from an entity
EntityStatMap stats = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
int critIdx = HeroCoreStatTypes.getCritChance();
float critChance = stats.get(critIdx).get(); // 0.0–1.0

// Read a native Hytale stat
int healthIdx = DefaultEntityStatTypes.getHealth();
float health = stats.get(healthIdx).get();

// Convenience helper
float value = HeroCoreStatTypes.getStatValue(ref, HeroCoreStatTypes.getSpellPower());
```

### Intercepting the Damage Pipeline

Register an `EntityEventSystem` with `SystemDependency` to insert your logic between existing stages:

```java
@Override
protected void setup() {
    var reg = getEntityStoreRegistry();

    reg.registerSystem(new EntityEventSystem<EntityStore, HeroCoreDamageEvent>(HeroCoreDamageEvent.class) {
        @Override
        public Query<EntityStore> getQuery() {
            // Return a query that matches entities with HeroCoreStatsComponent
            return HeroCoreStatsComponent.getComponentType();
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Set.of(
                new SystemDependency<>(Order.AFTER,  CriticalHitSystem.class),
                new SystemDependency<>(Order.BEFORE, LifestealSystem.class)
            );
        }

        @Override
        protected void handle(int index, ArchetypeChunk<EntityStore> chunk,
                              Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                              HeroCoreDamageEvent event) {
            if (event.isCancelled()) return;
            // Your custom damage modification logic here
            // e.g., class-specific damage bonuses, elemental interactions
        }
    });
}
```

### Reacting to Level-Ups

```java
reg.registerSystem(new EntityEventSystem<EntityStore, LevelUpEvent>(LevelUpEvent.class) {
    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    protected void handle(int index, ArchetypeChunk<EntityStore> chunk,
                          Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                          LevelUpEvent event) {
        HeroCoreStatsComponent rpg = chunk.getComponent(index, HeroCoreStatsComponent.getComponentType());
        // Scale attributes on level-up — derivation system picks up changes next tick
        rpg.setVitality(20f + event.getNewLevel() * 2f);
        rpg.setStrength(15f + event.getNewLevel() * 1.5f);
    }
});
```

### Reacting to Combat Exit

```java
reg.registerSystem(new EntityEventSystem<EntityStore, CombatExitEvent>(CombatExitEvent.class) {
    @Override
    public Query<EntityStore> getQuery() {
        // Match entities with CombatStateComponent
        return CombatStateComponent.getComponentType();
    }

    @Override
    protected void handle(int index, ArchetypeChunk<EntityStore> chunk,
                          Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                          CombatExitEvent event) {
        // Entity left combat — trigger out-of-combat effects, UI changes, etc.
    }
});
```

### Modifying Primary Attributes

Write directly to `HeroCoreStatsComponent`. The `AttributeDerivationSystem` automatically recalculates derived modifiers on the next tick:

```java
HeroCoreStatsComponent stats = store.getComponent(ref, HeroCoreStatsComponent.getComponentType());
stats.setStrength(stats.getStrength() + 5f);
// Next tick: AttributeDerivationSystem writes updated modifiers into EntityStatMap
```

### Writing Custom Modifiers

Use `HeroCoreModifiers` key conventions for namespace safety:

```java
EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
int critIdx = HeroCoreStatTypes.getCritChance();

// Apply a modifier (namespace with your plugin prefix)
// Hytale API: putModifier(int statIndex, String key, Modifier modifier)
statMap.putModifier(critIdx, "myplugin:elixir_crit_boost",
    new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, 0.1f));

// Remove it later
// Hytale API: removeModifier(int statIndex, String key)
statMap.removeModifier(critIdx, "myplugin:elixir_crit_boost");
```

> **Note on `putModifier` argument order:** The Hytale compiled API signature is `putModifier(int statIndex, String key, Modifier modifier)` — stat index first, then key string, verified from the decompiled `EntityStatMap.java`. This matches `removeModifier(int statIndex, String key)`. Note that some documentation (including the refactor guide) shows the arguments in a different order `(String key, int index, ...)` — that order is **incorrect** per the actual bytecode. All HeroCore code uses the verified `(int, String, Modifier)` order.

### Granting XP

```java
// Inside an ECS system (has access to Ref, Store, CommandBuffer):
LevelingRegistry leveling = HeroCore.get().getLevelingRegistry();
leveling.grantXP(entityRef, store, cb, "combat", 100.0, XPSource.KILL);
```

### Using Mob Profiles

```java
MobRegistry mobs = HeroCore.get().getMobRegistry();
mobs.registerProfile(myBossProfile);
Optional<MobProfile> profile = mobs.getProfile("skeleton_warrior");
```

### Using Zone Modifiers (v0.3 planned)

> **⚠️ Not yet implemented.** `ZoneModifierRegistry` with `ZoneTrigger.REGION_ENTRY` is a planned v0.3 feature. The API surface exists but zone trigger integration with Hytale's world system is not wired. **Do not use this API in production code** — it will compile but have no runtime effect. See [Planned Features](#planned-features) for details.

---

### Faction API

HeroCore exposes a lightweight Faction API for downstream plugins to manage factions, membership, roles, and relations. It's intended to be a small, safe facade that stores faction meta and membership lists and dispatches events for membership/relationship changes.

Key classes and concepts:
- `FactionRegistry` — register and look up `FactionProfile` objects.
- `FactionProfile` — id, display name, persistent metadata (tags, color).
- `FactionMember` — membership record (ref, role, joinedAt).
- `FactionRole` — ordered role enum (OWNER, OFFICER, MEMBER, RECRUIT).
- `FactionRelationEvent` — dispatched when faction relations or membership change.

Basic usage (downstream):

1. Declare dependency in `manifest.json` as usual (see Downstream Integration Guide).
2. Register a faction profile during your `setup()`:

```java
FactionRegistry factions = HeroCore.get().getFactionRegistry();
factions.register(FactionProfile.builder()
    .id("myplugin:bandits")
    .displayName("Bandit Gang")
    .build());
```

3. Add / remove members and query membership from your gameplay systems:

```java
factions.addMember("myplugin:bandits", playerRef, FactionRole.MEMBER);
Optional<FactionProfile> p = factions.getProfile("myplugin:bandits");
boolean isMember = factions.isMember(playerRef, "myplugin:bandits");
```

Notes:
- The registry is thread-safe for normal ECS usage patterns; mutate factions via systems or initialization code.
- Faction membership changes dispatch `FactionRelationEvent` through the ECS so other systems can react (chat tags, PvP rules).

### Language API

HeroCore includes a small Language API used by the in-project tests and text utilities. It provides translation lookups, training registry hooks for localized content, and a `TextDistortionEngine` for stylistic effects.

Key classes and assets:
- `LanguageService` — primary API: `translate(key, locale, placeholders...)`.
- `TrainingRegistry` — registry for learning/teaching phrases used in tests and optional gameplay features.
- `TextDistortionEngine` — utilities for obfuscation, leetspeak, and stylistic distortions used by tests (e.g., `TextDistortionEngineTest`).
- Language packs live in `src/main/resources/hero-core-languages-default.json` and are loaded by default.

Basic usage:

```java
LanguageService lang = HeroCore.get().getLanguageService();
String welcome = lang.translate("ui.welcome", Locale.EN_US, Map.of("player", playerName));

// Distort a string for an effect
String spooky = HeroCore.get().getTextDistortionEngine().distort("Beware the forest", DistortionMode.GHOST);
```

Notes and guidance:
- The service falls back to built-in language assets if no server pack is provided.
- Keep translation keys simple and namespaced (e.g., `myplugin.item.sword.name`).
- `TrainingRegistry` is useful if you need dynamic or procedurally generated phrases — see tests for usage patterns.

---

## Planned Features

These APIs exist as interface/class definitions but are **not yet wired** to Hytale's runtime. They are reserved for future versions.

### Zone Modifier System (v0.3)

`ZoneModifierRegistry` will allow applying attribute modifiers when entities enter or leave world zones. The registry interface and `ZoneModifierDefinition` builder are defined but zone boundary detection via Hytale's world system is not yet integrated.

```java
// Future usage (v0.3):
ZoneModifierRegistry zones = HeroCore.get().getZoneModifierRegistry();
zones.register(ZoneModifierDefinition.builder()
    .zoneId("dungeon_fire")
    .trigger(ZoneTrigger.REGION_ENTRY)
    .modifier(new AttributeModifier(RPGAttribute.FIRE_RESISTANCE, -0.2))
    .build());
```

### HeroCoreReadyEvent

`HeroCoreReadyEvent` is defined as a lifecycle signal for downstream plugins that treat HeroCore as an **optional dependency**. Currently, this event is **not dispatched** — it exists as a reserved API surface.

For plugins that declare HeroCore as a **required dependency** in `manifest.json`, standard load ordering is sufficient: HeroCore's `setup()` and `start()` will complete before your plugin's `setup()` runs. You do not need this event.

This event will be wired in a future version for optional-dependency use cases where a plugin needs to detect HeroCore availability at runtime.

---

## API Reference

### Key Classes by Package

#### `net.herotale.herocore.api`

| Class | Description |
|-------|-------------|
| `HeroCore` | Service locator — `HeroCore.get()` for registries |

#### `net.herotale.herocore.api.attribute`

| Class | Description |
|-------|-------------|
| `RPGAttribute` | Enum of all 62 RPG attributes |
| `AttributeModifier` | Record: attribute + value + label |

#### `net.herotale.herocore.api.component`

| Class | Description |
|-------|-------------|
| `HeroCoreStatsComponent` | Primary attribute bases (persistent) |
| `HeroCoreProgressionComponent` | Level and XP (persistent) |
| `CombatStateComponent` | In-combat tracking (transient) |
| `StatusEffectIndexComponent` | Active effect index (transient) |

#### `net.herotale.herocore.api.damage`

| Class | Description |
|-------|-------------|
| `HeroCoreDamageEvent` | `CancellableEcsEvent` — damage pipeline event |
| `DamageType` | Enum: PHYSICAL, MAGICAL, FIRE, ICE, LIGHTNING, POISON, ARCANE, TRUE, FALL, VOID |
| `DamageFlag` | Enum: CRIT, DOT, AOE, MELEE, RANGED, SPELL, etc. |

#### `net.herotale.herocore.api.heal`

| Class | Description |
|-------|-------------|
| `HeroCoreHealEvent` | `CancellableEcsEvent` — heal pipeline event |
| `HealType` | Enum: SPELL, POTION, REGEN_TICK, PASSIVE, ENVIRONMENTAL |

#### `net.herotale.herocore.api.entity`

| Class | Description |
|-------|-------------|
| `MobRegistry` | Registry for mob/NPC profiles — `registerProfile()`, `getProfile()` |
| `MobProfile` | Interface: mob identity, base level, base attributes, optional scaling profile |
| `MobScalingProfile` | Interface: per-level stat scaling rules |
| `MobCategory` | Enum: mob classification (used by profiles) |

#### `net.herotale.herocore.api.event`

| Class | Description |
|-------|-------------|
| `LevelUpEvent` | `EcsEvent` — dispatched on level gain (via `grantXP()`) |
| `LevelDownEvent` | `EcsEvent` — dispatched on level loss (via `setXP()` when new level < old) |
| `CombatExitEvent` | `EcsEvent` — dispatched when combat timeout expires |
| `HeroCoreReadyEvent` | Lifecycle signal — **not yet dispatched** (reserved for v0.3 optional-dependency support; see [Planned Features](#planned-features)) |
| `AttributeChangeEvent` | Server event — attribute value changed (reserved — defined but not yet dispatched) |
| `StatusChangeEvent` | Server event — status effect applied/removed (reserved — defined but not yet dispatched) |
| `MobSpawnEvent` | Server event — mob spawned with profile (reserved) |
| `MobDeathEvent` | Server event — mob died (reserved) |
| `ProgressionEvent` | Server event — progression milestone (reserved) |
| `MorphEvent` | Server event — entity morph change (reserved) |
| `ResourceChangeEvent` | Server event — resource pool changed (reserved) |
| `CraftingEvent` | Server event — crafting action (reserved) |

#### `net.herotale.herocore.impl`

| Class | Description |
|-------|-------------|
| `HeroCorePlugin` | Main plugin class — singleton via `HeroCorePlugin.get()` |
| `HeroCoreComponentRegistry` | Registers all 4 components, exposes `ComponentType` handles |
| `HeroCoreStatTypes` | Resolves custom stat indices from JSON assets |
| `HeroCoreModifiers` | Modifier key string constants (HC_ prefix) |

#### `net.herotale.herocore.system.damage`

| Class | Pipeline Position | Description |
|-------|-------------------|-------------|
| `AttackDamageBonusSystem` | 1st | Scales damage by attacker's attack damage |
| `FallDamageReductionSystem` | 2nd | Reduces fall damage (only for `DamageType.FALL`). HeroCore addition — declares `AFTER AttackDamageBonusSystem`. |
| `ResistanceMitigationSystem` | 3rd | Applies resistance per damage type |
| `CriticalHitSystem` | 4th | Rolls and applies crits |
| `LifestealSystem` | 5th | Heals attacker for % of damage |
| `MinimumDamageSystem` | 6th | Enforces minimum damage floor |
| `DamageApplicationSystem` | 7th (final) | Writes damage to EntityStatMap |

#### `net.herotale.herocore.system.heal`

| Class | Pipeline Position | Description |
|-------|-------------------|-------------|
| `HealingPowerScalingSystem` | 1st | Scales healing by healing power |
| `HealingReceivedBonusSystem` | 2nd | Applies healing received bonus |
| `HealCritSystem` | 3rd | Rolls and applies heal crits |

#### `net.herotale.herocore.system.combat`

| Class | Description |
|-------|-------------|
| `CombatTimeoutSystem` | `DelayedEntitySystem` — exits combat after timeout |
| `StatusEffectTickSystem` | `DelayedEntitySystem` — decrements effect timers (every 0.25s), removes expired effects and their tracked modifiers |

---

## Building

```bash
# Build and run all tests
./gradlew clean build

# Tests only
./gradlew test
```

**Requirements:**
- JDK 25
- Gradle 9.2.0 (wrapper included)
- Hytale Server API `2026.02.17+` (resolved from `maven.hytale.com`)

**Test Suites (62 tests total):**
- `AttributeDerivationFormulasTest` — 13 tests covering all derivation formulas
- `DamageFormulasTest` — 21 tests covering resistance, crit, lifesteal, min damage
- `HealFormulasTest` — 14 tests covering heal scaling, heal crit
- `XpCurveTest` — 14 tests covering exponential, linear, discrete XP curves

All math tests are pure-function tests that require no server instance.

---

## Compliance Notes
- **No bridge pattern** — `EntityStatMap` is the single source of truth
- **No parallel stat engine** — `HeroCoreStatsComponent` holds only primary bases
- **No custom event bus** — all events use `CancellableEcsEvent` / `EcsEvent` via `CommandBuffer.invoke()`
- **No custom system ordering** — `SystemDependency` with `Order.BEFORE` / `Order.AFTER`
- **No `ScheduledExecutorService`** — tick systems use `DelayedEntitySystem` and `EntityTickingSystem`
- **No standalone Gson persistence** — `BuilderCodec` with `HC_`-prefixed keys
- **No custom system abstraction** — all systems extend native Hytale types directly
- **`Ref<EntityStore>` in all runtime logic** — UUID only at persistence boundaries
