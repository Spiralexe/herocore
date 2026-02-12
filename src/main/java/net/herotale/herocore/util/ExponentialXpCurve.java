package net.herotale.herocore.util;

import net.herotale.herocore.api.leveling.XpCurve;

/**
 * Exponential XP curve ported from MMOSkillTree's {@code ExponentialExperienceCurve}.
 *
 * <p>Formula (geometric series):
 * {@code threshold(level) = base * (growthRate^(level-1) - 1) / (growthRate - 1)}
 *
 * <p>Level 1 requires 0 XP. Level 2 requires {@code baseXp} XP.
 * Each subsequent level requires {@code growthRate} times more XP than the previous.
 */
public class ExponentialXpCurve implements XpCurve {

    private final int baseXp;
    private final double growthRate;

    /**
     * @param baseXp     XP required for level 1→2 (must be > 0)
     * @param growthRate exponential growth factor (must be > 1.0)
     */
    public ExponentialXpCurve(int baseXp, double growthRate) {
        if (baseXp <= 0) throw new IllegalArgumentException("baseXp must be > 0");
        if (growthRate <= 1.0) throw new IllegalArgumentException("growthRate must be > 1.0");
        this.baseXp = baseXp;
        this.growthRate = growthRate;
    }

    @Override
    public long getThreshold(int level) {
        if (level <= 1) return 0;
        // Geometric series: base * (r^(n-1) - 1) / (r - 1)
        // where n = level, so exponent = level - 1
        double numerator = Math.pow(growthRate, level - 1) - 1.0;
        double denominator = growthRate - 1.0;
        return Math.round(baseXp * (numerator / denominator));
    }

    @Override
    public int getLevel(long experience) {
        if (experience <= 0) return 1;
        // Inverse of geometric series:
        // experience = base * (r^(level-1) - 1) / (r - 1)
        // experience * (r - 1) / base = r^(level-1) - 1
        // r^(level-1) = experience * (r - 1) / base + 1
        // level - 1 = log_r(experience * (r - 1) / base + 1)
        // level = 1 + floor(log(experience * (r - 1) / base + 1) / log(r))
        double inner = (double) experience * (growthRate - 1.0) / baseXp + 1.0;
        if (inner <= 1.0) return 1;
        int level = 1 + (int) Math.floor(Math.log(inner) / Math.log(growthRate));
        // Clamp for floating-point drift so thresholds round-trip.
        while (getThreshold(level) > experience && level > 1) {
            level--;
        }
        while (getThreshold(level + 1) <= experience) {
            level++;
        }
        return level;
    }
}
