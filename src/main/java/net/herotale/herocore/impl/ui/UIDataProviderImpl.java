package net.herotale.herocore.impl.ui;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.component.ResourcePoolComponent;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.component.StatusEffectsComponent;
import net.herotale.herocore.api.leveling.LevelingProfile;
import net.herotale.herocore.api.leveling.LevelingRegistry;
import net.herotale.herocore.api.resource.ResourceType;
import net.herotale.herocore.api.status.StatusEffect;
import net.herotale.herocore.api.ui.DefenseCategory;
import net.herotale.herocore.api.ui.UIDataProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementation of UIDataProvider.
 * <p>
 * Pulls data from per-entity ECS components rather than global registries.
 * The provider functions are injected by whatever wiring layer creates this object.
 */
public class UIDataProviderImpl implements UIDataProvider {

    private final Function<UUID, StatsComponent> statsProvider;
    private final Function<UUID, ResourcePoolComponent> resourceProvider;
    private final Function<UUID, CombatStateComponent> combatProvider;
    private final Function<UUID, StatusEffectsComponent> statusProvider;
    private final LevelingRegistry levelingRegistry;

    public UIDataProviderImpl(
            Function<UUID, StatsComponent> statsProvider,
            Function<UUID, ResourcePoolComponent> resourceProvider,
            Function<UUID, CombatStateComponent> combatProvider,
            Function<UUID, StatusEffectsComponent> statusProvider,
            LevelingRegistry levelingRegistry) {
        this.statsProvider = statsProvider;
        this.resourceProvider = resourceProvider;
        this.combatProvider = combatProvider;
        this.statusProvider = statusProvider;
        this.levelingRegistry = levelingRegistry;
    }

    @Override
    public double getResourceCurrent(UUID entityUUID, ResourceType type) {
        ResourcePoolComponent pool = resourceProvider.apply(entityUUID);
        return pool != null ? pool.get(type) : 0.0;
    }

    @Override
    public double getResourceMax(UUID entityUUID, ResourceType type) {
        StatsComponent stats = statsProvider.apply(entityUUID);
        if (stats == null) return 0.0;
        return switch (type) {
            case HEALTH -> stats.getValue(RPGAttribute.MAX_HEALTH);
            case MANA -> stats.getValue(RPGAttribute.MAX_MANA);
            case STAMINA -> stats.getValue(RPGAttribute.MAX_STAMINA);
            case OXYGEN -> stats.getValue(RPGAttribute.MAX_OXYGEN);
            case AMMO -> stats.getValue(RPGAttribute.MAX_AMMO);
        };
    }

    @Override
    public double getResourcePercent(UUID entityUUID, ResourceType type) {
        double max = getResourceMax(entityUUID, type);
        if (max <= 0.0) return 0.0;
        double current = getResourceCurrent(entityUUID, type);
        return Math.max(0.0, Math.min(1.0, current / max));
    }

    @Override
    public boolean isResourceRegenDelayed(UUID entityUUID, ResourceType type) {
        if (type != ResourceType.STAMINA) return false;
        CombatStateComponent combat = combatProvider.apply(entityUUID);
        return combat != null && combat.isStaminaRegenDelayed();
    }

    @Override
    public double getAttributeValue(UUID entityUUID, RPGAttribute attribute) {
        StatsComponent stats = statsProvider.apply(entityUUID);
        return stats != null ? stats.getValue(attribute) : 0.0;
    }

    @Override
    public double getDefensePercent(UUID entityUUID, DefenseCategory category) {
        StatsComponent stats = statsProvider.apply(entityUUID);
        if (stats == null) return 0.0;

        double value = switch (category) {
            case PHYSICAL_DEFENSE -> stats.getValue(RPGAttribute.PHYSICAL_RESISTANCE_PERCENT);
            case MAGICAL_DEFENSE -> stats.getValue(RPGAttribute.MAGIC_RESIST);
            case PROJECTILE_RESISTANCE -> stats.getValue(RPGAttribute.PROJECTILE_RESISTANCE_PERCENT);
            case ELEMENTAL_FIRE -> preferPercent(stats.getValue(RPGAttribute.FIRE_RESISTANCE_PERCENT),
                    stats.getValue(RPGAttribute.ELEMENTAL_RESIST_FIRE));
            case ELEMENTAL_FROST -> stats.getValue(RPGAttribute.ELEMENTAL_RESIST_ICE);
            case ELEMENTAL_LIGHTNING -> stats.getValue(RPGAttribute.ELEMENTAL_RESIST_LIGHTNING);
            case ELEMENTAL_POISON -> stats.getValue(RPGAttribute.ELEMENTAL_RESIST_POISON);
            case ELEMENTAL_ARCANE -> stats.getValue(RPGAttribute.ELEMENTAL_RESIST_ARCANE);
        };

        return normalizePercent(value);
    }

    @Override
    public int getLevel(UUID entityUUID, String profileId) {
        return levelingRegistry.getLevel(entityUUID, profileId);
    }

    @Override
    public long getXP(UUID entityUUID, String profileId) {
        return levelingRegistry.getXP(entityUUID, profileId);
    }

    @Override
    public long getXPToNextLevel(UUID entityUUID, String profileId) {
        LevelingProfile profile = levelingRegistry.getProfile(profileId);
        if (profile == null) return 0;
        int level = levelingRegistry.getLevel(entityUUID, profileId);
        int nextLevel = Math.min(level + 1, profile.getMaxLevel());
        long currentThreshold = profile.getXpCurve().getThreshold(level);
        long nextThreshold = profile.getXpCurve().getThreshold(nextLevel);
        long currentXp = levelingRegistry.getXP(entityUUID, profileId);
        if (nextThreshold <= currentThreshold) return 0;
        return Math.max(0, nextThreshold - currentXp);
    }

    @Override
    public double getXPPercent(UUID entityUUID, String profileId) {
        LevelingProfile profile = levelingRegistry.getProfile(profileId);
        if (profile == null) return 0.0;
        int level = levelingRegistry.getLevel(entityUUID, profileId);
        if (level >= profile.getMaxLevel()) return 1.0;
        long currentThreshold = profile.getXpCurve().getThreshold(level);
        long nextThreshold = profile.getXpCurve().getThreshold(level + 1);
        long currentXp = levelingRegistry.getXP(entityUUID, profileId);
        if (nextThreshold <= currentThreshold) return 0.0;
        return Math.max(0.0, Math.min(1.0,
                (double) (currentXp - currentThreshold) / (double) (nextThreshold - currentThreshold)));
    }

    @Override
    public Collection<StatusEffect> getActiveStatusEffects(UUID entityUUID) {
        StatusEffectsComponent status = statusProvider.apply(entityUUID);
        return status != null ? status.getActiveEffects() : Collections.emptyList();
    }

    @Override
    public boolean isInCombat(UUID entityUUID) {
        CombatStateComponent combat = combatProvider.apply(entityUUID);
        return combat != null && combat.isInCombat();
    }

    @Override
    public long getCombatTimeoutRemaining(UUID entityUUID) {
        CombatStateComponent combat = combatProvider.apply(entityUUID);
        return combat != null ? combat.getCombatTimeoutRemaining() : 0;
    }

    private static double preferPercent(double primary, double fallback) {
        return primary > 0.0 ? primary : fallback;
    }

    private static double normalizePercent(double value) {
        if (value <= 0.0) return 0.0;
        double normalized = value > 1.0 ? value / 100.0 : value;
        return Math.max(0.0, Math.min(1.0, normalized));
    }
}
