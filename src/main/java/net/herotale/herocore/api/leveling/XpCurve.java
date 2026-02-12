package net.herotale.herocore.api.leveling;

/**
 * Defines an XP curve — the mapping between XP totals and levels.
 * Implementations: exponential, linear, discrete.
 */
public interface XpCurve {

    /**
     * Get the cumulative XP threshold required to reach a given level.
     *
     * @param level the target level (1-based)
     * @return cumulative XP required; 0 for level 1
     */
    long getThreshold(int level);

    /**
     * Determine the level for a given cumulative XP total.
     *
     * @param experience total XP accumulated
     * @return the level (1-based)
     */
    int getLevel(long experience);

    /**
     * Create an exponential XP curve.
     * Formula: {@code threshold(level) = base * (growthRate^(level-1) - 1) / (growthRate - 1)}
     *
     * @param baseXp     XP required for level 1→2
     * @param growthRate exponential growth rate (must be > 1.0)
     * @return the curve instance
     */
    static XpCurve exponential(int baseXp, double growthRate) {
        return new net.herotale.herocore.util.ExponentialXpCurve(baseXp, growthRate);
    }

    /**
     * Create a linear XP curve with fixed XP per level.
     *
     * @param xpPerLevel fixed XP required per level
     * @return the curve instance
     */
    static XpCurve linear(int xpPerLevel) {
        return new net.herotale.herocore.util.LinearXpCurve(xpPerLevel);
    }

    /**
     * Create a discrete XP curve from explicit threshold values.
     *
     * @param thresholds sorted array of cumulative XP thresholds (index 0 = level 2)
     * @return the curve instance
     */
    static XpCurve discrete(long[] thresholds) {
        return new net.herotale.herocore.util.DiscreteXpCurve(thresholds);
    }
}
