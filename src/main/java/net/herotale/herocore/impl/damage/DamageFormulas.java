package net.herotale.herocore.impl.damage;

import net.herotale.herocore.api.damage.DamageType;

import java.util.Map;

/**
 * Pure-math functions for each damage pipeline stage.
 * <p>
 * Stateless and side-effect-free — any function can be unit-tested with
 * primitive inputs. The ECS {@code EntityEventSystem} implementations are
 * thin wrappers that read values from {@code EntityStatMap} and delegate
 * to these formulas.
 */
public final class DamageFormulas {

    private DamageFormulas() {}

    // ── Attack Damage Bonus ────────────────────────────────────────

    /**
     * Scale damage by the attacker's ATTACK_DAMAGE attribute.
     *
     * @param damage      current damage
     * @param attackDamage attacker's ATTACK_DAMAGE value (0-1 scale, e.g. 0.5 = +50%)
     * @return scaled damage
     */
    public static float applyAttackDamageBonus(float damage, float attackDamage) {
        if (attackDamage > 0) {
            return damage * (1.0f + attackDamage);
        }
        return damage;
    }

    // ── Fall Damage Reduction ──────────────────────────────────────

    /**
     * Reduce fall damage by the victim's FALL_DAMAGE_REDUCTION attribute.
     *
     * @param damage        current fall damage
     * @param fallReduction victim's FALL_DAMAGE_REDUCTION (0-1 scale, capped at 0.9)
     * @return reduced damage
     */
    public static float applyFallDamageReduction(float damage, float fallReduction) {
        if (fallReduction > 0) {
            float reduction = Math.min(fallReduction, 0.9f);
            return damage * (1.0f - reduction);
        }
        return damage;
    }

    // ── Resistance Mitigation ──────────────────────────────────────

    /**
     * The canonical resistance map: DamageType → a string key for the resist attribute.
     * Physical and TRUE are excluded (physical = native engine, TRUE = no resistance).
     */
    private static final Map<DamageType, String> RESIST_KEYS = Map.ofEntries(
            Map.entry(DamageType.PROJECTILE, "projectile_resistance_percent"),
            Map.entry(DamageType.MAGICAL,    "magic_resist"),
            Map.entry(DamageType.FIRE,       "fire_resistance_percent"),
            Map.entry(DamageType.ICE,        "ice_resistance_percent"),
            Map.entry(DamageType.LIGHTNING,  "lightning_resistance_percent"),
            Map.entry(DamageType.POISON,     "poison_resistance_percent"),
            Map.entry(DamageType.ARCANE,     "arcane_resistance_percent")
    );

    /**
     * Apply resistance mitigation to damage.
     * <p>
     * Physical and TRUE damage bypass this entirely. For all other types:
     * {@code finalDamage = damage * (1 - clamp(resistance - penetration, 0, maxResist))}
     *
     * @param damage         current damage
     * @param resistValue    victim's resistance (0-1 scale)
     * @param magicPen       attacker's magic penetration (0-1 scale)
     * @param maxResist      configured max resistance cap (e.g., 0.9)
     * @return mitigated damage
     */
    public static float applyResistance(float damage, float resistValue, float magicPen, float maxResist) {
        float effectiveResist = Math.max(0.0f, resistValue - magicPen);
        float reduction = Math.min(effectiveResist, maxResist);
        if (reduction > 0.0f) {
            return damage * (1.0f - reduction);
        }
        return damage;
    }

    /**
     * Check if a damage type should be mitigated by the resistance system.
     * Physical is handled by Hytale native. TRUE bypasses all resistance.
     */
    public static boolean shouldApplyResistance(DamageType type) {
        return type != DamageType.TRUE && type != DamageType.PHYSICAL;
    }

    // ── Critical Hit ───────────────────────────────────────────────

    /**
     * Apply crit multiplier to damage.
     *
     * @param damage         current damage
     * @param critMultiplier the crit damage multiplier (e.g., 2.0 = 200%)
     * @return crit-amplified damage
     */
    public static float applyCritMultiplier(float damage, float critMultiplier) {
        return damage * critMultiplier;
    }

    /**
     * Roll whether a critical hit occurs.
     *
     * @param critChance chance (0-1 scale)
     * @param roll       random roll (0-1), e.g. from {@code random.nextFloat()}
     * @return true if crit triggers
     */
    public static boolean rollCrit(float critChance, float roll) {
        return critChance > 0 && roll < critChance;
    }

    // ── Lifesteal ──────────────────────────────────────────────────

    /**
     * Compute the lifesteal heal amount from final damage.
     *
     * @param damage    final modified damage
     * @param lifesteal attacker's LIFESTEAL attribute (0-1 scale)
     * @return heal amount, or 0 if no lifesteal
     */
    public static float computeLifestealHeal(float damage, float lifesteal) {
        if (lifesteal <= 0) return 0;
        return damage * lifesteal;
    }

    // ── Minimum Damage ─────────────────────────────────────────────

    /**
     * Enforce a minimum damage floor.
     *
     * @param damage     current damage
     * @param minDamage  configured minimum
     * @return max(damage, minDamage)
     */
    public static float applyMinimumDamage(float damage, float minDamage) {
        return Math.max(damage, minDamage);
    }
}
