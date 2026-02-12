package net.herotale.herocore.util;

import net.herotale.herocore.api.leveling.XpCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all XP curve implementations.
 */
class XpCurveTest {

    // --- Exponential Curve ---

    @Test
    void exponential_level1_requires0XP() {
        XpCurve curve = XpCurve.exponential(100, 1.15);
        assertEquals(0, curve.getThreshold(1));
    }

    @Test
    void exponential_level2_requiresBaseXP() {
        XpCurve curve = XpCurve.exponential(100, 1.15);
        // threshold(2) = 100 * (1.15^1 - 1) / (1.15 - 1) = 100 * 0.15 / 0.15 = 100
        assertEquals(100, curve.getThreshold(2));
    }

    @Test
    void exponential_level3_usesGeometricSeries() {
        XpCurve curve = XpCurve.exponential(100, 1.15);
        // threshold(3) = 100 * (1.15^2 - 1) / (1.15 - 1) = 100 * 0.3225 / 0.15 = 215
        long threshold = curve.getThreshold(3);
        assertEquals(215, threshold);
    }

    @Test
    void exponential_getLevel_invertsThreshold() {
        XpCurve curve = XpCurve.exponential(100, 1.15);
        assertEquals(1, curve.getLevel(0));
        assertEquals(1, curve.getLevel(99));
        assertEquals(2, curve.getLevel(100));
        assertEquals(2, curve.getLevel(214));
        assertEquals(3, curve.getLevel(215));
    }

    @Test
    void exponential_roundTrip_consistency() {
        XpCurve curve = XpCurve.exponential(100, 1.15);
        for (int level = 1; level <= 30; level++) {
            long threshold = curve.getThreshold(level);
            assertEquals(level, curve.getLevel(threshold),
                    "Level mismatch at threshold for level " + level);
        }
    }

    @Test
    void exponential_rejectsInvalidParams() {
        assertThrows(IllegalArgumentException.class, () -> XpCurve.exponential(0, 1.15));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.exponential(100, 1.0));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.exponential(100, 0.5));
    }

    // --- Linear Curve ---

    @Test
    void linear_level1_requires0XP() {
        XpCurve curve = XpCurve.linear(1000);
        assertEquals(0, curve.getThreshold(1));
    }

    @Test
    void linear_fixedXpPerLevel() {
        XpCurve curve = XpCurve.linear(1000);
        assertEquals(1000, curve.getThreshold(2));
        assertEquals(2000, curve.getThreshold(3));
        assertEquals(9000, curve.getThreshold(10));
    }

    @Test
    void linear_getLevel_invertsThreshold() {
        XpCurve curve = XpCurve.linear(1000);
        assertEquals(1, curve.getLevel(0));
        assertEquals(1, curve.getLevel(999));
        assertEquals(2, curve.getLevel(1000));
        assertEquals(2, curve.getLevel(1999));
        assertEquals(3, curve.getLevel(2000));
    }

    @Test
    void linear_roundTrip() {
        XpCurve curve = XpCurve.linear(500);
        for (int level = 1; level <= 50; level++) {
            assertEquals(level, curve.getLevel(curve.getThreshold(level)));
        }
    }

    // --- Discrete Curve ---

    @Test
    void discrete_level1_requires0XP() {
        XpCurve curve = XpCurve.discrete(new long[]{100, 300, 600, 1000});
        assertEquals(0, curve.getThreshold(1));
    }

    @Test
    void discrete_usesExplicitThresholds() {
        XpCurve curve = XpCurve.discrete(new long[]{100, 300, 600, 1000});
        assertEquals(100, curve.getThreshold(2));
        assertEquals(300, curve.getThreshold(3));
        assertEquals(600, curve.getThreshold(4));
        assertEquals(1000, curve.getThreshold(5));
    }

    @Test
    void discrete_getLevel_invertsThreshold() {
        XpCurve curve = XpCurve.discrete(new long[]{100, 300, 600, 1000});
        assertEquals(1, curve.getLevel(0));
        assertEquals(1, curve.getLevel(99));
        assertEquals(2, curve.getLevel(100));
        assertEquals(2, curve.getLevel(299));
        assertEquals(3, curve.getLevel(300));
        assertEquals(5, curve.getLevel(1000));
        assertEquals(5, curve.getLevel(9999)); // beyond max
    }

    @Test
    void discrete_roundTrip() {
        long[] thresholds = {100, 300, 600, 1000, 1500, 2100};
        XpCurve curve = XpCurve.discrete(thresholds);
        for (int level = 1; level <= thresholds.length + 1; level++) {
            assertEquals(level, curve.getLevel(curve.getThreshold(level)));
        }
    }
}
