package net.herotale.herocore.system.heal;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.heal.HealEvent;
import net.herotale.herocore.api.system.HealSystem;
import net.herotale.herocore.api.system.SystemOrder;

import java.util.Random;

/**
 * Rolls heal crits based on the healer's {@code HEAL_CRIT_CHANCE} and
 * applies the {@code HEAL_CRIT_MULTIPLIER}.
 * Runs last among heal systems — after all scaling is applied.
 */
@SystemOrder(after = "herocore:healing_received_bonus")
public class HealCritSystem implements HealSystem {

    private final Random random;
    private boolean enabled;

    public HealCritSystem(boolean enabledByDefault) { this(enabledByDefault, new Random()); }
    public HealCritSystem(boolean enabledByDefault, Random random) {
        this.enabled = enabledByDefault;
        this.random = random;
    }

    @Override public String getId() { return "herocore:heal_crit"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onHeal(HealEvent event, StatsComponent healerStats, StatsComponent targetStats) {
        StatsComponent source = healerStats != null ? healerStats : targetStats;
        if (source == null) return;
        double healCritChance = source.getValue(RPGAttribute.HEAL_CRIT_CHANCE);
        if (healCritChance > 0 && random.nextDouble() < healCritChance) {
            double healCritMultiplier = source.getValue(RPGAttribute.HEAL_CRIT_MULTIPLIER);
            if (healCritMultiplier <= 0) { healCritMultiplier = 1.5; }
            event.setModifiedAmount(event.getModifiedAmount() * healCritMultiplier);
            event.setCrit(true);
        }
    }
}
