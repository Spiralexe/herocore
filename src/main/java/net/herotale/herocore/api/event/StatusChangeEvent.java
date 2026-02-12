package net.herotale.herocore.api.event;

import net.herotale.herocore.api.status.StatusEffect;

import java.util.UUID;

/**
 * Fired when a status effect is applied to or removed from an entity.
 * <p>
 * This event allows UI systems (like heroes-rpg) to react to status changes
 * and update HUD displays accordingly.
 */
public class StatusChangeEvent {

    private final UUID entityUUID;
    private final StatusEffect effect;
    private final ChangeType changeType;

    public StatusChangeEvent(UUID entityUUID, StatusEffect effect, ChangeType changeType) {
        this.entityUUID = entityUUID;
        this.effect = effect;
        this.changeType = changeType;
    }

    /**
     * @return The entity whose status changed
     */
    public UUID getEntityUUID() {
        return entityUUID;
    }

    /**
     * @return The status effect that was applied, refreshed, or removed
     */
    public StatusEffect getEffect() {
        return effect;
    }

    /**
     * @return The type of change (APPLIED, REFRESHED, REMOVED)
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    public enum ChangeType {
        /** Effect was newly applied */
        APPLIED,
        
        /** Effect duration was extended or stacks were added */
        REFRESHED,
        
        /** Effect expired or was manually removed */
        REMOVED
    }
}
