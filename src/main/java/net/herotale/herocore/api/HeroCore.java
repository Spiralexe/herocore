package net.herotale.herocore.api;

import net.herotale.herocore.api.attribute.AttributeMap;
import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.component.ResourcePoolComponent;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.component.StatusEffectsComponent;
import net.herotale.herocore.api.entity.MobRegistry;
import net.herotale.herocore.api.event.HeroCoreEventBus;
import net.herotale.herocore.api.harvest.HarvestTierRegistry;
import net.herotale.herocore.api.leveling.LevelingRegistry;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.HealSystem;
import net.herotale.herocore.api.system.HeroCoreSystem;
import net.herotale.herocore.api.ui.UIDataProvider;
import net.herotale.herocore.api.zone.ZoneModifierRegistry;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Public API facade for HeroCore.
 * <p>
 * Downstream plugins use {@code HeroCore.get()} to access registries, component
 * providers, and the event bus. HeroCore remains a <b>schema provider</b> —
 * this facade simply gathers the pieces under one roof so callers don't need to
 * import internal implementation types.
 * <p>
 * HeroCore does NOT own the entity lifecycle. Your plugin creates and stores
 * components. Register component providers via {@link #setStatsProvider},
 * {@link #setResourceProvider}, etc. so that HeroCore systems, bridges, and the
 * UI data provider can find components by UUID.
 * <p>
 * <b>Setup example (in your plugin's {@code setup()}):</b>
 * <pre>{@code
 * HeroCore core = HeroCore.get();
 *
 * // Register providers for your component stores
 * core.setStatsProvider(uuid -> myStatsMap.get(uuid));
 * core.setResourceProvider(uuid -> myResourceMap.get(uuid));
 * core.setCombatProvider(uuid -> myCombatMap.get(uuid));
 * core.setStatusProvider(uuid -> myStatusMap.get(uuid));
 *
 * // Access registries
 * LevelingRegistry leveling = core.getLevelingRegistry();
 *
 * // Subscribe to events
 * core.getEventBus().subscribe(LevelUpEvent.class, event -> {
 *     // handle level up
 * });
 * }</pre>
 */
public final class HeroCore {

    private static HeroCore instance;

    // ── Registries (set during HeroCorePlugin.setup) ─────────────────
    private LevelingRegistry levelingRegistry;
    private MobRegistry mobRegistry;
    private ZoneModifierRegistry zoneModifierRegistry;
    private HarvestTierRegistry harvestTierRegistry;
    private UIDataProvider uiDataProvider;

    // ── Component providers (set by downstream plugins) ──────────────
    private Function<UUID, StatsComponent> statsProvider;
    private Function<UUID, ResourcePoolComponent> resourceProvider;
    private Function<UUID, CombatStateComponent> combatProvider;
    private Function<UUID, StatusEffectsComponent> statusProvider;

    // ── Event bus ────────────────────────────────────────────────────
    private final HeroCoreEventBus eventBus = new HeroCoreEventBus();

    // ── Systems access (delegated from HeroCorePlugin) ───────────────
    private Function<String, HeroCoreSystem> systemLookup;
    private java.util.function.Supplier<List<DamageSystem>> damageSystemsSupplier;
    private java.util.function.Supplier<List<HealSystem>> healSystemsSupplier;

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

    /** Read-only data facade for HUD / UI plugins. */
    public UIDataProvider getUIDataProvider() {
        return uiDataProvider;
    }

    // ── Event bus ────────────────────────────────────────────────────
    public HeroCoreEventBus getEventBus() {
        return eventBus;
    }

    // ── Component providers ──────────────────────────────────────────

    /**
     * Look up a {@link StatsComponent} for an entity.
     *
     * @param entityUuid the entity UUID
     * @return the stats component, or null if not registered
     */
    public StatsComponent getStats(UUID entityUuid) {
        return statsProvider != null ? statsProvider.apply(entityUuid) : null;
    }

    /**
     * Look up the {@link AttributeMap} for an entity.
     * <p>
     * This is a convenience alias for {@link #getStats(UUID)} — the returned
     * {@link StatsComponent} implements {@link AttributeMap}.
     *
     * @param entityUuid the entity UUID
     * @return the attribute map (a {@link StatsComponent}), or null if not registered
     */
    public AttributeMap getAttributeMapFor(UUID entityUuid) {
        return getStats(entityUuid);
    }

    /**
     * Look up a {@link ResourcePoolComponent} for an entity.
     *
     * @param entityUuid the entity UUID
     * @return the resource pool, or null if not registered
     */
    public ResourcePoolComponent getResources(UUID entityUuid) {
        return resourceProvider != null ? resourceProvider.apply(entityUuid) : null;
    }

    /**
     * Look up a {@link CombatStateComponent} for an entity.
     *
     * @param entityUuid the entity UUID
     * @return the combat state, or null if not registered
     */
    public CombatStateComponent getCombat(UUID entityUuid) {
        return combatProvider != null ? combatProvider.apply(entityUuid) : null;
    }

    /**
     * Look up a {@link StatusEffectsComponent} for an entity.
     *
     * @param entityUuid the entity UUID
     * @return the status effects, or null if not registered
     */
    public StatusEffectsComponent getStatus(UUID entityUuid) {
        return statusProvider != null ? statusProvider.apply(entityUuid) : null;
    }

    // ── Component provider registration ──────────────────────────────

    /**
     * Register the function that resolves {@link StatsComponent} per entity.
     * Typically backed by your plugin's {@code ConcurrentHashMap<UUID, StatsComponent>}.
     */
    public void setStatsProvider(Function<UUID, StatsComponent> provider) {
        this.statsProvider = provider;
    }

    /**
     * Register the function that resolves {@link ResourcePoolComponent} per entity.
     */
    public void setResourceProvider(Function<UUID, ResourcePoolComponent> provider) {
        this.resourceProvider = provider;
    }

    /**
     * Register the function that resolves {@link CombatStateComponent} per entity.
     */
    public void setCombatProvider(Function<UUID, CombatStateComponent> provider) {
        this.combatProvider = provider;
    }

    /**
     * Register the function that resolves {@link StatusEffectsComponent} per entity.
     */
    public void setStatusProvider(Function<UUID, StatusEffectsComponent> provider) {
        this.statusProvider = provider;
    }

    /** Get the stats provider function (for wiring into registries/services). */
    public Function<UUID, StatsComponent> getStatsProvider() {
        return statsProvider;
    }

    /** Get the resource provider function. */
    public Function<UUID, ResourcePoolComponent> getResourceProvider() {
        return resourceProvider;
    }

    /** Get the combat provider function. */
    public Function<UUID, CombatStateComponent> getCombatProvider() {
        return combatProvider;
    }

    /** Get the status provider function. */
    public Function<UUID, StatusEffectsComponent> getStatusProvider() {
        return statusProvider;
    }

    // ── Systems access ───────────────────────────────────────────────

    /**
     * Look up a registered system by ID (damage or heal).
     *
     * @param systemId the system ID (e.g., {@code "herocore:critical_hit"})
     * @return the system, or null if not found
     */
    public HeroCoreSystem getSystem(String systemId) {
        return systemLookup != null ? systemLookup.apply(systemId) : null;
    }

    /** All registered damage systems (unmodifiable). */
    public List<DamageSystem> getDamageSystems() {
        return damageSystemsSupplier != null ? damageSystemsSupplier.get() : List.of();
    }

    /** All registered heal systems (unmodifiable). */
    public List<HealSystem> getHealSystems() {
        return healSystemsSupplier != null ? healSystemsSupplier.get() : List.of();
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
    public void setUIDataProvider(UIDataProvider provider) {
        this.uiDataProvider = provider;
    }

    /** @hidden */
    public void setSystemLookup(Function<String, HeroCoreSystem> lookup) {
        this.systemLookup = lookup;
    }

    /** @hidden */
    public void setDamageSystemsSupplier(java.util.function.Supplier<List<DamageSystem>> supplier) {
        this.damageSystemsSupplier = supplier;
    }

    /** @hidden */
    public void setHealSystemsSupplier(java.util.function.Supplier<List<HealSystem>> supplier) {
        this.healSystemsSupplier = supplier;
    }
}
