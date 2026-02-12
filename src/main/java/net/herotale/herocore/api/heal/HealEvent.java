package net.herotale.herocore.api.heal;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single heal event posted on a target entity.
 */
public class HealEvent {

    private final UUID healer;   // nullable for passive/environmental
    private final UUID target;
    private final double rawAmount;
    private double modifiedAmount;
    private final HealType healType;
    private boolean isCrit;
    private boolean cancelled;

    private HealEvent(Builder builder) {
        this.healer = builder.healer;
        this.target = Objects.requireNonNull(builder.target, "Target must not be null");
        this.rawAmount = builder.rawAmount;
        this.modifiedAmount = builder.rawAmount;
        this.healType = Objects.requireNonNull(builder.healType, "HealType must not be null");
        this.isCrit = false;
        this.cancelled = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getHealer() {
        return healer;
    }

    public UUID getTarget() {
        return target;
    }

    public double getRawAmount() {
        return rawAmount;
    }

    public double getModifiedAmount() {
        return modifiedAmount;
    }

    public void setModifiedAmount(double modifiedAmount) {
        this.modifiedAmount = modifiedAmount;
    }

    public HealType getHealType() {
        return healType;
    }

    public boolean isCrit() {
        return isCrit;
    }

    public void setCrit(boolean crit) {
        isCrit = crit;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static final class Builder {
        private UUID healer;
        private UUID target;
        private double rawAmount;
        private HealType healType;

        private Builder() {}

        public Builder healer(UUID healer) {
            this.healer = healer;
            return this;
        }

        public Builder target(UUID target) {
            this.target = target;
            return this;
        }

        public Builder rawAmount(double rawAmount) {
            this.rawAmount = rawAmount;
            return this;
        }

        public Builder healType(HealType healType) {
            this.healType = healType;
            return this;
        }

        public HealEvent build() {
            return new HealEvent(this);
        }
    }
}
