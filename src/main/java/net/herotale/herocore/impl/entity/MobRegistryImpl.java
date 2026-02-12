package net.herotale.herocore.impl.entity;

import net.herotale.herocore.api.entity.MobProfile;
import net.herotale.herocore.api.entity.MobRegistry;
import net.herotale.herocore.api.entity.MobScalingProfile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of MobRegistry.
 * Stores all mob profiles and scaling profiles registered by content plugins.
 */
public class MobRegistryImpl implements MobRegistry {

    private final Map<String, MobProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, MobScalingProfile> scalingProfiles = new ConcurrentHashMap<>();

    @Override
    public void registerProfile(MobProfile profile) {
        if (profiles.containsKey(profile.getId())) {
            throw new IllegalStateException("MobProfile with ID '" + profile.getId() + "' is already registered");
        }
        profiles.put(profile.getId(), profile);
    }

    @Override
    public void registerScalingProfile(MobScalingProfile scalingProfile) {
        if (scalingProfiles.containsKey(scalingProfile.getId())) {
            throw new IllegalStateException("MobScalingProfile with ID '" + scalingProfile.getId() + "' is already registered");
        }
        scalingProfiles.put(scalingProfile.getId(), scalingProfile);
    }

    @Override
    public Optional<MobProfile> getProfile(String id) {
        return Optional.ofNullable(profiles.get(id));
    }

    @Override
    public Optional<MobScalingProfile> getScalingProfile(String id) {
        return Optional.ofNullable(scalingProfiles.get(id));
    }

    @Override
    public Collection<MobProfile> getAllProfiles() {
        return profiles.values();
    }

    @Override
    public Collection<MobScalingProfile> getAllScalingProfiles() {
        return scalingProfiles.values();
    }
}
