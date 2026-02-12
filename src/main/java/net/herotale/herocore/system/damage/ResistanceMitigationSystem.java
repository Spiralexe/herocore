package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.damage.DamageType;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

import java.util.Map;

/**
 * Applies resistance mitigation per damage type.
 * <p>
 * <b>Physical damage is NOT mitigated here.</b> Physical mitigation is owned by
 * Hytale's native {@code ArmorDamageReduction} system. HeroCore writes Physical
 * resistance values via {@code DefenseBridge} and lets the engine apply them.
 * This avoids double-mitigation.
 * <p>
 * All other damage types (Projectile, Magical, Elemental) are mitigated here
 * using the canonical formula:
 * <pre>
 * finalDamage = baseDamage * (1 - resistancePercent)
 * </pre>
 * Where {@code resistancePercent} is clamped to [0, maxResistanceReduction] (default 0.9).
 * <p>
 * Each elemental/magical resistance uses the new 0–1 scale {@code _PERCENT} attributes.
 * If a percent attribute is zero, the system falls back to the legacy 0–100 scale
 * {@code ELEMENTAL_RESIST_*} attribute (divided by 100).
 * <p>
 * Third-party plugins can inject a {@code ShieldSystem} between this and crit
 * via {@code @SystemOrder(after = "herocore:resistance_mitigation", before = "herocore:critical_hit")}.
 */
@SystemOrder(after = "herocore:fall_damage_reduction")
public class ResistanceMitigationSystem implements DamageSystem {

    /**
     * Maps each DamageType to the RPGAttribute used for percent-based mitigation.
     * <p>
     * <b>Physical is NOT in this map.</b> Physical mitigation is handled entirely by
     * Hytale's native {@code ArmorDamageReduction} system via {@code DefenseBridge}.
     * Including it here would cause double-mitigation.
     * <p>
     * All mapped attributes use the 0–1 scale {@code _PERCENT} attributes.
     * Legacy 0–100 scale attributes are used as fallbacks only.
     */
    private static final Map<DamageType, RPGAttribute> RESIST_MAP = Map.ofEntries(
            Map.entry(DamageType.PROJECTILE, RPGAttribute.PROJECTILE_RESISTANCE_PERCENT),
            Map.entry(DamageType.MAGICAL,    RPGAttribute.MAGIC_RESIST),
            Map.entry(DamageType.FIRE,       RPGAttribute.FIRE_RESISTANCE_PERCENT),
            Map.entry(DamageType.ICE,        RPGAttribute.ICE_RESISTANCE_PERCENT),
            Map.entry(DamageType.LIGHTNING,  RPGAttribute.LIGHTNING_RESISTANCE_PERCENT),
            Map.entry(DamageType.POISON,     RPGAttribute.POISON_RESISTANCE_PERCENT),
            Map.entry(DamageType.ARCANE,     RPGAttribute.ARCANE_RESISTANCE_PERCENT)
    );

    /**
     * Fallback map: legacy 0–100 scale attributes. When the primary percent attribute
     * is zero, the system checks these and normalizes by dividing by 100.
     */
    private static final Map<DamageType, RPGAttribute> LEGACY_FALLBACK = Map.of(
            DamageType.FIRE,      RPGAttribute.ELEMENTAL_RESIST_FIRE,
            DamageType.ICE,       RPGAttribute.ELEMENTAL_RESIST_ICE,
            DamageType.LIGHTNING, RPGAttribute.ELEMENTAL_RESIST_LIGHTNING,
            DamageType.POISON,    RPGAttribute.ELEMENTAL_RESIST_POISON,
            DamageType.ARCANE,    RPGAttribute.ELEMENTAL_RESIST_ARCANE
    );

    /** Hard default; overridden by DamageConfig.maxResistanceReduction at runtime. */
    private double maxResistanceReduction = 0.9;

    private boolean enabled = true;

    @Override public String getId() { return "herocore:resistance_mitigation"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setMaxResistanceReduction(double cap) {
        this.maxResistanceReduction = cap;
    }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (event.getDamageType() == DamageType.TRUE) return;
        // Physical is handled by Hytale's native ArmorDamageReduction via DefenseBridge
        if (event.getDamageType() == DamageType.PHYSICAL) return;
        if (victimStats == null) return;

        RPGAttribute resistAttr = RESIST_MAP.get(event.getDamageType());
        if (resistAttr == null) return;

        double resistValue = victimStats.getValue(resistAttr);

        // If the primary percent attribute is zero, try the legacy 0–100 fallback
        if (resistValue <= 0.0) {
            RPGAttribute fallback = LEGACY_FALLBACK.get(event.getDamageType());
            if (fallback != null) {
                resistValue = victimStats.getValue(fallback) / 100.0;
            }
        }

        // Magic penetration reduces effective resistance for elemental/magical damage
        if (attackerStats != null) {
            double pen = attackerStats.getValue(RPGAttribute.MAGIC_PENETRATION);
            resistValue = Math.max(0.0, resistValue - pen);
        }

        // Clamp to configured max (prevents 100% immunity)
        double reduction = Math.min(resistValue, maxResistanceReduction);

        if (reduction > 0.0) {
            // finalDamage = baseDamage * (1 - resistancePercent)
            event.setModifiedAmount(event.getModifiedAmount() * (1.0 - reduction));
        }
    }

    private static boolean isElementalOrMagical(DamageType type) {
        return type != DamageType.PHYSICAL && type != DamageType.PROJECTILE
                && type != DamageType.TRUE && type != DamageType.FALL
                && type != DamageType.VOID;
    }
}
