package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.damage.DamageType;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

import java.util.Map;

/**
 * Applies armor / elemental resistance mitigation.
 * <p>
 * Runs after fall-damage reduction so all early scaling is resolved first.
 * Third-party plugins can inject a {@code ShieldSystem} between this and crit
 * via {@code @SystemOrder(after = "herocore:resistance_mitigation", before = "herocore:critical_hit")}.
 */
@SystemOrder(after = "herocore:fall_damage_reduction")
public class ResistanceMitigationSystem implements DamageSystem {

    private static final Map<DamageType, RPGAttribute> RESIST_MAP = Map.of(
            DamageType.PHYSICAL, RPGAttribute.ARMOR,
            DamageType.FIRE, RPGAttribute.ELEMENTAL_RESIST_FIRE,
            DamageType.ICE, RPGAttribute.ELEMENTAL_RESIST_ICE,
            DamageType.LIGHTNING, RPGAttribute.ELEMENTAL_RESIST_LIGHTNING,
            DamageType.POISON, RPGAttribute.ELEMENTAL_RESIST_POISON,
            DamageType.ARCANE, RPGAttribute.ELEMENTAL_RESIST_ARCANE
    );

    private boolean enabled = true;

    @Override public String getId() { return "herocore:resistance_mitigation"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (event.getDamageType() == DamageType.TRUE) return;
        if (victimStats == null) return;
        RPGAttribute resistAttr = RESIST_MAP.get(event.getDamageType());
        if (resistAttr == null) return;
        double resistValue = victimStats.getValue(resistAttr);
        if (event.getDamageType() == DamageType.PHYSICAL && attackerStats != null) {
            resistValue = Math.max(0, resistValue - attackerStats.getValue(RPGAttribute.ARMOR_PENETRATION));
        }
        if (event.getDamageType() != DamageType.PHYSICAL && attackerStats != null) {
            resistValue = Math.max(0, resistValue - attackerStats.getValue(RPGAttribute.MAGIC_PENETRATION));
        }
        double reduction = Math.min(resistValue / 100.0, 0.9);
        if (reduction > 0) {
            event.setModifiedAmount(event.getModifiedAmount() * (1.0 - reduction));
        }
    }
}
