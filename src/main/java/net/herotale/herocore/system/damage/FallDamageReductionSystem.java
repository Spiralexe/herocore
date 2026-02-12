package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.damage.DamageType;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

/**
 * Reduces fall damage based on the victim's {@code FALL_DAMAGE_REDUCTION} attribute.
 * Runs after attack-damage bonus so base scaling is already applied.
 */
@SystemOrder(after = "herocore:attack_damage_bonus")
public class FallDamageReductionSystem implements DamageSystem {

    private boolean enabled = true;

    @Override public String getId() { return "herocore:fall_damage_reduction"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (event.getDamageType() != DamageType.FALL) return;
        if (victimStats == null) return;
        double fallReduction = victimStats.getValue(RPGAttribute.FALL_DAMAGE_REDUCTION);
        if (fallReduction > 0) {
            double reduction = Math.min(fallReduction, 0.9);
            event.setModifiedAmount(event.getModifiedAmount() * (1.0 - reduction));
        }
    }
}
