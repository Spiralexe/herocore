package net.herotale.herocore.api.entity;

import java.util.List;

/**
 * Defines how a mob scales with level and how difficulty tiers
 * affect its stats when facing players of different levels.
 * <p>
 * Example: A "Spider" profile might grant per-level multipliers,
 * and when a player 8 levels lower faces it, the matching difficulty tier
 * adds additional modifiers and behavior flags.
 */
public interface MobScalingProfile {

    /**
     * @return The unique ID of this scaling profile (e.g., "spider", "skeleton_warrior")
     */
    String getId();

    /** Per-level multiplier for ARMOR. Default: 1.04 (4% per level above base). */
    double getArmorPerLevelMultiplier();

    /** Per-level multiplier for MAX_HEALTH. Default: 1.06. */
    double getHealthPerLevelMultiplier();

    /** Per-level multiplier for ATTACK_DAMAGE. Default: 1.05. */
    double getDamagePerLevelMultiplier();

    /** Per-level multiplier for all ELEMENTAL_RESIST_* attributes. Default: 1.03. */
    double getResistPerLevelMultiplier();

    /** Ordered list of difficulty tiers, checked from highest gap downward. */
    List<DifficultyTier> getDifficultyTiers();
}
