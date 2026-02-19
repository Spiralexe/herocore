package net.herotale.herocore.system.combat;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.event.CombatExitEvent;

/**
 * Ticks combat state for entities, dispatching a {@link CombatExitEvent}
 * when the combat timeout elapses.
 * <p>
 * Extends {@link DelayedEntitySystem} — fires at a configurable interval (default 0.5s),
 * not every tick, to reduce overhead. Intentionally slower than
 * {@link StatusEffectTickSystem}'s 0.25s — combat timeout doesn't need
 * sub-second precision.
 */
public class CombatTimeoutSystem extends DelayedEntitySystem<EntityStore> {

    private final float combatTimeoutSeconds;

    /**
     * @param combatTimeoutSeconds seconds of no damage before exiting combat
     */
    public CombatTimeoutSystem(float combatTimeoutSeconds) {
        super(0.5f); // check every 0.5 seconds
        this.combatTimeoutSeconds = combatTimeoutSeconds;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return CombatStateComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        CombatStateComponent combat = chunk.getComponent(index, CombatStateComponent.getComponentType());
        if (combat == null || !combat.isInCombat()) return;

        combat.tickElapsed(dt);

        if (combat.getSecondsSinceLastDamage() > combatTimeoutSeconds) {
            combat.setInCombat(false);
            // Dispatch CombatExitEvent via CommandBuffer
            cb.invoke(chunk.getReferenceTo(index), new CombatExitEvent());
        }
    }
}
