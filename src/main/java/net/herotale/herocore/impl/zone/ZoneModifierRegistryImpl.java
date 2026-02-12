package net.herotale.herocore.impl.zone;

import net.herotale.herocore.api.zone.ZoneModifierDefinition;
import net.herotale.herocore.api.zone.ZoneModifierRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ZoneModifierRegistry}.
 * Stores zone modifier definitions for lookup by zone systems.
 */
public class ZoneModifierRegistryImpl implements ZoneModifierRegistry {

    private final Map<String, ZoneModifierDefinition> definitions = new ConcurrentHashMap<>();

    @Override
    public void register(ZoneModifierDefinition definition) {
        definitions.put(definition.getZoneId(), definition);
    }

    @Override
    public void unregister(String zoneId) {
        definitions.remove(zoneId);
    }

    @Override
    public ZoneModifierDefinition getDefinition(String zoneId) {
        return definitions.get(zoneId);
    }
}
