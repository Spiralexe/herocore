package net.herotale.herocore.impl;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import net.herotale.herocore.api.HeroCore;
import net.herotale.herocore.impl.config.CoreConfig;
import net.herotale.herocore.impl.config.CoreConfigLoader;
import net.herotale.herocore.impl.system.AttributeDerivationSystem;
import net.herotale.herocore.impl.system.HeroCoreSetupSystem;
import net.herotale.herocore.system.damage.*;
import net.herotale.herocore.system.heal.*;

/**
 * Hytale plugin entry point for HeroCore.
 * <p>
 * <b>Architecture:</b> HeroCore is a <b>schema library and formula provider</b> that extends
 * Hytale's native ECS and stat systems, not a parallel system alongside them.
 * <p>
 * All damage/heal systems are registered as ECS {@code EntityEventSystem} instances
 * with correct ordering via {@code SystemDependency}. There is no custom event bus
 * or system orchestrator — Hytale's native system pipeline handles execution order.
 */
public class HeroCorePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HeroCorePlugin instance;

    private CoreConfig config;

    public HeroCorePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> preLoad() {
        config = CoreConfigLoader.load();
        return super.preLoad();
    }

    @Override
    protected void setup() {
        // 1. Register HeroCore component types
        HeroCoreComponentRegistry.registerComponents(getEntityStoreRegistry());

        // 2. Register HolderSystem — ensures HeroCoreStatsComponent on all entities
        getEntityStoreRegistry().registerSystem(new HeroCoreSetupSystem());

        // 3. Register AttributeDerivationSystem (tick system: primary → derived stats)
        getEntityStoreRegistry().registerSystem(new AttributeDerivationSystem());

        // 4. Register damage pipeline (EntityEventSystem instances, ordered via SystemDependency)
        getEntityStoreRegistry().registerSystem(new AttackDamageBonusSystem());
        getEntityStoreRegistry().registerSystem(new FallDamageReductionSystem());
        getEntityStoreRegistry().registerSystem(new ResistanceMitigationSystem(
                (float) config.damage().maxResistanceReduction()));
        getEntityStoreRegistry().registerSystem(new CriticalHitSystem(
                (float) config.damage().critDamageBaseMultiplier()));
        getEntityStoreRegistry().registerSystem(new LifestealSystem());
        getEntityStoreRegistry().registerSystem(new MinimumDamageSystem(
                (float) config.damage().minimumDamage()));

        // 5. Register heal pipeline (EntityEventSystem instances, ordered via SystemDependency)
        getEntityStoreRegistry().registerSystem(new HealingPowerScalingSystem(
                config.heal().healingPowerScalesRegenTick()));
        getEntityStoreRegistry().registerSystem(new HealingReceivedBonusSystem());
        getEntityStoreRegistry().registerSystem(new HealCritSystem());

        // 6. Initialize the public API facade
        HeroCore api = HeroCore.initialize();

        LOGGER.at(Level.INFO).log("HeroCore initialized — damage/heal pipelines and " +
                "AttributeDerivationSystem registered as ECS systems.");
    }

    @Override
    protected void start() {
        // Resolve and validate stat type indices after assets are loaded
        try {
            HeroCoreStatTypes.update();
            HeroCoreStatTypes.validate();
            LOGGER.at(Level.INFO).log("HeroCore stat types resolved and validated successfully.");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("HeroCore stat type validation failed. " +
                    "Some derived attributes may not function.");
        }
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("HeroCore shutting down cleanly.");
    }

    // ── Accessors ────────────────────────────────────────────────────

    public static HeroCorePlugin get() { return instance; }
    public CoreConfig getConfig() { return config; }
}
