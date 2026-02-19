package net.herotale.herocore.api.status;

import java.util.Objects;

/**
 * Represents a status effect applied to an entity.
 * Hero-Core tracks presence, duration, stacks, and source — not behavior.
 * <p>
 * Duration is measured in <b>seconds</b> at every level — matching the
 * {@code StatusEffectIndexComponent}'s {@code remainingSeconds} field and
 * the {@code dt}-based decrement in {@code StatusEffectTickSystem}.
 */
public final class StatusEffect {

    private final String id;
    private final float durationSeconds;   // -1 = permanent until removed
    private final String source;
    private final int stacks;

    private StatusEffect(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Status id must not be null");
        this.durationSeconds = builder.durationSeconds;
        this.source = Objects.requireNonNull(builder.source, "Status source must not be null");
        this.stacks = builder.stacks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    /** Duration in seconds. Negative means permanent until explicitly removed. */
    public float getDurationSeconds() {
        return durationSeconds;
    }

    public String getSource() {
        return source;
    }

    public int getStacks() {
        return stacks;
    }

    public boolean isPermanent() {
        return durationSeconds < 0;
    }

    public static final class Builder {
        private String id;
        private float durationSeconds = -1f;
        private String source;
        private int stacks = 1;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /** Set the effect duration in seconds. Pass a negative value for permanent effects. */
        public Builder durationSeconds(float durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder stacks(int stacks) {
            this.stacks = stacks;
            return this;
        }

        public StatusEffect build() {
            return new StatusEffect(this);
        }
    }
}
