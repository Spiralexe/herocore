package net.herotale.herocore.api.factions.events;

import java.util.UUID;

public final class FactionPostChangeEvent extends FactionEvent {
    private final String oldFaction;
    private final String newFaction;

    public FactionPostChangeEvent(UUID playerId, String oldFaction, String newFaction) {
        super(playerId);
        this.oldFaction = oldFaction;
        this.newFaction = newFaction;
    }

    public String getOldFaction() {
        return oldFaction;
    }

    public String getNewFaction() {
        return newFaction;
    }
}
