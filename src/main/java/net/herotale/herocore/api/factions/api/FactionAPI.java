package net.herotale.herocore.api.factions.api;

import net.herotale.herocore.api.factions.InteractionType;

import java.util.UUID;

public interface FactionAPI {
    boolean isEnabled();

    boolean hasFaction(UUID playerId);

    String getFactionId(UUID playerId);

    String getFactionDisplayName(UUID playerId);

    String getFactionDisplayName(String factionId);

    boolean areAllied(UUID a, UUID b);

    boolean canInteract(UUID a, UUID b, InteractionType type);
}
