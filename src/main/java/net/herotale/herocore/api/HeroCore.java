package net.herotale.herocore.api;

import net.herotale.herocore.api.entity.MobRegistry;
import net.herotale.herocore.api.factions.FactionRegistry;
import net.herotale.herocore.api.factions.FactionRules;
import net.herotale.herocore.api.factions.api.FactionAPI;
import net.herotale.herocore.api.harvest.HarvestTierRegistry;
import net.herotale.herocore.api.leveling.LevelingRegistry;
import net.herotale.herocore.api.zone.ZoneModifierRegistry;
import net.herotale.herocore.api.language.LanguageService;
import net.herotale.herocore.api.language.TrainingRegistry;

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
    private LanguageService languageService;
    private TrainingRegistry trainingRegistry;

    // ── Faction API (set by the faction-providing plugin, e.g. Heroes) ──
    private FactionAPI factionAPI;
    private FactionRegistry factionRegistry;
    private FactionRules factionRules;

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

    /** Language system for faction/race languages and proficiency. */
    public LanguageService getLanguageService() {
        return languageService;
    }

    /** Training registry for language proficiency advancement. */
    public TrainingRegistry getTrainingRegistry() {
        return trainingRegistry;
    }

    // ── Faction accessors ────────────────────────────────────────────

    /**
     * Faction query API — check player factions, alliance status, interaction rules.
     * Provided by the faction-implementing plugin (e.g. Heroes).
     *
     * @return the FactionAPI, or {@code null} if no faction provider is installed
     */
    public FactionAPI getFactionAPI() {
        return factionAPI;
    }

    /** Registry of all faction definitions. */
    public FactionRegistry getFactionRegistry() {
        return factionRegistry;
    }

    /** Global faction selection/switching rules. */
    public FactionRules getFactionRules() {
        return factionRules;
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

    /** @hidden */
    public void setLanguageService(LanguageService service) {
        this.languageService = service;
    }

    /** @hidden */
    public void setTrainingRegistry(TrainingRegistry registry) {
        this.trainingRegistry = registry;
    }

    /** @hidden Called by the faction-providing plugin during its setup(). */
    public void setFactionAPI(FactionAPI api) {
        this.factionAPI = api;
    }

    /** @hidden */
    public void setFactionRegistry(FactionRegistry registry) {
        this.factionRegistry = registry;
    }

    /** @hidden */
    public void setFactionRules(FactionRules rules) {
        this.factionRules = rules;
    }
}
