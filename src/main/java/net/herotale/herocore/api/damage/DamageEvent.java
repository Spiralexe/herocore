package net.herotale.herocore.api.damage;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single damage event posted on a victim entity.
 * {@code rawAmount} is the original and never mutated; {@code modifiedAmount} is mutated by ordered systems.
 */
public class DamageEvent {

    private final UUID attacker;   // nullable for environmental damage
    private final UUID victim;
    private final double rawAmount;
    private double modifiedAmount;
    private final DamageType damageType;
    private final Set<DamageFlag> flags;
    private boolean cancelled;

    private DamageEvent(Builder builder) {
        this.attacker = builder.attacker;
        this.victim = Objects.requireNonNull(builder.victim, "Victim must not be null");
        this.rawAmount = builder.rawAmount;
        this.modifiedAmount = builder.rawAmount;
        this.damageType = Objects.requireNonNull(builder.damageType, "DamageType must not be null");
        this.flags = builder.flags.isEmpty() ? EnumSet.noneOf(DamageFlag.class) : EnumSet.copyOf(builder.flags);
        this.cancelled = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getAttacker() {
        return attacker;
    }

    public UUID getVictim() {
        return victim;
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

    public DamageType getDamageType() {
        return damageType;
    }

    public Set<DamageFlag> getFlags() {
        return flags;
    }

    public void addFlag(DamageFlag flag) {
        flags.add(flag);
    }

    public boolean hasFlag(DamageFlag flag) {
        return flags.contains(flag);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static final class Builder {
        private UUID attacker;
        private UUID victim;
        private double rawAmount;
        private DamageType damageType;
        private final Set<DamageFlag> flags = EnumSet.noneOf(DamageFlag.class);

        private Builder() {}

        public Builder attacker(UUID attacker) {
            this.attacker = attacker;
            return this;
        }

        public Builder victim(UUID victim) {
            this.victim = victim;
            return this;
        }

        public Builder rawAmount(double rawAmount) {
            this.rawAmount = rawAmount;
            return this;
        }

        public Builder damageType(DamageType damageType) {
            this.damageType = damageType;
            return this;
        }

        public Builder flag(DamageFlag flag) {
            this.flags.add(flag);
            return this;
        }

        public DamageEvent build() {
            return new DamageEvent(this);
        }
    }
}
