package net.herotale.herocore.api.entity;

import net.herotale.herocore.api.attribute.AttributeModifier;

import java.util.List;
import java.util.Objects;

/**
 * Qualitative difficulty modifier that activates on a level gap threshold.
 */
public final class DifficultyTier {

    private final int levelGapThreshold;
    private final String name;
    private final List<AttributeModifier> additionalModifiers;
    private final List<String> behaviorFlags;
    private final double xpPenaltyMultiplier;

    public DifficultyTier(int levelGapThreshold,
                          String name,
                          List<AttributeModifier> additionalModifiers,
                          List<String> behaviorFlags,
                          double xpPenaltyMultiplier) {
        this.levelGapThreshold = levelGapThreshold;
        this.name = Objects.requireNonNull(name, "name");
        this.additionalModifiers = List.copyOf(additionalModifiers);
        this.behaviorFlags = List.copyOf(behaviorFlags);
        this.xpPenaltyMultiplier = xpPenaltyMultiplier;
    }

    public int getLevelGapThreshold() {
        return levelGapThreshold;
    }

    public String getName() {
        return name;
    }

    public List<AttributeModifier> getAdditionalModifiers() {
        return additionalModifiers;
    }

    public List<String> getBehaviorFlags() {
        return behaviorFlags;
    }

    public double getXpPenaltyMultiplier() {
        return xpPenaltyMultiplier;
    }
}
