package net.herotale.herocore.system.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;

import net.herotale.herocore.api.component.StatusEffectIndexComponent;
import net.herotale.herocore.api.component.StatusEffectIndexComponent.EffectEntry;

import java.util.Iterator;
import java.util.Map;

/**
 * DelayedSystem that ticks status effect durations down and removes expired
 * effects. This system <b>owns full cleanup</b>: when an effect expires, it
 * removes both the index entry and all tracked {@code StaticModifier} entries
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
public class StatusEffectTickSystem extends DelayedSystem<EntityStore> {

    public StatusEffectTickSystem() {
        super(0.25f); // tick every 0.25 seconds
    }

    @Override
    public void delayedTick(float dt, int systemIndex, Store<EntityStore> store) {
        store.forEachChunk(StatusEffectIndexComponent.getComponentType(),
                (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> cmd) -> {
            for (int i = 0; i < chunk.size(); i++) {
                StatusEffectIndexComponent index = chunk.getComponent(
                        i, StatusEffectIndexComponent.getComponentType());
                if (index == null) continue;

                Map<String, EffectEntry> effects = index.getActiveEffects();
                if (effects.isEmpty()) continue;

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
                                        chunk.getReferenceTo(i),
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
        });
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
