package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

/**
 * Enforces a configurable minimum damage floor.
 * Runs last — after all other modifiers have had their say.
 */
@SystemOrder(after = "herocore:lifesteal")
public class MinimumDamageSystem implements DamageSystem {

    private final double minimumDamage;
    private boolean enabled = true;

    public MinimumDamageSystem(double minimumDamage) { this.minimumDamage = minimumDamage; }

    @Override public String getId() { return "herocore:minimum_damage"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        event.setModifiedAmount(Math.max(event.getModifiedAmount(), minimumDamage));
    }
}
