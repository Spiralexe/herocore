package net.herotale.herocore.api.factions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry of all faction definitions.
 * <p>
 * Downstream plugins (e.g. Heroes) populate this registry during their {@code setup()} phase
 * by calling {@link #register(FactionDefinition)}. Other plugins (e.g. Guilds) read from it
 * via {@link #getFaction(String)} or {@link #getAll()}.
 */
public class FactionRegistry {
    private final Map<String, FactionDefinition> factions = new HashMap<>();

    public FactionRegistry() {}

    /**
     * Register a faction definition. Overwrites any previously registered definition with the same id.
     */
    public void register(FactionDefinition definition) {
        if (definition == null || definition.getId() == null || definition.getId().isBlank()) {
            return;
        }
        factions.put(definition.getId().toLowerCase(), definition);
    }

    public Optional<Faction> getFaction(String factionId) {
        if (factionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(factions.get(factionId.toLowerCase()));
    }

    public Map<String, FactionDefinition> getAll() {
        return Collections.unmodifiableMap(factions);
    }
}
