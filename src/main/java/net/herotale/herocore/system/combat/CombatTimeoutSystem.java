package net.herotale.herocore.system.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.event.CombatExitEvent;

/**
 * Ticks combat state for entities, dispatching a {@link CombatExitEvent}
 * when the combat timeout elapses.
 * <p>
 * Extends {@link DelayedSystem} — fires at a configurable interval (default 0.5s),
 * not every tick, to reduce overhead.
 */
public class CombatTimeoutSystem extends DelayedSystem<EntityStore> {

    private final float combatTimeoutSeconds;

    /**
     * @param combatTimeoutSeconds seconds of no damage before exiting combat
     */
    public CombatTimeoutSystem(float combatTimeoutSeconds) {
        super(0.5f); // check every 0.5 seconds
        this.combatTimeoutSeconds = combatTimeoutSeconds;
    }

    @Override
    public void delayedTick(float dt, int systemIndex, Store<EntityStore> store) {
        // Iterate entity chunks matching CombatStateComponent query
        store.forEachChunk(CombatStateComponent.getComponentType(),
                (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> cmd) -> {
            for (int i = 0; i < chunk.size(); i++) {
                CombatStateComponent combat = chunk.getComponent(i, CombatStateComponent.getComponentType());
                if (combat == null || !combat.isInCombat()) continue;

                combat.tickElapsed(dt);

                if (combat.getSecondsSinceLastDamage() > combatTimeoutSeconds) {
                    combat.setInCombat(false);
                    // Dispatch CombatExitEvent via CommandBuffer
                    cmd.invoke(chunk.getReferenceTo(i), new CombatExitEvent());
                }
            }
        });
    }
}
