package net.herotale.herocore.impl;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import net.herotale.herocore.api.HeroCore;
import net.herotale.herocore.api.event.HeroCoreReadyEvent;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.HealSystem;
import net.herotale.herocore.api.system.HeroCoreSystem;
import net.herotale.herocore.impl.config.CoreConfig;
import net.herotale.herocore.impl.config.CoreConfigLoader;
import net.herotale.herocore.system.damage.*;
import net.herotale.herocore.system.heal.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hytale plugin entry point for hero-core.
 * <p>
 * Thin shell — registers default ECS systems and loads configuration.
 * HeroCore is a <b>schema provider</b>, not a controller:
 * <ul>
 *   <li>Defines shared {@code Component} classes (StatsComponent, ResourcePoolComponent, etc.)</li>
 *   <li>Defines shared {@code Event} classes (DamageEvent, HealEvent, etc.)</li>
 *   <li>Provides default {@code System} implementations that can be individually disabled</li>
 * </ul>
 * <p>
 * Each system is an independent event handler
 * ordered via {@link net.herotale.herocore.api.system.SystemOrder @SystemOrder}.
 * The Hytale event bus dispatches events to systems in topological order.
 */
public class HeroCorePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HeroCorePlugin instance;

    private CoreConfig config;

    /** Registered default damage systems — individual event handlers with declared ordering. */
    private final List<DamageSystem> damageSystems = new ArrayList<>();

    /** Registered default heal systems — individual event handlers with declared ordering. */
    private final List<HealSystem> healSystems = new ArrayList<>();

    public HeroCorePlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // 1. Load configuration
        config = CoreConfigLoader.load();

        // 2. Register default damage systems (ordered via @SystemOrder, not priority numbers)
        registerDamageSystem(new AttackDamageBonusSystem());
        registerDamageSystem(new FallDamageReductionSystem());
        registerDamageSystem(new ResistanceMitigationSystem());
        registerDamageSystem(new CriticalHitSystem(config.damage().critDamageBaseMultiplier()));
        registerDamageSystem(new LifestealSystem(healEvent -> {
            // Route lifesteal heals to the entity event bus.
            // In production: targetEntity.postEvent(healEvent)
        }));
        registerDamageSystem(new MinimumDamageSystem(config.damage().minimumDamage()));

        // 3. Register default heal systems
        registerHealSystem(new HealingPowerScalingSystem(config.heal().healingPowerScalesRegenTick()));
        registerHealSystem(new HealingReceivedBonusSystem());
        registerHealSystem(new HealCritSystem(true));

        // 4. Apply per-system enable/disable from config
        applySystemConfig(config.systemOverrides());

        // 5. Initialize the public API facade
        HeroCore api = HeroCore.initialize();
        api.setSystemLookup(this::getSystem);
        api.setDamageSystemsSupplier(this::getDamageSystems);
        api.setHealSystemsSupplier(this::getHealSystems);

        // 6. Fire HeroCoreReadyEvent so downstream plugins know the API is live
        api.getEventBus().fire(new HeroCoreReadyEvent(api));

        LOGGER.atInfo().log("HeroCore enabled — %d damage systems, %d heal systems.",
                damageSystems.size(), healSystems.size());
    }

    protected void shutdown() {
        LOGGER.atInfo().log("HeroCore disabled cleanly.");
    }

    // ── System registration ──────────────────────────────────────────

    /**
     * Register a damage system. Each system is an independent event handler;
     * execution order is declared via {@code @SystemOrder}, not orchestrated by a central dispatcher.
     */
    public void registerDamageSystem(DamageSystem system) {
        damageSystems.add(system);
    }

    /**
     * Register a heal system.
     */
    public void registerHealSystem(HealSystem system) {
        healSystems.add(system);
    }

    // ── Accessors ────────────────────────────────────────────────────

    public static HeroCorePlugin get() { return instance; }
    public CoreConfig getConfig() { return config; }

    /** All registered damage systems (unmodifiable). */
    public List<DamageSystem> getDamageSystems() { return Collections.unmodifiableList(damageSystems); }

    /** All registered heal systems (unmodifiable). */
    public List<HealSystem> getHealSystems() { return Collections.unmodifiableList(healSystems); }

    /**
     * Look up any registered system by ID (damage or heal).
     * Returns {@code null} if not found.
     */
    public HeroCoreSystem getSystem(String systemId) {
        for (DamageSystem s : damageSystems) { if (s.getId().equals(systemId)) return s; }
        for (HealSystem s : healSystems)     { if (s.getId().equals(systemId)) return s; }
        return null;
    }

    // ── Internals ────────────────────────────────────────────────────

    private void applySystemConfig(Map<String, Boolean> overrides) {
        if (overrides == null || overrides.isEmpty()) return;
        for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
            HeroCoreSystem system = getSystem(entry.getKey());
            if (system != null) {
                system.setEnabled(entry.getValue());
                if (!entry.getValue()) {
                    LOGGER.atInfo().log("System '%s' disabled by configuration.", entry.getKey());
                }
            }
        }
    }
}
