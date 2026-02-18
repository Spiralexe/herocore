package net.herotale.herocore.impl;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.HeroCoreStatsComponent;

/**
 * Central registry for HeroCore ECS components and their serialization codecs.
 *
 * Follows the pattern from EntityStatsModule and other core modules:
 * - Register components and codecs once during plugin setup()
 * - ComponentType is returned by registerComponent() — not created via static factory
 * - Uses BuilderCodec builder pattern with KeyedCodec for each field
 */
public class HeroCoreComponentRegistry {

    /**
     * ComponentType handle for HeroCoreStatsComponent.
     * Set during {@link #registerComponents} — null before plugin setup().
     */
    public static ComponentType<EntityStore, HeroCoreStatsComponent> HERO_CORE_STATS;

    /**
     * BuilderCodec for HeroCoreStatsComponent persistence.
     * KeyedCodec keys must start with uppercase (Hytale convention).
     */
    public static final BuilderCodec<HeroCoreStatsComponent> CODEC =
            BuilderCodec.builder(HeroCoreStatsComponent.class, HeroCoreStatsComponent::new)
                    .append(new KeyedCodec<>("Strength", Codec.DOUBLE),
                            (c, v) -> c.setStrength(v), HeroCoreStatsComponent::getStrength).add()
                    .append(new KeyedCodec<>("Dexterity", Codec.DOUBLE),
                            (c, v) -> c.setDexterity(v), HeroCoreStatsComponent::getDexterity).add()
                    .append(new KeyedCodec<>("Intelligence", Codec.DOUBLE),
                            (c, v) -> c.setIntelligence(v), HeroCoreStatsComponent::getIntelligence).add()
                    .append(new KeyedCodec<>("Faith", Codec.DOUBLE),
                            (c, v) -> c.setFaith(v), HeroCoreStatsComponent::getFaith).add()
                    .append(new KeyedCodec<>("Vitality", Codec.DOUBLE),
                            (c, v) -> c.setVitality(v), HeroCoreStatsComponent::getVitality).add()
                    .append(new KeyedCodec<>("Resolve", Codec.DOUBLE),
                            (c, v) -> c.setResolve(v), HeroCoreStatsComponent::getResolve).add()
                    .build();

    /**
     * Register all HeroCore components with the entity store.
     * Called during plugin setup(). The returned ComponentType is stored for later use.
     */
    public static void registerComponents(ComponentRegistryProxy<EntityStore> registry) {
        HERO_CORE_STATS = registry.registerComponent(
                HeroCoreStatsComponent.class,
                "herocore:stats",
                CODEC
        );
    }

    private HeroCoreComponentRegistry() {}
}
