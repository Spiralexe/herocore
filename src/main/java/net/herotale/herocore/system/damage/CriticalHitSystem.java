package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.damage.DamageFlag;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

import java.util.Random;

/**
 * Rolls critical hits based on the attacker's {@code CRIT_CHANCE} and
 * applies the {@code CRIT_DAMAGE_MULTIPLIER}.
 * Runs after resistance mitigation so the crit multiplies post-armor damage.
 */
@SystemOrder(after = "herocore:resistance_mitigation")
public class CriticalHitSystem implements DamageSystem {

    private final double fallbackCritMultiplier;
    private final Random random;
    private boolean enabled = true;

    public CriticalHitSystem(double fallbackCritMultiplier) { this(fallbackCritMultiplier, new Random()); }
    public CriticalHitSystem(double fallbackCritMultiplier, Random random) {
        this.fallbackCritMultiplier = fallbackCritMultiplier;
        this.random = random;
    }

    @Override public String getId() { return "herocore:critical_hit"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (attackerStats == null) return;
        double critChance = attackerStats.getValue(RPGAttribute.CRIT_CHANCE);
        if (critChance > 0 && random.nextDouble() < critChance) {
            double critMultiplier = attackerStats.getValue(RPGAttribute.CRIT_DAMAGE_MULTIPLIER);
            if (critMultiplier <= 0) { critMultiplier = fallbackCritMultiplier; }
            event.setModifiedAmount(event.getModifiedAmount() * critMultiplier);
            event.addFlag(DamageFlag.CRIT);
        }
    }
}
