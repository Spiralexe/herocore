package net.herotale.herocore.api.ui;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.resource.ResourceType;
import net.herotale.herocore.api.status.StatusEffect;

import java.util.Collection;
import java.util.UUID;

/**
 * Pull-based API for UI systems to query core data.
 * <p>
 * HeroCore provides an implementation of this interface via {@link net.herotale.herocore.impl.ui.UIDataProviderImpl}.
 * The heroes-rpg plugin (or any other UI plugin) calls these methods to build HUD displays,
 * status panels, character sheets, etc.
 * <p>
 * This design keeps HeroCore free of UI concerns while providing a clean contract for UI plugins
 * to pull the data they need.
 * <p>
 * Example usage in heroes-rpg:
 * <pre>{@code
 * UIDataProvider uiData = ...; // injected or constructed with component providers
 *
 * double currentHealth = uiData.getResourceCurrent(playerUUID, ResourceType.HEALTH);
 * double maxHealth = uiData.getResourceMax(playerUUID, ResourceType.HEALTH);
 * double xpPercent = uiData.getXPPercent(playerUUID, "heroes:class_level");
 * Collection<StatusEffect> effects = uiData.getActiveStatusEffects(playerUUID);
 * }</pre>
 */
public interface UIDataProvider {

    /**
     * Get the current resource value.
     */
    double getResourceCurrent(UUID entityUUID, ResourceType type);

    /**
     * Get the max resource value.
     */
    double getResourceMax(UUID entityUUID, ResourceType type);

    /**
     * Get resource percent (0..1). Returns 0 if max is 0.
     */
    double getResourcePercent(UUID entityUUID, ResourceType type);

    /**
     * True when regen for the resource is delayed (stamina).
     */
    boolean isResourceRegenDelayed(UUID entityUUID, ResourceType type);

    /**
     * Get a computed attribute value for an entity.
     */
    double getAttributeValue(UUID entityUUID, RPGAttribute attribute);

    /**
     * Get a defense/resistance percentage (0..1) for UI display.
     * <p>
     * This is a HeroCore-only view and does not alter vanilla Hytale behavior.
     */
    double getDefensePercent(UUID entityUUID, DefenseCategory category);

    /**
     * Get the current level for a leveling profile.
     */
    int getLevel(UUID entityUUID, String profileId);

    /**
     * Get the current cumulative XP for a leveling profile.
     */
    long getXP(UUID entityUUID, String profileId);

    /**
     * Get XP required to reach the next level.
     */
    long getXPToNextLevel(UUID entityUUID, String profileId);

    /**
     * Get percent progress to next level (0..1). Returns 1 if maxed.
     */
    double getXPPercent(UUID entityUUID, String profileId);

    /**
     * Retrieves all active status effects on an entity.
     *
     * @param entityUUID The entity's UUID
     * @return Collection of active status effects (may be empty)
     */
    Collection<StatusEffect> getActiveStatusEffects(UUID entityUUID);

    /**
     * True if the entity is considered in combat.
     */
    boolean isInCombat(UUID entityUUID);

    /**
     * Remaining combat timeout in milliseconds (0 if out of combat).
     */
    long getCombatTimeoutRemaining(UUID entityUUID);
}
