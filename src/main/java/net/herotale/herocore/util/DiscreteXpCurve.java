package net.herotale.herocore.util;

import net.herotale.herocore.api.leveling.XpCurve;

import java.util.Arrays;

/**
 * Discrete XP curve — explicit threshold table.
 * Each entry defines the cumulative XP required for that level.
 * Index 0 = level 2 threshold, index 1 = level 3 threshold, etc.
 */
public class DiscreteXpCurve implements XpCurve {

    private final long[] thresholds;

    /**
     * @param thresholds sorted ascending cumulative XP thresholds.
     *                   Index 0 = XP for level 2. Must be sorted.
     */
    public DiscreteXpCurve(long[] thresholds) {
        this.thresholds = Arrays.copyOf(thresholds, thresholds.length);
    }

    @Override
    public long getThreshold(int level) {
        if (level <= 1) return 0;
        int idx = level - 2;
        if (idx >= thresholds.length) {
            return thresholds[thresholds.length - 1]; // cap at last defined
        }
        return thresholds[idx];
    }

    @Override
    public int getLevel(long experience) {
        if (experience <= 0) return 1;
        // Binary search for the highest threshold <= experience
        int low = 0, high = thresholds.length - 1;
        int result = 0; // will map to level 1 if experience < thresholds[0]
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (thresholds[mid] <= experience) {
                result = mid + 1; // This means we're at least level (mid + 2)
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result + 1; // +1 because level is 1-based
    }
}
