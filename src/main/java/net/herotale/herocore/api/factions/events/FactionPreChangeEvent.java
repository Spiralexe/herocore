package net.herotale.herocore.api.factions.events;

import java.util.UUID;

public final class FactionPreChangeEvent extends FactionEvent {
    private final String oldFaction;
    private final String newFaction;
    private boolean cancelled;

    public FactionPreChangeEvent(UUID playerId, String oldFaction, String newFaction) {
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

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
