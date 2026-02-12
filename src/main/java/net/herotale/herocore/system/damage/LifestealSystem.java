package net.herotale.herocore.system.damage;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.heal.HealEvent;
import net.herotale.herocore.api.heal.HealType;
import net.herotale.herocore.api.system.DamageSystem;
import net.herotale.herocore.api.system.SystemOrder;

import java.util.function.Consumer;

/**
 * Heals the attacker for a percentage of final damage based on the
 * {@code LIFESTEAL} attribute.  Posts a {@link HealEvent} via the
 * provided consumer (which should post to the entity event bus).
 * Runs after critical-hit so lifesteal is calculated on crit-amplified damage.
 */
@SystemOrder(after = "herocore:critical_hit")
public class LifestealSystem implements DamageSystem {

    private final Consumer<HealEvent> healEventConsumer;
    private boolean enabled = true;

    public LifestealSystem(Consumer<HealEvent> healEventConsumer) { this.healEventConsumer = healEventConsumer; }

    @Override public String getId() { return "herocore:lifesteal"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats) {
        if (attackerStats == null) return;
        if (event.getAttacker() == null) return;
        double lifesteal = attackerStats.getValue(RPGAttribute.LIFESTEAL);
        if (lifesteal <= 0) return;
        double healAmount = event.getModifiedAmount() * lifesteal;
        if (healAmount > 0 && healEventConsumer != null) {
            HealEvent healEvent = HealEvent.builder()
                    .healer(event.getAttacker())
                    .target(event.getAttacker())
                    .rawAmount(healAmount)
                    .healType(HealType.PASSIVE)
                    .build();
            healEventConsumer.accept(healEvent);
        }
    }
}
