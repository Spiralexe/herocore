package net.herotale.herocore.api.zone;

/**
 * Registry for zone-scoped attribute modifiers.
 * Modifiers are applied when players enter zones and removed when they leave.
 */
public interface ZoneModifierRegistry {

    /**
     * Register a zone modifier definition.
     */
    void register(ZoneModifierDefinition definition);

    /**
     * Unregister a zone modifier definition by zone ID.
     */
    void unregister(String zoneId);

    /**
     * Get a registered definition by zone ID.
     */
    ZoneModifierDefinition getDefinition(String zoneId);
}
