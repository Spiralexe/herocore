package net.herotale.herocore.api.factions.service;

import net.herotale.herocore.api.factions.Faction;
import net.herotale.herocore.api.factions.FactionChangeReason;

import java.util.Optional;
import java.util.UUID;

public interface FactionService {
    boolean isEnabled();

    Optional<Faction> getFaction(String factionId);

    Optional<Faction> getPlayerFaction(UUID playerId);

    boolean hasFaction(UUID playerId);

    boolean canSelectFaction(UUID playerId, String factionId);

    void setFaction(UUID playerId, String factionId, FactionChangeReason reason);

    void clearFaction(UUID playerId, FactionChangeReason reason);

    boolean areSameFaction(UUID a, UUID b);
}
