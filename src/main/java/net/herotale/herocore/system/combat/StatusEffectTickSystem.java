package net.herotale.herocore.system.combat;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;

import net.herotale.herocore.api.component.StatusEffectIndexComponent;
import net.herotale.herocore.api.component.StatusEffectIndexComponent.EffectEntry;

import java.util.Iterator;
import java.util.Map;

/**
 * {@link DelayedEntitySystem} that ticks status effect durations down and removes
 * expired effects. This system <b>owns full cleanup</b>: when an effect expires,
 * it removes both the index entry and all tracked {@code StaticModifier} entries
 * from the entity's {@code EntityStatMap}.
 * <p>
 * Fires every 0.25 seconds (intentional — provides responsive effect expiry
 * without per-tick overhead). The combat timeout system fires at 0.5s for
 * lower-frequency checks.
 * <p>
 * <b>Cleanup contract:</b> When applying an effect, callers must register
 * modifier keys via {@link EffectEntry#trackModifier(int, String)} so this
 * system knows which modifiers to remove on expiry.
 */
public class StatusEffectTickSystem extends DelayedEntitySystem<EntityStore> {

    public StatusEffectTickSystem() {
        super(0.25f); // tick every 0.25 seconds
    }

    @Override
    public Query<EntityStore> getQuery() {
        return StatusEffectIndexComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        StatusEffectIndexComponent idx = chunk.getComponent(
                index, StatusEffectIndexComponent.getComponentType());
        if (idx == null) return;

        Map<String, EffectEntry> effects = idx.getActiveEffects();
        if (effects.isEmpty()) return;

        // Lazy-loaded — only fetched if an effect actually expires
        EntityStatMap statMap = null;

        Iterator<Map.Entry<String, EffectEntry>> it = effects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EffectEntry> entry = it.next();
            EffectEntry effect = entry.getValue();

            effect.remainingSeconds -= dt;

            if (effect.remainingSeconds <= 0f) {
                // Effect expired — remove all its tracked modifiers from EntityStatMap
                if (!effect.getModifierRefs().isEmpty()) {
                    if (statMap == null) {
                        statMap = store.getComponent(
                                chunk.getReferenceTo(index),
                                EntityStatsModule.get().getEntityStatMapComponentType());
                    }
                    if (statMap != null) {
                        removeEffectModifiers(statMap, effect);
                    }
                }
                it.remove();
            }
        }
    }

    /**
     * Removes all modifier entries that were tracked by this effect.
     * Each {@link EffectEntry.ModifierRef} contains the stat index and key
     * needed to call {@code EntityStatMap.removeModifier()}.
     */
    private void removeEffectModifiers(EntityStatMap statMap, EffectEntry effect) {
        for (EffectEntry.ModifierRef ref : effect.getModifierRefs()) {
            statMap.removeModifier(ref.statIndex(), ref.modifierKey());
        }
    }
}
