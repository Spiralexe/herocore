package net.herotale.herocore.api.event;

import com.hypixel.hytale.component.system.EcsEvent;

/**
 * ECS Event dispatched when an entity exits combat (combat timeout elapsed).
 * <p>
 * Fired by {@link net.herotale.herocore.system.combat.CombatTimeoutSystem}
 * via {@code cb.invoke(ref, new CombatExitEvent())}.
 */
public class CombatExitEvent extends EcsEvent {

    public CombatExitEvent() {}
}
