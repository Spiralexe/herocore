package net.herotale.herocore.impl;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.component.HeroCoreProgressionComponent;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.api.component.StatusEffectIndexComponent;

/**
 * Central registry for HeroCore ECS components.
 * <p>
 * Follows the pattern from EntityStatsModule and other core modules:
 * - Register components once during plugin setup()
 * - ComponentType is returned by registerComponent()
 * - Uses BuilderCodec for persistent components; Supplier for non-persistent
 */
public class HeroCoreComponentRegistry {

    /**
     * ComponentType handle for HeroCoreStatsComponent (persistent).
     * Set during {@link #registerComponents}.
     */
    public static ComponentType<EntityStore, HeroCoreStatsComponent> HERO_CORE_STATS;

    /**
     * ComponentType handle for HeroCoreProgressionComponent (persistent).
     * Set during {@link #registerComponents}.
     */
    public static ComponentType<EntityStore, HeroCoreProgressionComponent> HERO_CORE_PROGRESSION;

    /**
     * ComponentType handle for CombatStateComponent (non-persistent).
     * Set during {@link #registerComponents}.
     */
    public static ComponentType<EntityStore, CombatStateComponent> COMBAT_STATE;

    /**
     * ComponentType handle for StatusEffectIndexComponent (non-persistent).
     * Set during {@link #registerComponents}.
     */
    public static ComponentType<EntityStore, StatusEffectIndexComponent> STATUS_EFFECT_INDEX;

    /**
     * Register all HeroCore components with the entity store.
     * Called during plugin setup(). The returned ComponentType is stored for later use.
     */
    public static void registerComponents(ComponentRegistryProxy<EntityStore> registry) {
        // Persistent components (with BuilderCodec)
        HERO_CORE_STATS = registry.registerComponent(
                HeroCoreStatsComponent.class,
                "herocore:stats",
                HeroCoreStatsComponent.CODEC
        );
        HeroCoreStatsComponent.setComponentType(HERO_CORE_STATS);

        HERO_CORE_PROGRESSION = registry.registerComponent(
                HeroCoreProgressionComponent.class,
                "herocore:progression",
                HeroCoreProgressionComponent.CODEC
        );
        HeroCoreProgressionComponent.setComponentType(HERO_CORE_PROGRESSION);

        // Non-persistent components (with Supplier, no codec)
        COMBAT_STATE = registry.registerComponent(
                CombatStateComponent.class,
                CombatStateComponent::new
        );
        CombatStateComponent.setComponentType(COMBAT_STATE);

        STATUS_EFFECT_INDEX = registry.registerComponent(
                StatusEffectIndexComponent.class,
                StatusEffectIndexComponent::new
        );
        StatusEffectIndexComponent.setComponentType(STATUS_EFFECT_INDEX);
    }

    private HeroCoreComponentRegistry() {}
}
