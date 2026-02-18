package net.herotale.herocore.api;

import net.herotale.herocore.api.entity.MobRegistry;
import net.herotale.herocore.api.harvest.HarvestTierRegistry;
import net.herotale.herocore.api.leveling.LevelingRegistry;
import net.herotale.herocore.api.zone.ZoneModifierRegistry;

/**
 * Public API facade for HeroCore.
 * <p>
 * Downstream plugins use {@code HeroCore.get()} to access registries.
 * HeroCore is a <b>schema provider</b> — it defines RPG attribute taxonomy,
 * derivation formulas, and damage/heal pipeline math.
 * <p>
 * All stat storage, modifier stacking, and resource regeneration are handled
 * natively by Hytale's {@code EntityStatMap} and {@code RegeneratingValue}.
 * There is no parallel stat container or custom event bus.
 * <p>
 * <b>Usage in downstream plugins:</b>
 * <pre>{@code
 * HeroCore core = HeroCore.get();
 * LevelingRegistry leveling = core.getLevelingRegistry();
 * }</pre>
 */
public final class HeroCore {

    private static HeroCore instance;

    // ── Registries (set during HeroCorePlugin.setup) ─────────────────
    private LevelingRegistry levelingRegistry;
    private MobRegistry mobRegistry;
    private ZoneModifierRegistry zoneModifierRegistry;
    private HarvestTierRegistry harvestTierRegistry;

    private HeroCore() {}

    // ── Singleton ────────────────────────────────────────────────────

    /**
     * Get the HeroCore API instance.
     *
     * @return the singleton, never null after HeroCore plugin finishes {@code setup()}
     * @throws IllegalStateException if HeroCore has not initialized yet
     */
    public static HeroCore get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "HeroCore is not initialized yet. "
                    + "Ensure your plugin declares HeroCore as a dependency in manifest.json.");
        }
        return instance;
    }

    /**
     * @return true if HeroCore has finished initialization and {@link #get()} is safe to call.
     */
    public static boolean isReady() {
        return instance != null;
    }

    /** Internal — called by HeroCorePlugin during setup. Do not call from downstream plugins. */
    public static HeroCore initialize() {
        instance = new HeroCore();
        return instance;
    }

    // ── Registry accessors ───────────────────────────────────────────

    /** Leveling profiles and per-player XP/level tracking. */
    public LevelingRegistry getLevelingRegistry() {
        return levelingRegistry;
    }

    /** Mob/NPC profiles and scaling profiles. */
    public MobRegistry getMobRegistry() {
        return mobRegistry;
    }

    /** Zone-scoped attribute modifiers. */
    public ZoneModifierRegistry getZoneModifierRegistry() {
        return zoneModifierRegistry;
    }

    /** Harvest tier requirements. */
    public HarvestTierRegistry getHarvestTierRegistry() {
        return harvestTierRegistry;
    }

    // ── Internal wiring (called by HeroCorePlugin) ───────────────────

    /** @hidden */
    public void setLevelingRegistry(LevelingRegistry registry) {
        this.levelingRegistry = registry;
    }

    /** @hidden */
    public void setMobRegistry(MobRegistry registry) {
        this.mobRegistry = registry;
    }

    /** @hidden */
    public void setZoneModifierRegistry(ZoneModifierRegistry registry) {
        this.zoneModifierRegistry = registry;
    }

    /** @hidden */
    public void setHarvestTierRegistry(HarvestTierRegistry registry) {
        this.harvestTierRegistry = registry;
    }
}
