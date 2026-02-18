package net.herotale.herocore.impl.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.api.component.StatusEffectIndexComponent;
import net.herotale.herocore.impl.HeroCoreComponentRegistry;

import java.util.logging.Level;

/**
 * Ensures every entity that enters the store gets the required HeroCore
 * components attached with sensible defaults.
 * <p>
 * Extends Hytale's native {@link HolderSystem} so it fires automatically
 * when entities are added to or removed from the entity store — no manual
 * event listener registration is needed.
 */
public class HeroCoreSetupSystem extends HolderSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return null;
    }

    @Override
    public void onEntityAdd(Holder<EntityStore> holder, AddReason reason, Store<EntityStore> store) {
        // Ensure all HeroCore components exist — ensureComponent adds only if absent
        holder.ensureComponent(HeroCoreComponentRegistry.HERO_CORE_STATS);
        holder.ensureComponent(HeroCoreComponentRegistry.HERO_CORE_PROGRESSION);
        holder.ensureComponent(HeroCoreComponentRegistry.COMBAT_STATE);
        holder.ensureComponent(HeroCoreComponentRegistry.STATUS_EFFECT_INDEX);

        if (LOGGER.at(Level.FINE).isEnabled()) {
            LOGGER.at(Level.FINE).log("HeroCoreSetupSystem: ensured components for entity (reason=%s)", reason);
        }
    }

    @Override
    public void onEntityRemoved(Holder<EntityStore> holder, RemoveReason reason, Store<EntityStore> store) {
        // No cleanup needed — the store lifecycle handles component removal
    }
}
