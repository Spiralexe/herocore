package net.herotale.herocore.api.factions.api;

import net.herotale.herocore.api.factions.Faction;
import net.herotale.herocore.api.factions.InteractionType;
import net.herotale.herocore.api.factions.service.FactionService;

import java.util.Optional;
import java.util.UUID;

public class DefaultFactionAPI implements FactionAPI {
    private final FactionService factionService;

    public DefaultFactionAPI(FactionService factionService) {
        this.factionService = factionService;
    }

    @Override
    public boolean isEnabled() {
        return factionService.isEnabled();
    }

    @Override
    public boolean hasFaction(UUID playerId) {
        return factionService.hasFaction(playerId);
    }

    @Override
    public String getFactionId(UUID playerId) {
        Optional<Faction> faction = factionService.getPlayerFaction(playerId);
        return faction.map(Faction::getId).orElse(null);
    }

    @Override
    public String getFactionDisplayName(UUID playerId) {
        Optional<Faction> faction = factionService.getPlayerFaction(playerId);
        return faction.map(f -> f.getDisplayName() != null ? f.getDisplayName() : f.getId()).orElse(null);
    }

    @Override
    public String getFactionDisplayName(String factionId) {
        if (factionId == null) {
            return null;
        }

        Optional<Faction> faction = factionService.getFaction(factionId);
        return faction.map(f -> f.getDisplayName() != null ? f.getDisplayName() : f.getId()).orElse(null);
    }

    @Override
    public boolean areAllied(UUID a, UUID b) {
        if (!factionService.areSameFaction(a, b)) {
            return false;
        }
        Optional<Faction> faction = factionService.getPlayerFaction(a);
        return faction.map(f -> !f.isFriendlyFireAllowed()).orElse(false);
    }

    @Override
    public boolean canInteract(UUID a, UUID b, InteractionType type) {
        if (!isEnabled()) {
            return true;
        }

        return switch (type) {
            case PVP -> !areAllied(a, b);
            case CHAT, CLAN_JOIN, TRADE, PARTY -> true;
        };
    }
}
