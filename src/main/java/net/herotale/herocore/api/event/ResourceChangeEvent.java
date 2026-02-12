package net.herotale.herocore.api.event;

import net.herotale.herocore.api.resource.ResourceType;

import java.util.UUID;

/**
 * Fired when a player's resource (health, mana, stamina) changes.
 */
public class ResourceChangeEvent {

    private final UUID player;
    private final ResourceType resourceType;
    private final double oldValue;
    private final double newValue;

    public ResourceChangeEvent(UUID player, ResourceType resourceType, double oldValue, double newValue) {
        this.player = player;
        this.resourceType = resourceType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UUID getPlayer() {
        return player;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public double getOldValue() {
        return oldValue;
    }

    public double getNewValue() {
        return newValue;
    }
}
