package net.herotale.herocore.api.event;

import com.hypixel.hytale.component.system.EcsEvent;

/**
 * ECS Event fired when an entity loses levels.
 * <p>
 * Dispatched via {@code cb.invoke(entityRef, new LevelDownEvent(oldLevel, newLevel))}
 * through the native ECS event system.
 */
public class LevelDownEvent extends EcsEvent {

    private final int oldLevel;
    private final int newLevel;

    public LevelDownEvent(int oldLevel, int newLevel) {
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
