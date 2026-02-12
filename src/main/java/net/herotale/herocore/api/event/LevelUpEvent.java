package net.herotale.herocore.api.event;

import java.util.UUID;

/**
 * Fired when a player levels up in any leveling profile.
 */
public class LevelUpEvent {

    private final UUID player;
    private final String profileId;
    private final int oldLevel;
    private final int newLevel;

    public LevelUpEvent(UUID player, String profileId, int oldLevel, int newLevel) {
        this.player = player;
        this.profileId = profileId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getProfileId() {
        return profileId;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
