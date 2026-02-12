package net.herotale.herocore.api.event;

import net.herotale.herocore.api.HeroCore;

/**
 * Lifecycle event fired when HeroCore has finished initialization.
 * <p>
 * Subscribe to this event to safely access {@link HeroCore#get()} and all
 * registries. This is the signal that the HeroCore API is fully wired and
 * ready for use.
 * <p>
 * <b>Usage in downstream plugins:</b>
 * <pre>{@code
 * // In your plugin's setup(), subscribe BEFORE HeroCore fires the event:
 * HeroCore.get().getEventBus().subscribe(HeroCoreReadyEvent.class, event -> {
 *     LevelingRegistry leveling = event.getHeroCore().getLevelingRegistry();
 *     // safe to use all registries here
 * });
 * }</pre>
 * <p>
 * If your plugin loads after HeroCore (because it declares HeroCore as a
 * dependency in manifest.json), {@link HeroCore#isReady()} will already
 * return true by the time your {@code setup()} runs — so you can use the
 * API immediately without waiting for this event. The event is primarily
 * useful for optional dependencies or lazy initialization.
 */
public class HeroCoreReadyEvent {

    private final HeroCore heroCore;

    public HeroCoreReadyEvent(HeroCore heroCore) {
        this.heroCore = heroCore;
    }

    /**
     * The fully initialized HeroCore API instance.
     */
    public HeroCore getHeroCore() {
        return heroCore;
    }
}
