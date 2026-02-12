package net.herotale.herocore.api.event;

import net.herotale.herocore.api.attribute.RPGAttribute;

import java.util.UUID;

/**
 * Fired when the computed value of an attribute changes.
 */
public class AttributeChangeEvent {

    private final UUID entity;
    private final RPGAttribute attribute;
    private final double oldValue;
    private final double newValue;

    public AttributeChangeEvent(UUID entity, RPGAttribute attribute, double oldValue, double newValue) {
        this.entity = entity;
        this.attribute = attribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UUID getEntity() {
        return entity;
    }

    public RPGAttribute getAttribute() {
        return attribute;
    }

    public double getOldValue() {
        return oldValue;
    }

    public double getNewValue() {
        return newValue;
    }
}
