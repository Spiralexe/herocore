package net.herotale.herocore.api.event;

import com.hypixel.hytale.component.system.EcsEvent;

/**
 * ECS Event fired when an entity levels up.
 * <p>
 * Dispatched via {@code cb.invoke(entityRef, new LevelUpEvent(oldLevel, newLevel))}
 * through the native ECS event system — not a custom bus.
 */
public class LevelUpEvent extends EcsEvent {

    private final int oldLevel;
    private final int newLevel;

    public LevelUpEvent(int oldLevel, int newLevel) {
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
