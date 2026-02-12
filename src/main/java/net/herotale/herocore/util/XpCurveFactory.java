package net.herotale.herocore.util;

import net.herotale.herocore.api.leveling.XpCurve;

/**
 * Factory for XP curves. Provides named constructors for common setups.
 */
public final class XpCurveFactory {

    private XpCurveFactory() {}

    /**
     * Standard RPG exponential curve — 100 base XP, 1.15 growth rate.
     */
    public static XpCurve standard() {
        return XpCurve.exponential(100, 1.15);
    }

    /**
     * Slow progression — 200 base XP, 1.25 growth rate.
     */
    public static XpCurve slow() {
        return XpCurve.exponential(200, 1.25);
    }

    /**
     * Fast progression — 50 base XP, 1.10 growth rate.
     */
    public static XpCurve fast() {
        return XpCurve.exponential(50, 1.10);
    }

    /**
     * Fixed 1000 XP per level.
     */
    public static XpCurve flatCurve() {
        return XpCurve.linear(1000);
    }
}
