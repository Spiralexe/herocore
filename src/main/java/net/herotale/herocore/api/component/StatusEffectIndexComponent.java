package net.herotale.herocore.api.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;

/**
 * ECS Component: Thin index of active status effects.
 * <p>
 * Tracks effect IDs, stack counts, and remaining duration in seconds (decremented
 * by {@code dt} each tick). The actual stat modifications live as
 * {@link com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier}
 * entries in {@code EntityStatMap} — this component only tracks what to expire.
 * <p>
 * <b>Not persistent</b> — registered with no codec. Effects reset on entity reload.
 */
public class StatusEffectIndexComponent implements Component<EntityStore> {

    private final Map<String, EffectEntry> activeEffects = new HashMap<>();

    /**
     * Duration is tracked as remaining seconds, decremented by dt each tick.
     * This keeps time measurement consistent with the world tick — no wall clock.
     */
    public static class EffectEntry {
        public final int stacks;
        public float remainingSeconds;

        public EffectEntry(int stacks, float durationSeconds) {
            this.stacks = stacks;
            this.remainingSeconds = durationSeconds;
        }

        public EffectEntry(EffectEntry other) {
            this.stacks = other.stacks;
            this.remainingSeconds = other.remainingSeconds;
        }
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

    public void addEffect(String id, int stacks, float durationSeconds) {
        activeEffects.put(id, new EffectEntry(stacks, durationSeconds));
    }

    public void removeEffect(String id) {
        activeEffects.remove(id);
    }

    public Map<String, EffectEntry> getActiveEffects() {
        return activeEffects;
    }
}
