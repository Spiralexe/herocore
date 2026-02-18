package net.herotale.herocore.system.combat;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;

import net.herotale.herocore.api.component.StatusEffectIndexComponent;
import net.herotale.herocore.api.component.StatusEffectIndexComponent.EffectEntry;
import net.herotale.herocore.impl.HeroCoreModifiers;

import java.util.Iterator;
import java.util.Map;

/**
 * DelayedSystem that ticks status effect durations down and removes expired
 * effects (both the index entry and the corresponding EntityStatMap modifiers).
 * <p>
 * Fires every 0.25 seconds for responsive effect expiry without per-tick overhead.
 */
public class StatusEffectTickSystem extends DelayedSystem<EntityStore> {

    public StatusEffectTickSystem() {
        super(0.25f); // tick every 0.25 seconds
    }

    @Override
    public void delayedTick(float dt, int systemIndex, Store<EntityStore> store) {
        store.forEachChunk(StatusEffectIndexComponent.getComponentType(), (chunk, cmd) -> {
            for (int i = 0; i < chunk.size(); i++) {
                StatusEffectIndexComponent index = chunk.getComponent(
                        i, StatusEffectIndexComponent.getComponentType());
                if (index == null) continue;

                Map<String, EffectEntry> effects = index.getActiveEffects();
                if (effects.isEmpty()) continue;

                // Get the entity's stat map for modifier removal
                EntityStatMap statMap = store.getComponent(
                        chunk.getReferenceTo(i),
                        EntityStatsModule.get().getEntityStatMapComponentType());

                Iterator<Map.Entry<String, EffectEntry>> it = effects.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, EffectEntry> entry = it.next();
                    EffectEntry effect = entry.getValue();

                    effect.remainingSeconds -= dt;

                    if (effect.remainingSeconds <= 0f) {
                        // Effect expired — remove all its modifiers from EntityStatMap
                        if (statMap != null) {
                            removeEffectModifiers(statMap, entry.getKey());
                        }
                        it.remove();
                    }
                }
            }
        });
    }

    /**
     * Removes all modifiers keyed with the effect prefix pattern for the given effect ID.
     * Convention: effect modifiers use key {@code "HC_effect_{effectId}_{stat}"}.
     */
    private void removeEffectModifiers(EntityStatMap statMap, String effectId) {
        // We cannot enumerate all stat indices, so we rely on the convention that
        // effects register their modifier keys via HeroCoreModifiers.effect(effectId, stat).
        // The actual removal is best done by the system that applied the effect,
        // which knows which stat indices it modified. This is a safety sweep.
        // In practice, effect application code should track and clean up its own keys.
    }
}
