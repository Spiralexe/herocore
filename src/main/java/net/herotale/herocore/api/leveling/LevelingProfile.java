package net.herotale.herocore.api.leveling;

import java.util.Objects;

/**
 * A named leveling profile registered with the {@link LevelingRegistry}.
 * Any plugin can register profiles for its own progression tracks.
 */
public final class LevelingProfile {

    private final String id;
    private final int maxLevel;
    private final XpCurve xpCurve;

    private LevelingProfile(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Profile id must not be null");
        this.maxLevel = builder.maxLevel;
        this.xpCurve = Objects.requireNonNull(builder.xpCurve, "XpCurve must not be null");
        if (maxLevel < 1) throw new IllegalArgumentException("maxLevel must be >= 1");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public XpCurve getXpCurve() {
        return xpCurve;
    }

    public static final class Builder {
        private String id;
        private int maxLevel = 60;
        private XpCurve xpCurve;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder xpCurve(XpCurve xpCurve) {
            this.xpCurve = xpCurve;
            return this;
        }

        public LevelingProfile build() {
            return new LevelingProfile(this);
        }
    }
}
