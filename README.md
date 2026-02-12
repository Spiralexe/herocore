# HeroCore

> **An ECS-native RPG attribute and combat system for Hytale servers**

[![Version](https://img.shields.io/badge/version-0.0.1-blue.svg)](manifest.json)
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
2. Place `herocore-0.0.1.jar` in your server's `mods/` directory
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
    compileOnly(files("../herocore/build/libs/herocore-0.0.1.jar"))
    // Or use a Maven coordinate once published
    // compileOnly("net.herotale:herocore:0.0.1")
}
```

Declare the dependency in your `manifest.json`:

```json
{
  "Dependencies": {
    "net.herotale:herocore": "0.0.1"
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

### Defense Implementation Guide

HeroCore Defense is a **dual-layer system**:

- **Vanilla Layer:** Hytale continues to calculate and display **Physical Defense only** in the tab menu.
- **HeroCore Layer:** Your RPG UI can display a detailed, percentage-based breakdown of defense and resistances.

This design preserves Hytale's native mechanics while exposing richer RPG stats in the Hero Attributes UI.

#### Step 1 — Configure Defense Derivation

Add a `defenseDerivation` block in `mods/herocore/config.json`. These values determine how primaries feed resistance.

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
    "maxResistancePercent": 0.75
  }
}
```

#### Step 2 — Internal Defense Naming Convention

Use these canonical categories in UI and logs:

- **PhysicalDefense** → `PHYSICAL_RESISTANCE_PERCENT`
- **MagicalDefense** → `MAGIC_RESIST`
- **ProjectileResistance** → `PROJECTILE_RESISTANCE_PERCENT`
- **ElementalResistance.Fire** → `FIRE_RESISTANCE_PERCENT` (fallback: `ELEMENTAL_RESIST_FIRE`)
- **ElementalResistance.Frost** → `ELEMENTAL_RESIST_ICE`
- **ElementalResistance.Lightning** → `ELEMENTAL_RESIST_LIGHTNING`
- **ElementalResistance.Poison** → `ELEMENTAL_RESIST_POISON`
- **ElementalResistance.Arcane** → `ELEMENTAL_RESIST_ARCANE`

These names are HeroCore-facing and do **not** alter vanilla Hytale behavior.

#### Step 3 — Apply the Defense Bridge (Vanilla Compatibility)

Whenever your entity's `StatsComponent` changes (level up, gear swap, buff applied), call `DefenseBridge.apply(stats)`:

```java
import net.herotale.herocore.impl.bridge.DefenseBridge;

DefenseBridge defenseBridge = new DefenseBridge(playerId);
defenseBridge.apply(stats);
```

This syncs resistance values into Hytale's native ECS stat system. Hytale's built-in damage pipeline still owns the final mitigation.

#### Step 4 — Display Percent Values in Hero Attributes UI

Use the `UIDataProvider` defense API to retrieve **percentage-based** values (0.0–1.0) and format them as %:

```java
import net.herotale.herocore.api.ui.DefenseCategory;

double phys = uiData.getDefensePercent(playerId, DefenseCategory.PHYSICAL_DEFENSE);
double magic = uiData.getDefensePercent(playerId, DefenseCategory.MAGICAL_DEFENSE);
double fire = uiData.getDefensePercent(playerId, DefenseCategory.ELEMENTAL_FIRE);

String physText = String.format("Physical Defense: %.0f%%", phys * 100.0);
String fireText = String.format("Fire Resistance: %.0f%%", fire * 100.0);
```

#### Step 5 — Level-Based Defense

Level bonuses should be **additive modifiers** applied to the resistance percent attributes:

```java
stats.addModifier(AttributeModifier.builder()
    .id("herocore:level_defense_phys_pct")
    .attribute(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT)
    .value(level * config.defenseDerivation().physicalResistancePerLevelPercent())
    .type(ModifierType.FLAT)
    .source(ModifierSource.of("herocore:level_defense"))
    .build());
```

#### Step 6 — Gear and Buffs

Gear and buffs can target the same resistance attributes. Because these are HeroCore modifiers, they stack naturally:

- **Flat resistance**: `PHYSICAL_RESISTANCE`, `PROJECTILE_RESISTANCE`, `FIRE_RESISTANCE`
- **Percent resistance**: `PHYSICAL_RESISTANCE_PERCENT`, `PROJECTILE_RESISTANCE_PERCENT`, `FIRE_RESISTANCE_PERCENT`

#### Step 7 — Armor (Dormant Hook)

The `ARMOR` attribute exists as a **future-proof hook**. It is not wired into mitigation yet, but is stored and modifier-ready. You can safely expose it in gear or UI now and integrate it later without breaking the architecture.

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

The output JAR will be in `build/libs/herocore-0.0.1.jar`.

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