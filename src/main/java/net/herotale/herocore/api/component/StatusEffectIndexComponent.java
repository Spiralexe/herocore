package net.herotale.herocore.api.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ECS Component: Thin index of active status effects.
 * <p>
 * Tracks effect IDs, stack counts, remaining duration in seconds, and
 * which {@code EntityStatMap} modifiers were applied. When an effect expires,
 * {@code StatusEffectTickSystem} owns full cleanup — it removes both the
 * index entry and all tracked modifier keys from the entity's stat map.
 * <p>
 * <b>Not persistent</b> — registered with no codec. Effects reset on entity reload.
 */
public class StatusEffectIndexComponent implements Component<EntityStore> {

    private final Map<String, EffectEntry> activeEffects = new HashMap<>();

    /**
     * Tracks a single active effect: stacks, remaining duration, and the
     * modifier keys that were written to EntityStatMap when the effect was applied.
     * <p>
     * Duration is measured in seconds, decremented by {@code dt} each tick.
     */
    public static class EffectEntry {
        public final int stacks;
        public float remainingSeconds;
        private final List<ModifierRef> modifierRefs = new ArrayList<>();

        public EffectEntry(int stacks, float durationSeconds) {
            this.stacks = stacks;
            this.remainingSeconds = durationSeconds;
        }

        public EffectEntry(EffectEntry other) {
            this.stacks = other.stacks;
            this.remainingSeconds = other.remainingSeconds;
            this.modifierRefs.addAll(other.modifierRefs);
        }

        /**
         * Register a modifier that this effect wrote to EntityStatMap.
         * StatusEffectTickSystem will remove it automatically on expiry.
         *
         * @param statIndex   the stat type index the modifier was applied to
         * @param modifierKey the key passed to {@code EntityStatMap.putModifier()}
         */
        public void trackModifier(int statIndex, String modifierKey) {
            modifierRefs.add(new ModifierRef(statIndex, modifierKey));
        }

        /** All modifier references tracked for this effect. */
        public List<ModifierRef> getModifierRefs() {
            return modifierRefs;
        }

        /**
         * A stat index + modifier key pair, representing one modifier entry
         * in EntityStatMap that should be removed when this effect expires.
         */
        public record ModifierRef(int statIndex, String modifierKey) {}
    }

    /** Default constructor required by registration factory. */
    public StatusEffectIndexComponent() {}

    /** Copy constructor required by {@link #clone()}. */
    public StatusEffectIndexComponent(StatusEffectIndexComponent other) {
        for (var e : other.activeEffects.entrySet()) {
            activeEffects.put(e.getKey(), new EffectEntry(e.getValue()));
        }
    }

    @Override
    public Component<EntityStore> clone() {
        return new StatusEffectIndexComponent(this);
    }

    // ── Static ComponentType handle ──────────────────────────────────
    private static ComponentType<EntityStore, StatusEffectIndexComponent> type;

    public static ComponentType<EntityStore, StatusEffectIndexComponent> getComponentType() {
        return type;
    }

    public static void setComponentType(ComponentType<EntityStore, StatusEffectIndexComponent> t) {
        type = t;
    }

    // ── API ──────────────────────────────────────────────────────────

    /**
     * Add or overwrite an active effect.
     *
     * @param id              effect identifier (e.g., "burn", "fortify")
     * @param stacks          stack count
     * @param durationSeconds duration in seconds (-1 for permanent)
     */
    public void addEffect(String id, int stacks, float durationSeconds) {
        activeEffects.put(id, new EffectEntry(stacks, durationSeconds));
    }

    /**
     * Get an active effect entry by ID, for registering modifier refs after adding.
     *
     * @param id the effect ID
     * @return the entry, or null if not active
     */
    public EffectEntry getEffect(String id) {
        return activeEffects.get(id);
    }

    public void removeEffect(String id) {
        activeEffects.remove(id);
    }

    public Map<String, EffectEntry> getActiveEffects() {
        return activeEffects;
    }
}
