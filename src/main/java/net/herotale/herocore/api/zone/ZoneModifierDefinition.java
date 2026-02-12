package net.herotale.herocore.api.zone;

import net.herotale.herocore.api.attribute.AttributeModifier;

import java.util.List;
import java.util.Objects;

/**
 * Defines modifiers applied to players within a named zone.
 */
public final class ZoneModifierDefinition {

    private final String zoneId;
    private final ZoneTrigger trigger;
    private final double radius;   // only used for PROXIMITY trigger
    private final List<AttributeModifier> modifiers;

    private ZoneModifierDefinition(Builder builder) {
        this.zoneId = Objects.requireNonNull(builder.zoneId, "zoneId must not be null");
        this.trigger = Objects.requireNonNull(builder.trigger, "trigger must not be null");
        this.radius = builder.radius;
        this.modifiers = List.copyOf(builder.modifiers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getZoneId() {
        return zoneId;
    }

    public ZoneTrigger getTrigger() {
        return trigger;
    }

    public double getRadius() {
        return radius;
    }

    public List<AttributeModifier> getModifiers() {
        return modifiers;
    }

    public static final class Builder {
        private String zoneId;
        private ZoneTrigger trigger;
        private double radius;
        private final java.util.ArrayList<AttributeModifier> modifiers = new java.util.ArrayList<>();

        private Builder() {}

        public Builder zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder trigger(ZoneTrigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder modifier(AttributeModifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        public ZoneModifierDefinition build() {
            return new ZoneModifierDefinition(this);
        }
    }
}
