# HeroCore

> **An ECS-native RPG attribute and combat system for Hytale servers**

[![Version](https://img.shields.io/badge/version-*-blue.svg)](manifest.json)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Hytale](https://img.shields.io/badge/Hytale-2026.02.06+-purple.svg)](https://hytale.com)

HeroCore is a foundational RPG system library for Hytale servers that provides attribute management, combat mechanics, leveling systems, and status effects through a composable Entity Component System (ECS) architecture. Built as a schema provider and standard library, it enables downstream plugins to implement rich RPG experiences without reinventing core mechanics.

---

## Features

### Core Systems
- **Six Primary Attributes**: Strength, Dexterity, Intelligence, Faith, Vitality, Resolve
- **Auto-Derived Secondary Attributes**: Over 30 computed stats (health, mana, crit chance, resistances, etc.)
- **Flexible Modifier System**: FLAT, PERCENT_ADDITIVE, PERCENT_MULTIPLICATIVE, and OVERRIDE types
- **Damage Systems**: Physical, elemental (fire/ice/lightning/poison/arcane), and true damage with resistance mitigation
- **Heal Systems**: Standard healing, heal crits, lifesteal, and vampirism
- **Status Effects**: Stackable, duration-based effects with intensity, max stacks, and refresh modes

### Resource Management
- **Resource Pools**: Health, Mana, Stamina, Oxygen, Ammo with configurable regen and costs
- **Combat State Tracking**: Automatic combat timeout and out-of-combat bonuses
- **Resource Regen System**: Tick-based regeneration with combat awareness

### Progression
- **Leveling System**: Configurable XP curves, level caps, and multi-source XP (kills, quests, crafting, etc.)
- **Reputation System**: Faction-based reputation with gain/loss mechanics
- **Experience Statistics**: Detailed tracking per XP source

### Integration
- **Event-Driven Architecture**: All combat actions emit events for downstream plugins to intercept
- **Bridge Layer**: Optional sync between RPG stats and Hytale engine (movement speed, mining speed, attack speed)
- **Persistence**: JSON-based attribute serialization with Gson
- **Database Support**: HikariCP connection pooling for external storage

---

## Architecture

HeroCore follows an **Entity Component System** pattern where:

- **Components** are data bags attached to entities (players, mobs)
- **Systems** are standalone event handlers with declarative ordering
- **Events** carry intent and are dispatched through entity event buses

```
Entity (UUID)
  ├─ StatsComponent (attributes + modifiers)
  ├─ ResourcePoolComponent (health/mana/stamina)
  ├─ StatusEffectsComponent (active effects)
  ├─ CombatStateComponent (in combat? last damage time)
  └─ LevelingComponent (level, XP, reputation)
```

Systems never call each other directly — they react to events in the order defined by `@SystemOrder` annotations.

### Design Principles

1. **Composition over inheritance** — no deep class hierarchies
2. **Data-oriented** — components are plain data, logic lives in systems
3. **Decoupled** — systems communicate via events, not method calls
4. **Testable** — all math is pure functions, no server instance required for unit tests
5. **Extensible** — downstream plugins add their own components/systems

---

## Installation

### Requirements

- **Java 25** (Adoptium/Temurin recommended)
- **Hytale Server** `2026.02.06+`
- **Gradle** 8.0+ (bundled via wrapper)

### As a Server Plugin

1. Download the latest release JAR from the releases page
2. Place `herocore*.jar` in your server's `mods/` directory
3. Start the server — HeroCore will generate `mods/herocore/config.json`
4. Configure attribute derivations, damage formulas, and system behavior in the config

### As a Dependency (For Plugin Developers)

Add HeroCore as a dependency in your plugin:

```gradle
repositories {
    mavenCentral()
    // Add your local or remote repository for HeroCore
    flatDir { dirs("../herocore/build/libs") }
}

dependencies {
    compileOnly(files("../herocore/build/libs/herocore-*.jar"))
    // Or use a Maven coordinate once published
    // compileOnly("net.herotale:herocore:*")
}
```

Declare the dependency in your `manifest.json`:

```json
{
  "Dependencies": {
    "net.herotale:herocore": "*"
  }
}
```

---

## Quick Start

### Creating Components for a Player

```java
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.component.ResourcePoolComponent;
import net.herotale.herocore.api.attribute.RPGAttribute;

// On player join
UUID playerId = player.getUUID();

// Create stats component with default config
StatsComponent stats = StatsComponent.create(coreConfig.attributeDerivation());

// Set primary attributes
stats.setBase(RPGAttribute.STRENGTH, 15.0);
stats.setBase(RPGAttribute.VITALITY, 20.0);
stats.setBase(RPGAttribute.DEXTERITY, 12.0);

// Read computed values (auto-derived from primaries)
double maxHealth = stats.getValue(RPGAttribute.MAX_HEALTH);  // 100 + 20*10 + resolve*5
double critChance = stats.getValue(RPGAttribute.CRIT_CHANCE); // 12 * 0.005 = 0.06 (6%)

// Create resource pools
ResourcePoolComponent resources = ResourcePoolComponent.create();
resources.setMaxHealth(maxHealth);
resources.setHealth(maxHealth);
resources.setMaxMana(stats.getValue(RPGAttribute.MAX_MANA));

// Store in your plugin's entity map
entityMap.put(playerId, new PlayerData(stats, resources, ...));
```

### Applying a Modifier

```java
import net.herotale.herocore.api.attribute.AttributeModifier;
import net.herotale.herocore.api.attribute.ModifierType;

// Give a player +10% attack speed from an item
AttributeModifier speedBoost = new AttributeModifier(
    "iron_sword_speed",           // unique ID
    RPGAttribute.ATTACK_SPEED,    // target attribute
    0.10,                          // +10%
    ModifierType.PERCENT_ADDITIVE, // stacks additively with other %
    "Iron Sword",                  // source name
    600_000                        // 10 min duration (ms)
);

stats.addModifier(speedBoost);
double newSpeed = stats.getValue(RPGAttribute.ATTACK_SPEED); // 1.0 * 1.10 * (1 + dex*0.01)
```

### Dealing Damage

```java
import net.herotale.herocore.api.event.DamageEvent;
import net.herotale.herocore.api.event.DamageType;

// Post a damage event on the target entity's event bus
DamageEvent event = new DamageEvent(
    attackerId,
    targetId,
    50.0,                  // raw damage
    DamageType.PHYSICAL,   // damage type
    false,                 // not a critical hit (systems will compute)
    "Sword Strike"         // source label
);

targetEntity.getEventBus().post(event);

// After systems process:
// - CritSystem may upgrade it to a crit
// - ResistanceMitigationSystem reduces damage by target's armor
// - MinimumDamageSystem ensures at least 1.0 damage
// - Final damage is applied to ResourcePoolComponent
```

### Reading Status Effects

```java
import net.herotale.herocore.api.component.StatusEffectsComponent;
import net.herotale.herocore.api.statuseffect.StatusEffect;

StatusEffectsComponent effects = getEffects(playerId);

// Check for a specific effect
boolean isPoisoned = effects.hasEffect("poison");

// Get effect details
StatusEffect poison = effects.getEffect("poison");
if (poison != null) {
    int stacks = poison.getCurrentStacks();
    double intensity = poison.getIntensity();
    long remaining = poison.getRemainingDuration();
}

// List all active effects
List<StatusEffect> active = effects.getActiveEffects();
```

---

## Configuration

HeroCore generates `mods/herocore/config.json` on first run. Key sections:

### Attribute Derivation

Control how secondary attributes scale from primaries:

```json
{
  "attributeDerivation": {
    "vitality": {
      "healthPerPoint": 10.0,
      "healthBase": 100.0,
      "armorPercentPerPoint": 0.002
    },
    "strength": {
      "baseAttackDamagePerPoint": 0.5,
      "blockStrengthPerPoint": 2.0,
      "shieldStrengthPerPoint": 3.0
    },
    "dexterity": {
      "critChancePerPoint": 0.005,
      "attackSpeedPercentPerPoint": 0.01,
      "fallDamageReductionPerPoint": 0.01,
      "fallDamageReductionCap": 0.5
    },
    "resolve": {
      "healthPerPoint": 5.0,
      "ccResistancePerPoint": 0.01,
      "ccResistanceCap": 0.75
    }
  }
}
```

### Defense & Combat Implementation Guide for Heroes

This guide walks through setting up HeroCore's defense/resistance system and level-based damage scaling in your Heroes RPG plugin.

---

#### Part 1: Configure Defense Derivation

HeroCore derives resistance values from primary attributes. Configure `mods/herocore/config.json`:

```json
{
  "defenseDerivation": {
    "physicalResistanceFlatPerVitality": 0.5,
    "physicalResistancePercentPerVitality": 0.002,
    "physicalResistancePerLevelPercent": 0.01,
    "projectileResistanceFlatPerDexterity": 0.3,
    "projectileResistancePercentPerDexterity": 0.001,
    "fireResistanceFlatPerResolve": 0.4,
    "fireResistancePercentPerResolve": 0.0015,
    "iceResistancePercentPerIntelligence": 0.001,
    "lightningResistancePercentPerIntelligence": 0.001,
    "poisonResistancePercentPerResolve": 0.001,
    "arcaneResistancePercentPerIntelligence": 0.001,
    "maxResistancePercent": 0.75
  }
}
```

**What this does:**
- **Vitality** → Physical resistance (flat + percent)
- **Dexterity** → Projectile resistance (flat + percent)
- **Resolve** → Fire & Poison resistance (percent)
- **Intelligence** → Ice, Lightning, Arcane resistance (percent)
- All percent resistances capped at 75% to prevent immunity

---

#### Part 2: Dual-Layer Defense Architecture

HeroCore uses a **dual-layer** defense model:

| Layer | Scope | Display |
|---|---|---|
| **Vanilla Layer** | Physical Defense only | Hytale tab menu (vanilla "Defense" stat) |
| **HeroCore Layer** | All resistances | Heroes UI / character sheet |

**Why?** Hytale's tab menu only shows Physical Defense. HeroCore preserves this while adding detailed resistance tracking for your custom UI.

**Key Rules:**
- **Physical mitigation** → Handled by Hytale's native `ArmorDamageReduction` system
- **All other resistances** → Handled by HeroCore's `ResistanceMitigationSystem`
- Use `DefenseBridge` to sync Physical resistance to Hytale native for tab display only

---

#### Part 3: Apply Defense Bridge (Physical Only)

Call `DefenseBridge.apply()` whenever a player's stats change:

```java
import net.herotale.herocore.impl.bridge.DefenseBridge;
import net.herotale.herocore.api.component.StatsComponent;

public class HeroesPlayerService {
    
    public void onLevelUp(UUID playerId, int newLevel) {
        StatsComponent stats = getStatsComponent(playerId);
        
        // 1. Update base primaries from class/level progression
        stats.setBase(RPGAttribute.VITALITY, getVitalityForLevel(newLevel));
        stats.setBase(RPGAttribute.DEXTERITY, getDexterityForLevel(newLevel));
        // ... other primaries
        
        // 2. Apply level-based defense modifiers (see Part 4)
        applyLevelDefenseModifiers(stats, newLevel);
        
        // 3. Sync Physical resistance to Hytale native (tab menu display)
        DefenseBridge bridge = new DefenseBridge(playerId);
        bridge.apply(stats);
    }
    
    public void onEquipGear(UUID playerId, ItemStack gear) {
        StatsComponent stats = getStatsComponent(playerId);
        
        // Add gear modifiers
        stats.addModifier(AttributeModifier.builder()
            .id("gear:" + gear.getId() + ":physical_resist")
            .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
            .value(0.05) // +5% physical resist
            .type(ModifierType.FLAT)
            .source(ModifierSource.of("gear:" + gear.getId()))
            .build());
        
        // Re-sync to native
        new DefenseBridge(playerId).apply(stats);
    }
}
```

**When to call `DefenseBridge.apply()`:**
- Player levels up
- Primary attributes change (stat point allocation, talent bonuses)
- Gear equipped/unequipped
- Buff applied/removed
- Class change

---

#### Part 4: Level-Based Defense Scaling

Add **per-level** defense bonuses using additive modifiers:

```java
import net.herotale.herocore.api.attribute.AttributeModifier;
import net.herotale.herocore.api.attribute.ModifierSource;
import net.herotale.herocore.api.attribute.ModifierType;
import net.herotale.herocore.api.attribute.RPGAttribute;

public void applyLevelDefenseModifiers(StatsComponent stats, int level) {
    CoreConfig config = HeroCorePlugin.get().getConfig();
    
    // Physical Defense: +1% per level (config: physicalResistancePerLevelPercent = 0.01)
    double physDefense = level * config.defenseDerivation().physicalResistancePerLevelPercent();
    stats.addModifier(AttributeModifier.builder()
        .id("herocore:level_defense_physical")
        .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
        .value(physDefense)
        .type(ModifierType.FLAT)
        .source(ModifierSource.of("herocore:level_scaling"))
        .persistent(false) // Re-applied on login
        .build());
    
    // Optional: Add level-based bonuses for other resistances
    // Example: +0.5% fire resist per level
    double fireDefense = level * 0.005;
    stats.addModifier(AttributeModifier.builder()
        .id("herocore:level_defense_fire")
        .attribute(RPGAttribute.FIRE_RESISTANCE_PERCENT)
        .value(fireDefense)
        .type(ModifierType.FLAT)
        .source(ModifierSource.of("herocore:level_scaling"))
        .persistent(false)
        .build());
}
```

**Remove old modifiers before re-applying:**

```java
// Before applying new level-based modifiers, remove old ones
stats.removeModifier("herocore:level_defense_physical");
stats.removeModifier("herocore:level_defense_fire");
// ... then addModifier as shown above
```

---

#### Part 5: Display Resistances in Heroes UI

Use `UIDataProvider` to fetch percent values for your UI:

```java
import net.herotale.herocore.api.ui.UIDataProvider;
import net.herotale.herocore.api.ui.DefenseCategory;

public class HeroesCharacterSheetUI {
    
    private final UIDataProvider uiData;
    
    public void renderDefenseStats(UUID playerId) {
        // Fetch percent values (0.0–1.0)
        double physicalDef = uiData.getDefensePercent(playerId, DefenseCategory.PHYSICAL_DEFENSE);
        double magicalDef = uiData.getDefensePercent(playerId, DefenseCategory.MAGICAL_DEFENSE);
        double projectileDef = uiData.getDefensePercent(playerId, DefenseCategory.PROJECTILE_RESISTANCE);
        double fireDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_FIRE);
        double iceDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_FROST);
        double lightningDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_LIGHTNING);
        double poisonDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_POISON);
        double arcaneDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_ARCANE);
        
        // Format as percentages
        String physText = String.format("Physical Defense: %.1f%%", physicalDef * 100.0);
        String fireText = String.format("Fire Resistance: %.1f%%", fireDef * 100.0);
        
        // Display in UI...
    }
}
```

**Attribute → UI Category Mapping:**

| DefenseCategory | Primary RPGAttribute | Legacy Fallback |
|---|---|---|
| `PHYSICAL_DEFENSE` | `PHYSICAL_RESISTANCE_PERCENT` | — |
| `MAGICAL_DEFENSE` | `MAGIC_RESIST` | — |
| `PROJECTILE_RESISTANCE` | `PROJECTILE_RESISTANCE_PERCENT` | — |
| `ELEMENTAL_FIRE` | `FIRE_RESISTANCE_PERCENT` | `ELEMENTAL_RESIST_FIRE` |
| `ELEMENTAL_FROST` | `ICE_RESISTANCE_PERCENT` | `ELEMENTAL_RESIST_ICE` |
| `ELEMENTAL_LIGHTNING` | `LIGHTNING_RESISTANCE_PERCENT` | `ELEMENTAL_RESIST_LIGHTNING` |
| `ELEMENTAL_POISON` | `POISON_RESISTANCE_PERCENT` | `ELEMENTAL_RESIST_POISON` |
| `ELEMENTAL_ARCANE` | `ARCANE_RESISTANCE_PERCENT` | `ELEMENTAL_RESIST_ARCANE` |

---

#### Part 6: Level-Based Damage Scaling

Add **per-level** attack damage bonuses:

```java
public void applyLevelDamageModifiers(StatsComponent stats, int level) {
    // Example: +2% attack damage per level
    double damageBonus = level * 0.02;
    
    stats.addModifier(AttributeModifier.builder()
        .id("herocore:level_damage_bonus")
        .attribute(RPGAttribute.ATTACK_DAMAGE)
        .value(damageBonus)
        .type(ModifierType.PERCENT_ADDITIVE)
        .source(ModifierSource.of("herocore:level_scaling"))
        .persistent(false)
        .build());
    
    // For spell casters: scale spell power
    double spellPowerBonus = level * 0.015;
    stats.addModifier(AttributeModifier.builder()
        .id("herocore:level_spell_power_bonus")
        .attribute(RPGAttribute.SPELL_POWER)
        .value(spellPowerBonus)
        .type(ModifierType.PERCENT_ADDITIVE)
        .source(ModifierSource.of("herocore:level_scaling"))
        .persistent(false)
        .build());
}
```

**Apply on level-up:**

```java
public void onLevelUp(UUID playerId, int newLevel) {
    StatsComponent stats = getStatsComponent(playerId);
    
    // Remove old scaling modifiers
    stats.removeModifier("herocore:level_damage_bonus");
    stats.removeModifier("herocore:level_spell_power_bonus");
    stats.removeModifier("herocore:level_defense_physical");
    
    // Re-apply with new level values
    applyLevelDamageModifiers(stats, newLevel);
    applyLevelDefenseModifiers(stats, newLevel);
    
    // Sync to native
    new DefenseBridge(playerId).apply(stats);
}
```

---

#### Part 7: Gear & Buff Modifiers

Stack resistance/damage modifiers from gear and buffs naturally:

```java
// Armor piece: +8% physical resistance, +15 flat physical resistance
stats.addModifier(AttributeModifier.builder()
    .id("gear:iron_chestplate:phys_pct")
    .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
    .value(0.08)
    .type(ModifierType.FLAT)
    .source(ModifierSource.of("gear:iron_chestplate"))
    .build());

stats.addModifier(AttributeModifier.builder()
    .id("gear:iron_chestplate:phys_flat")
    .attribute(RPGAttribute.PHYSICAL_RESISTANCE)
    .value(15.0)
    .type(ModifierType.FLAT)
    .source(ModifierSource.of("gear:iron_chestplate"))
    .build());

// Buff: +20% fire resistance for 60 seconds
stats.addModifier(AttributeModifier.builder()
    .id("buff:fire_ward")
    .attribute(RPGAttribute.FIRE_RESISTANCE_PERCENT)
    .value(0.20)
    .type(ModifierType.FLAT)
    .source(ModifierSource.of("buff:fire_ward"))
    .duration(60_000) // 60 seconds
    .build());
```

**All modifiers stack according to HeroCore's formula:**

```
FinalValue = (Base + Σ FLAT) * (1 + Σ PERCENT_ADDITIVE) * Π(1 + each PERCENT_MULTIPLICATIVE)
```

---

#### Part 8: Class-Specific Resistance Bonuses

Add class-specific baseline resistances:

```java
public enum HeroesClass {
    WARRIOR(0.10, 0.05), // 10% phys, 5% fire
    MAGE(0.02, 0.15),    // 2% phys, 15% arcane
    ROGUE(0.05, 0.08);   // 5% phys, 8% poison
    
    private final double physicalResist;
    private final double elementalResist;
    
    // ...
}

public void applyClassBonuses(StatsComponent stats, HeroesClass heroClass) {
    stats.addModifier(AttributeModifier.builder()
        .id("class:" + heroClass.name().toLowerCase() + ":phys_resist")
        .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
        .value(heroClass.getPhysicalResist())
        .type(ModifierType.FLAT)
        .source(ModifierSource.of("class:" + heroClass.name().toLowerCase()))
        .persistent(true) // Saved to database
        .build());
}
```

---

#### Part 9: Integration Checklist

✅ **Configure defense derivation** in `mods/herocore/config.json`  
✅ **Call `DefenseBridge.apply()`** on stat changes (level, gear, buffs)  
✅ **Add level-based modifiers** for defense and damage  
✅ **Display resistances** in Heroes UI via `UIDataProvider.getDefensePercent()`  
✅ **Stack gear/buff modifiers** naturally via `stats.addModifier()`  
✅ **Apply class-specific bonuses** as persistent modifiers  
✅ **Test resistance caps** (default: 75% max) work correctly  
✅ **Verify vanilla tab menu** shows Physical Defense only

---

#### Part 10: Complete Example

```java
public class HeroesIntegrationExample {
    
    public void setupPlayerDefense(UUID playerId, int level, HeroesClass heroClass) {
        StatsComponent stats = getStatsComponent(playerId);
        
        // 1. Set base primaries from class + level
        stats.setBase(RPGAttribute.VITALITY, 20 + (level * 2));
        stats.setBase(RPGAttribute.DEXTERITY, 15 + (level * 1.5));
        stats.setBase(RPGAttribute.RESOLVE, 10 + (level * 1));
        stats.setBase(RPGAttribute.INTELLIGENCE, 12 + (level * 1.2));
        
        // 2. Apply class bonuses
        applyClassBonuses(stats, heroClass);
        
        // 3. Apply level-based scaling
        applyLevelDefenseModifiers(stats, level);
        applyLevelDamageModifiers(stats, level);
        
        // 4. Sync Physical to Hytale native
        new DefenseBridge(playerId).apply(stats);
        
        // 5. Display in UI
        displayDefenseStats(playerId);
    }
    
    private void displayDefenseStats(UUID playerId) {
        UIDataProvider uiData = HeroCorePlugin.get().getUIDataProvider();
        
        double physDef = uiData.getDefensePercent(playerId, DefenseCategory.PHYSICAL_DEFENSE);
        double fireDef = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_FIRE);
        
        // Format: "Physical Defense: 45.2%"
        sendMessage(playerId, String.format("§ePhysical Defense: §a%.1f%%", physDef * 100));
        sendMessage(playerId, String.format("§eFire Resistance: §c%.1f%%", fireDef * 100));
    }
}
```

---

### Damage System

```json
{
  "damage": {
    "critDamageBaseMultiplier": 1.5,
    "minimumDamage": 1.0,
    "maxResistanceReduction": 0.9,
    "resistanceMapping": {
      "PHYSICAL": "ARMOR",
      "FIRE": "ELEMENTAL_RESIST_FIRE",
      "ICE": "ELEMENTAL_RESIST_ICE"
    }
  }
}
```

### Resource Regen

```json
{
  "resourceRegen": {
    "tickIntervalMs": 2000,
    "outOfCombatBonusMultiplier": 3.0,
    "combatTimeoutMs": 8000
  }
}
```

### Leveling

```json
{
  "leveling": {
    "defaultMaxLevel": 60,
    "sourceWeights": {
      "KILL": 1.0,
      "QUEST": 1.0,
      "CRAFTING": 0.8,
      "GATHERING": 0.7
    }
  }
}
```

---

## Building

### Clone and Build

```bash
git clone https://github.com/herotale/herocore.git
cd herocore
./gradlew build
```

The output JAR will be in `build/libs/herocore-*.jar`.

### Run Tests

```bash
./gradlew test
```

HeroCore includes extensive unit tests for:
- Attribute calculation and modifier stacking
- Damage/heal formulas
- Status effect mechanics
- XP curves
- Serialization/deserialization

Test reports are generated in `build/reports/tests/test/index.html`.

### Development with IntelliJ IDEA

1. Open the project root in IntelliJ
2. Gradle should auto-import dependencies
3. Set JDK to Java 25 (Adoptium)
4. Run tests via the Gradle panel or `Run > Run 'All Tests'`

---

## Documentation

- **[Integration Guide](HEROES_INTEGRATION_GUIDE.md)** — Comprehensive guide for plugin developers integrating with HeroCore
- **[API Javadoc](src/main/java/)** — Inline documentation in source code
- **[Config Reference](src/main/resources/hero-core-defaults.json)** — Default configuration with all options

### Key API Packages

- `net.herotale.herocore.api.attribute` — Attribute enums, modifiers, calculator
- `net.herotale.herocore.api.component` — Core components (Stats, Resources, Effects)
- `net.herotale.herocore.api.event` — Event classes (Damage, Heal, Leveling)
- `net.herotale.herocore.api.statuseffect` — Status effect definitions
- `net.herotale.herocore.system.*` — Built-in systems (damage, heal, resistance)

### Attribute Reference

#### Primary Attributes
- **STRENGTH** — Physical damage, block strength, shield strength
- **DEXTERITY** — Crit chance, attack speed, dodge rating, fall damage reduction
- **INTELLIGENCE** — Spell power, mana pool, spell crit
- **FAITH** — Healing power, mana (secondary), mana regen, heal crit, buff strength
- **VITALITY** — Max health, health regen, armor %
- **RESOLVE** — Max health (secondary), CC resistance, debuff resistance, threat gen, stamina regen, magic resist

#### Secondary Attributes (Auto-Computed)
- MAX_HEALTH, MAX_MANA, MAX_STAMINA
- HEALTH_REGEN, MANA_REGEN, STAMINA_REGEN
- MOVE_SPEED, ATTACK_SPEED, MINING_SPEED
- CRIT_CHANCE, CRIT_DAMAGE_MULTIPLIER
- SPELL_POWER, SPELL_CRIT_CHANCE
- HEALING_POWER, HEAL_CRIT_CHANCE
- ARMOR, BLOCK_STRENGTH, DODGE_RATING, SHIELD_STRENGTH
- CC_RESISTANCE, DEBUFF_RESISTANCE, THREAT_GENERATION
- And more... (see `RPGAttribute` enum)

---

## Plugin Ecosystem

HeroCore is designed to support a rich plugin ecosystem:

- **Heroes RPG** — Class system, skills, talents
- **Guilds** — Guild bonuses, territory modifiers
- **Quests** — Reward XP, modify attributes temporarily
- **Economy** — Item modifiers, enchantments
- **PvP Combat** — Arena modifiers, combat logging
- **Dungeons** — Zone-based attribute modifiers

Each plugin adds its own components and systems while sharing HeroCore's standard attribute/combat layer.

---

## Contributing

Contributions are welcome! HeroCore is public domain software (Unlicense).

### Guidelines

1. **Code Style** — Follow existing patterns (records for data, services for logic)
2. **Tests** — Add unit tests for new systems or formulas
3. **Documentation** — Update integration guide for API changes
4. **Compatibility** — Maintain backward compatibility in minor versions

### Submitting Changes

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-system`)
3. Write tests for your changes
4. Run `./gradlew test` to ensure all tests pass
5. Submit a pull request with a clear description

---

## Roadmap

### Version 0.1.0 (Current Focus)
- [x] Core attribute system with derivations
- [x] Damage and heal systems
- [x] Status effects
- [x] Resource pools
- [x] Leveling and XP
- [ ] Mob profile system
- [ ] Zone modifier registry
- [ ] Harvest tier registry

### Version 0.2.0 (Planned)
- [ ] Advanced status effect conditions (stat thresholds, location-based)
- [ ] Skill cooldown tracking
- [ ] Combat log events for analytics
- [ ] Performance profiling tools

### Version 1.0.0 (Stable)
- [ ] API freeze and compatibility guarantee
- [ ] Migration guides for breaking changes
- [ ] Production-ready documentation
- [ ] Example plugin implementations

---

## Support

- **Discord**: [hc.to/herotale](https://hc.to/herotale)
- **Issues**: [GitHub Issues](https://github.com/herotale/herocore/issues)
- **Website**: [herotale.net](https://herotale.net/)

---


---

## Acknowledgments

- Built for the **Hytale** server platform by Hypixel Studios
- Inspired by traditional RPG stat systems (D&D, EverQuest, Diablo)
- Special thanks to the Hytale modding community for feedback during Early Access

---

**Made with ❤️ for the Hytale community**
