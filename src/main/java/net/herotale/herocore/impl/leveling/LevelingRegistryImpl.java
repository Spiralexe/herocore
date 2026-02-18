package net.herotale.herocore.impl.leveling;

import net.herotale.herocore.api.event.LevelDownEvent;
import net.herotale.herocore.api.event.LevelUpEvent;
import net.herotale.herocore.api.leveling.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of the {@link LevelingRegistry}.
 * Manages named leveling profiles and per-player XP/level data.
 * <p>
 * Events are emitted via injected consumers rather than a global event bus.
 */
public class LevelingRegistryImpl implements LevelingRegistry {

    private final Map<String, LevelingProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, PlayerLevelData>> playerData = new ConcurrentHashMap<>();
    private final Map<XPSource, Double> sourceWeights;
    private final Consumer<LevelUpEvent> levelUpSink;
    private final Consumer<LevelDownEvent> levelDownSink;

    /**
     * @param sourceWeights  XP source weights
     * @param levelUpSink    receives level-up events
     * @param levelDownSink  receives level-down events
     */
    public LevelingRegistryImpl(Map<XPSource, Double> sourceWeights,
                                Consumer<LevelUpEvent> levelUpSink,
                                Consumer<LevelDownEvent> levelDownSink) {
        this.sourceWeights = sourceWeights;
        this.levelUpSink = levelUpSink;
        this.levelDownSink = levelDownSink;
    }

    @Override
    public void register(LevelingProfile profile) {
        profiles.put(profile.getId(), profile);
        playerData.putIfAbsent(profile.getId(), new ConcurrentHashMap<>());
    }

    @Override
    public void unregister(String profileId) {
        profiles.remove(profileId);
    }

    @Override
    public LevelingProfile getProfile(String profileId) {
        return profiles.get(profileId);
    }

    @Override
    public void grantXP(UUID playerUuid, String profileId, double amount, XPSource source) {
        LevelingProfile profile = profiles.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown leveling profile: " + profileId);
        }

        // Apply source weight
        double sourceWeight = sourceWeights.getOrDefault(source, 1.0);
        amount *= sourceWeight;

        // TODO: XP_GAIN_MULTIPLIER should be read from EntityStatMap via Ref<EntityStore>
        // when a Ref-based XP granting API is available. For now, the multiplier is
        // only applied through the source weight system.

        PlayerLevelData data = getOrCreateData(profileId, playerUuid);
        int oldLevel = data.level;
        data.xp += Math.round(amount);

        // Clamp XP to max
        long maxXp = profile.getXpCurve().getThreshold(profile.getMaxLevel());
        if (maxXp > 0) {
            data.xp = Math.min(data.xp, maxXp);
        }

        // Recalculate level
        int newLevel = profile.getXpCurve().getLevel(data.xp);
        newLevel = Math.min(newLevel, profile.getMaxLevel());
        data.level = newLevel;

        // Fire level change events
        if (newLevel > oldLevel) {
            levelUpSink.accept(new LevelUpEvent(playerUuid, profileId, oldLevel, newLevel));
        } else if (newLevel < oldLevel) {
            levelDownSink.accept(new LevelDownEvent(playerUuid, profileId, oldLevel, newLevel));
        }
    }

    @Override
    public int getLevel(UUID playerUuid, String profileId) {
        PlayerLevelData data = getData(profileId, playerUuid);
        return data != null ? data.level : 1;
    }

    @Override
    public long getXP(UUID playerUuid, String profileId) {
        PlayerLevelData data = getData(profileId, playerUuid);
        return data != null ? data.xp : 0;
    }

    @Override
    public void setXP(UUID playerUuid, String profileId, long xp) {
        LevelingProfile profile = profiles.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown leveling profile: " + profileId);
        }

        PlayerLevelData data = getOrCreateData(profileId, playerUuid);
        data.xp = xp;
        data.level = Math.min(profile.getXpCurve().getLevel(xp), profile.getMaxLevel());
    }

    private PlayerLevelData getOrCreateData(String profileId, UUID playerUuid) {
        return playerData.computeIfAbsent(profileId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(playerUuid, k -> new PlayerLevelData());
    }

    private PlayerLevelData getData(String profileId, UUID playerUuid) {
        Map<UUID, PlayerLevelData> profileMap = playerData.get(profileId);
        return profileMap != null ? profileMap.get(playerUuid) : null;
    }

    private static class PlayerLevelData {
        long xp = 0;
        int level = 1;
    }
}
