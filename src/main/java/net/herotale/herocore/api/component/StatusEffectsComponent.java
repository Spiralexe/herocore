package net.herotale.herocore.api.component;

import net.herotale.herocore.api.status.StatusEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ECS Component: Per-entity status effect storage.
 * <p>
 * Tracks active status effects with duration, stacks, and source.
 * Does <b>not</b> define what a status does — behavior is implemented
 * by the plugin that applies it. This component is data only.
 * <p>
 * Replaces the old {@code StatusRegistry} singleton.
 */
public class StatusEffectsComponent {

    private final Map<String, ActiveStatus> statuses = new ConcurrentHashMap<>();

    /** Apply a status effect. If already present, refreshes duration and adds stacks. */
    public void apply(StatusEffect effect) {
        ActiveStatus existing = statuses.get(effect.getId());
        if (existing != null) {
            existing.expiresAt = effect.isPermanent() ? -1
                    : System.currentTimeMillis() + effect.getDurationMs();
            existing.stacks += effect.getStacks();
            existing.source = effect.getSource();
        } else {
            ActiveStatus active = new ActiveStatus();
            active.source = effect.getSource();
            active.stacks = effect.getStacks();
            active.expiresAt = effect.isPermanent() ? -1
                    : System.currentTimeMillis() + effect.getDurationMs();
            statuses.put(effect.getId(), active);
        }
    }

    /** Check if a status is active. */
    public boolean has(String statusId) {
        ActiveStatus active = statuses.get(statusId);
        if (active == null) return false;
        if (active.expiresAt >= 0 && System.currentTimeMillis() >= active.expiresAt) {
            statuses.remove(statusId);
            return false;
        }
        return true;
    }

    /** Get the current stack count (0 if not present). */
    public int getStacks(String statusId) {
        if (!has(statusId)) return 0;
        ActiveStatus active = statuses.get(statusId);
        return active != null ? active.stacks : 0;
    }

    /** Remove a specific status. */
    public void remove(String statusId) {
        statuses.remove(statusId);
    }

    /** Remove all statuses from a specific source. */
    public void removeBySource(String source) {
        statuses.entrySet().removeIf(e -> e.getValue().source.equals(source));
    }

    /** Remove all statuses. */
    public void clearAll() {
        statuses.clear();
    }

    /** Get all currently active status effects. */
    public Collection<StatusEffect> getActiveEffects() {
        long now = System.currentTimeMillis();
        List<StatusEffect> effects = new ArrayList<>();
        for (Map.Entry<String, ActiveStatus> entry : statuses.entrySet()) {
            ActiveStatus active = entry.getValue();
            if (active.expiresAt >= 0 && now >= active.expiresAt) continue;
            long duration = active.expiresAt < 0 ? -1 : Math.max(0, active.expiresAt - now);
            effects.add(StatusEffect.builder()
                    .id(entry.getKey())
                    .source(active.source)
                    .stacks(active.stacks)
                    .duration(duration)
                    .build());
        }
        return effects;
    }

    /** Tick to expire statuses. Call periodically from StatusExpirySystem. */
    public void tick() {
        long now = System.currentTimeMillis();
        statuses.entrySet().removeIf(e -> {
            ActiveStatus as = e.getValue();
            return as.expiresAt >= 0 && now >= as.expiresAt;
        });
    }

    private static class ActiveStatus {
        String source;
        int stacks;
        long expiresAt; // -1 = permanent
    }
}
