package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

/**
 * Adds base attack-damage scaling from the attacker's {@code ATTACK_DAMAGE} attribute.
 * Runs first among damage systems — no ordering dependencies.
 */
@SystemOrder
public class AttackDamageBonusSystem implements DamageSystem {

    private boolean enabled = true;

    @Override public String getId() { return "herocore:attack_damage_bonus"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (attackerStats == null) return;
        double damageBonus = attackerStats.getValue(RPGAttribute.ATTACK_DAMAGE);
        if (damageBonus > 0) {
            event.setModifiedAmount(event.getModifiedAmount() * (1.0 + damageBonus));
        }
    }
}
