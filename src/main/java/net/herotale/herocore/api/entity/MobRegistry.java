package net.herotale.herocore.api.entity;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for all mob and NPC profiles in the system.
 * Content plugins register profiles here during plugin initialization.
 * <p>
 * Example usage:
 * <pre>{@code
 * MobRegistry registry = HeroCore.get().getMobRegistry();
 * registry.registerProfile(new ZombieProfile());
 * registry.registerScalingProfile(new UndeadScalingProfile());
 * }</pre>
 */
public interface MobRegistry {

    /**
     * Registers a mob or NPC profile.
     *
     * @param profile The profile to register
     * @throws IllegalStateException if a profile with the same ID already exists
     */
    void registerProfile(MobProfile profile);

    /**
     * Registers a scaling profile that can be referenced by multiple mob profiles.
     *
     * @param scalingProfile The scaling profile to register
     * @throws IllegalStateException if a scaling profile with the same ID already exists
     */
    void registerScalingProfile(MobScalingProfile scalingProfile);

    /**
     * Retrieves a mob profile by ID.
     *
     * @param id The unique mob ID
     * @return The profile, or empty if not found
     */
    Optional<MobProfile> getProfile(String id);

    /**
     * Retrieves a scaling profile by ID.
     *
     * @param id The unique scaling profile ID
     * @return The scaling profile, or empty if not found
     */
    Optional<MobScalingProfile> getScalingProfile(String id);

    /**
     * @return All registered mob profiles
     */
    Collection<MobProfile> getAllProfiles();

    /**
     * @return All registered scaling profiles
     */
    Collection<MobScalingProfile> getAllScalingProfiles();
}
