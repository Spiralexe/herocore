package net.herotale.herocore.api.leveling;

import java.util.UUID;

/**
 * Registry of named leveling profiles. Any plugin can register a profile
 * and grant XP to it. The {@code XP_GAIN_MULTIPLIER} attribute is honored
 * globally across all profiles.
 */
public interface LevelingRegistry {

    /**
     * Register a leveling profile. Idempotent — re-registration with the same ID overwrites.
     *
     * @param profile the profile to register
     */
    void register(LevelingProfile profile);

    /**
     * Unregister a profile by ID.
     *
     * @param profileId the profile ID to remove
     */
    void unregister(String profileId);

    /**
     * Get a registered profile by ID.
     *
     * @param profileId the profile ID
     * @return the profile, or null if not registered
     */
    LevelingProfile getProfile(String profileId);

    /**
     * Grant XP to a player for a specific profile.
     * <ol>
     *   <li>Multiply amount by player's {@code XP_GAIN_MULTIPLIER}</li>
     *   <li>Add to stored XP for this profile</li>
    *   <li>Check if new XP total crosses level threshold</li>
    *   <li>If yes: update level, fire {@code LevelUpEvent} or {@code LevelDownEvent}</li>
     * </ol>
     *
     * @param playerUuid the player UUID
     * @param profileId  the leveling profile ID
     * @param amount     raw XP amount (before multipliers)
     * @param source     where the XP came from
     */
    void grantXP(UUID playerUuid, String profileId, double amount, XPSource source);

    /**
     * Get the current level for a player in a profile.
     *
     * @param playerUuid the player UUID
     * @param profileId  the profile ID
     * @return the current level, or 1 if no data
     */
    int getLevel(UUID playerUuid, String profileId);

    /**
     * Get the current cumulative XP for a player in a profile.
     *
     * @param playerUuid the player UUID
     * @param profileId  the profile ID
     * @return the cumulative XP, or 0 if no data
     */
    long getXP(UUID playerUuid, String profileId);

    /**
     * Set the XP and level directly (for admin/data loading).
     *
     * @param playerUuid the player UUID
     * @param profileId  the profile ID
     * @param xp         the cumulative XP to set
     */
    void setXP(UUID playerUuid, String profileId, long xp);
}
