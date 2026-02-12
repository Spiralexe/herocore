package net.herotale.herocore.system.heal;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.heal.HealEvent;
import net.herotale.herocore.api.heal.HealType;
import net.herotale.herocore.api.system.HealSystem;
import net.herotale.herocore.api.system.SystemOrder;

/**
 * Scales healing amounts by the healer's {@code HEALING_POWER} attribute.
 * Applies to spell and passive heals. Optionally scales regen ticks
 * (configurable).
 * Runs first among heal systems — no ordering dependencies.
 */
@SystemOrder
public class HealingPowerScalingSystem implements HealSystem {

    private final boolean scalesRegenTick;
    private boolean enabled = true;

    public HealingPowerScalingSystem(boolean scalesRegenTick) { this.scalesRegenTick = scalesRegenTick; }

    @Override public String getId() { return "herocore:healing_power_scaling"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onHeal(HealEvent event, StatsComponent healerStats, StatsComponent targetStats) {
        HealType type = event.getHealType();
        boolean shouldScale = (type == HealType.SPELL || type == HealType.PASSIVE);
        if (!shouldScale && type == HealType.REGEN_TICK && scalesRegenTick) { shouldScale = true; }
        if (shouldScale && healerStats != null) {
            double healingPower = healerStats.getValue(RPGAttribute.HEALING_POWER);
            event.setModifiedAmount(event.getModifiedAmount() * (1.0 + healingPower));
        }
    }
}
