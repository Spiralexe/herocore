package net.herotale.herocore.system.heal;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.heal.HealEvent;
import net.herotale.herocore.api.system.HealSystem;
import net.herotale.herocore.api.system.SystemOrder;

/**
 * Applies the target's {@code HEALING_RECEIVED_BONUS} multiplier.
 * Runs after healing-power scaling so the bonus stacks multiplicatively.
 */
@SystemOrder(after = "herocore:healing_power_scaling")
public class HealingReceivedBonusSystem implements HealSystem {

    private boolean enabled = true;

    @Override public String getId() { return "herocore:healing_received_bonus"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onHeal(HealEvent event, StatsComponent healerStats, StatsComponent targetStats) {
        if (targetStats == null) return;
        double bonus = targetStats.getValue(RPGAttribute.HEALING_RECEIVED_BONUS);
        event.setModifiedAmount(event.getModifiedAmount() * (1.0 + bonus));
    }
}
