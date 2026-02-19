package net.herotale.herocore.api.factions.events;

import java.util.UUID;

public final class FactionClearedEvent extends FactionEvent {
    private final String oldFaction;

    public FactionClearedEvent(UUID playerId, String oldFaction) {
        super(playerId);
        this.oldFaction = oldFaction;
    }

    public String getOldFaction() {
        return oldFaction;
    }
}
