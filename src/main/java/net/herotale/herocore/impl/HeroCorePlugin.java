package net.herotale.herocore.impl;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import net.herotale.herocore.api.HeroCore;
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.api.event.CombatExitEvent;
import net.herotale.herocore.api.event.LevelUpEvent;
import net.herotale.herocore.api.heal.HeroCoreHealEvent;
import net.herotale.herocore.impl.config.CoreConfig;
import net.herotale.herocore.impl.config.CoreConfigLoader;
import net.herotale.herocore.impl.system.AttributeDerivationSystem;
import net.herotale.herocore.impl.system.HeroCoreSetupSystem;
import net.herotale.herocore.system.combat.CombatTimeoutSystem;
import net.herotale.herocore.system.combat.StatusEffectTickSystem;
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
        var reg = getEntityStoreRegistry();

        // 1. Register HeroCore component types (persistent + non-persistent)
        HeroCoreComponentRegistry.registerComponents(reg);

        // 2. Register ECS event types
        reg.registerEntityEventType(HeroCoreDamageEvent.class);
        reg.registerEntityEventType(HeroCoreHealEvent.class);
        reg.registerEntityEventType(LevelUpEvent.class);
        reg.registerEntityEventType(CombatExitEvent.class);

        // 3. Register HolderSystem — ensures HeroCoreStatsComponent on all entities
        reg.registerSystem(new HeroCoreSetupSystem());

        // 4. Register AttributeDerivationSystem (tick system: primary → derived stats)
        reg.registerSystem(new AttributeDerivationSystem());

        // 5. Register damage pipeline (EntityEventSystem instances, ordered via SystemDependency)
        reg.registerSystem(new AttackDamageBonusSystem());
        reg.registerSystem(new FallDamageReductionSystem());
        reg.registerSystem(new ResistanceMitigationSystem(
                (float) config.damage().maxResistanceReduction()));
        reg.registerSystem(new CriticalHitSystem(
                (float) config.damage().critDamageBaseMultiplier()));
        reg.registerSystem(new LifestealSystem());
        reg.registerSystem(new MinimumDamageSystem(
                (float) config.damage().minimumDamage()));
        reg.registerSystem(new DamageApplicationSystem());

        // 6. Register heal pipeline (EntityEventSystem instances, ordered via SystemDependency)
        reg.registerSystem(new HealingPowerScalingSystem(
                config.heal().healingPowerScalesRegenTick()));
        reg.registerSystem(new HealingReceivedBonusSystem());
        reg.registerSystem(new HealCritSystem());

        // 7. Register tick-based systems (DelayedSystem)
        reg.registerSystem(new CombatTimeoutSystem(
                config.resourceRegen().combatTimeoutMs() / 1000f));
        reg.registerSystem(new StatusEffectTickSystem());

        // 8. Initialize the public API facade
        HeroCore api = HeroCore.initialize();

        LOGGER.at(Level.INFO).log("HeroCore initialized — damage/heal pipelines, " +
                "combat timeout, status effects, and AttributeDerivationSystem registered.");
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
