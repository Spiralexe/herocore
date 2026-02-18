package net.herotale.herocore.impl.heal;

import net.herotale.herocore.api.heal.HealType;

/**
 * Pure-math functions for each heal pipeline stage.
 * <p>
 * Stateless and side-effect-free — any function can be unit-tested with
 * primitive inputs. The ECS {@code EntityEventSystem} implementations are
 * thin wrappers that read values from {@code EntityStatMap} and delegate
 * to these formulas.
 */
public final class HealFormulas {

    private HealFormulas() {}

    // ── Healing Power Scaling ──────────────────────────────────────

    /**
     * Scale healing by the healer's HEALING_POWER attribute.
     * Applies to SPELL and PASSIVE heals. Optionally scales REGEN_TICK.
     *
     * @param heal             current heal amount
     * @param healingPower     healer's HEALING_POWER attribute (0+ scale)
     * @param healType         the heal type
     * @param scalesRegenTick  whether regen ticks should be scaled
     * @return scaled heal amount
     */
    public static float applyHealingPowerScaling(float heal, float healingPower, HealType healType, boolean scalesRegenTick) {
        boolean shouldScale = (healType == HealType.SPELL || healType == HealType.PASSIVE);
        if (!shouldScale && healType == HealType.REGEN_TICK && scalesRegenTick) {
            shouldScale = true;
        }
        if (shouldScale && healingPower != 0) {
            return heal * (1.0f + healingPower);
        }
        return heal;
    }

    // ── Healing Received Bonus ─────────────────────────────────────

    /**
     * Apply the target's HEALING_RECEIVED_BONUS multiplier.
     *
     * @param heal  current heal amount
     * @param bonus target's HEALING_RECEIVED_BONUS attribute (0+ scale)
     * @return scaled heal amount
     */
    public static float applyHealingReceivedBonus(float heal, float bonus) {
        return heal * (1.0f + bonus);
    }

    // ── Heal Crit ──────────────────────────────────────────────────

    /**
     * Apply heal crit multiplier.
     *
     * @param heal           current heal amount
     * @param critMultiplier the heal crit multiplier (e.g., 1.5 = 150%)
     * @return crit-amplified heal amount
     */
    public static float applyHealCritMultiplier(float heal, float critMultiplier) {
        return heal * critMultiplier;
    }

    /**
     * Roll whether a heal crit occurs.
     *
     * @param healCritChance chance (0-1 scale)
     * @param roll           random roll (0-1)
     * @return true if heal crit triggers
     */
    public static boolean rollHealCrit(float healCritChance, float roll) {
        return healCritChance > 0 && roll < healCritChance;
    }
}
