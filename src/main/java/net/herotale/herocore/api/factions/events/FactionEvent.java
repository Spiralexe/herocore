package net.herotale.herocore.api.factions.events;

import java.util.UUID;

public abstract class FactionEvent {
    private final UUID playerId;

    protected FactionEvent(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
