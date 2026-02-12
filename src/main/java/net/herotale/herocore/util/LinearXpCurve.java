package net.herotale.herocore.util;

import net.herotale.herocore.api.leveling.XpCurve;

/**
 * Linear XP curve — fixed XP per level.
 * Level 1 = 0 XP, Level 2 = xpPerLevel, Level 3 = 2 * xpPerLevel, etc.
 */
public class LinearXpCurve implements XpCurve {

    private final int xpPerLevel;

    /**
     * @param xpPerLevel constant XP required per level (must be > 0)
     */
    public LinearXpCurve(int xpPerLevel) {
        if (xpPerLevel <= 0) throw new IllegalArgumentException("xpPerLevel must be > 0");
        this.xpPerLevel = xpPerLevel;
    }

    @Override
    public long getThreshold(int level) {
        if (level <= 1) return 0;
        return (long) (level - 1) * xpPerLevel;
    }

    @Override
    public int getLevel(long experience) {
        if (experience <= 0) return 1;
        return 1 + (int) (experience / xpPerLevel);
    }
}
