package net.herotale.herocore.api.status;

import java.util.Objects;

/**
 * Represents a status effect applied to an entity.
 * Hero-Core tracks presence, duration, stacks, and source — not behavior.
 */
public final class StatusEffect {

    private final String id;
    private final long durationMs;   // -1 = permanent until removed
    private final String source;
    private final int stacks;

    private StatusEffect(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Status id must not be null");
        this.durationMs = builder.durationMs;
        this.source = Objects.requireNonNull(builder.source, "Status source must not be null");
        this.stacks = builder.stacks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getSource() {
        return source;
    }

    public int getStacks() {
        return stacks;
    }

    public boolean isPermanent() {
        return durationMs < 0;
    }

    public static final class Builder {
        private String id;
        private long durationMs = -1;
        private String source;
        private int stacks = 1;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder duration(long durationMs) {
            this.durationMs = durationMs;
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
