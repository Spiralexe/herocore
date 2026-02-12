package net.herotale.herocore.api.entity;

import net.herotale.herocore.api.attribute.RPGAttribute;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a single mob or NPC profile registered in the system.
 * Each profile has a unique ID, category, base attributes, and an optional scaling profile.
 * <p>
 * When a mob spawns, the system:
 * 1. Creates a StatsComponent from baseAttributes
 * 2. If scalingProfile is present, applies per-level modifiers
 * 3. If scalingProfile is present and a player is nearby, applies tier modifiers
 */
public interface MobProfile {

    /**
     * @return The unique ID for this mob (e.g., "zombie", "skeleton_archer", "goblin_shaman")
     */
    String getId();

    /**
     * @return The qualitative category (NORMAL, ELITE, BOSS, etc.)
     */
    MobCategory getCategory();

    /**
     * @return The base level at which this profile was authored.
     */
    int getBaseLevel();

    /**
     * @return Base attributes for this mob profile.
     */
    Map<RPGAttribute, Double> getBaseAttributes();

    /**
     * @return The scaling profile that defines how this mob grows with level
     *         and reacts to player level gaps. Empty if this mob does not scale.
     */
    Optional<MobScalingProfile> getScalingProfile();
}
