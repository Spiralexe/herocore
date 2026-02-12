package net.herotale.herocore.api.event;

import net.herotale.herocore.api.HeroCore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight typed event bus for HeroCore lifecycle and game events.
 * <p>
 * This is intentionally simple — not a general-purpose event framework.
 * It dispatches HeroCore-specific events to subscribers registered by
 * downstream plugins.
 * <p>
 * Supported event types:
 * <ul>
 *   <li>{@link HeroCoreReadyEvent} — HeroCore has finished initialization</li>
 *   <li>{@link LevelUpEvent} — a player leveled up</li>
 *   <li>{@link LevelDownEvent} — a player leveled down</li>
 *   <li>{@link AttributeChangeEvent} — an attribute value changed</li>
 *   <li>{@link ResourceChangeEvent} — a resource pool value changed</li>
 *   <li>{@link StatusChangeEvent} — a status effect was applied/removed</li>
 *   <li>{@link MobSpawnEvent} — a mob spawned</li>
 *   <li>{@link MobDeathEvent} — a mob died</li>
 * </ul>
 * <p>
 * <b>Usage (named subscriptions):</b>
 * <pre>{@code
 * // Subscribe with a named ID (recommended — easy to unsubscribe)
 * HeroCoreEventBus.get().on(LevelUpEvent.class, event -> {
 *     getLogger().info("Player leveled up to " + event.getNewLevel());
 * }, "heroes:level-up");
 *
 * // Unsubscribe by name
 * HeroCoreEventBus.get().off(LevelUpEvent.class, "heroes:level-up");
 * }</pre>
 * <p>
 * <b>Usage (anonymous subscriptions):</b>
 * <pre>{@code
 * HeroCoreEventBus bus = HeroCore.get().getEventBus();
 * bus.subscribe(LevelUpEvent.class, event -> { ... });
 * }</pre>
 * <p>
 * Thread safety: subscribers are stored in {@link CopyOnWriteArrayList} so
 * registration is safe from any thread. Firing dispatches synchronously on
 * the caller's thread.
 */
public class HeroCoreEventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, NamedSubscription<?>> namedSubscriptions = new ConcurrentHashMap<>();

    // ── Static accessor ──────────────────────────────────────────────

    /**
     * Convenience static accessor. Equivalent to {@code HeroCore.get().getEventBus()}.
     *
     * @return the singleton event bus
     * @throws IllegalStateException if HeroCore has not initialized yet
     */
    public static HeroCoreEventBus get() {
        return HeroCore.get().getEventBus();
    }

    /**
     * Subscribe to events of a specific type.
     *
     * @param eventType the event class to listen for
     * @param handler   the handler that receives events of this type
     * @param <T>       the event type
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /**
     * Unsubscribe a specific handler from an event type.
     *
     * @param eventType the event class
     * @param handler   the handler to remove
     * @param <T>       the event type
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<?>> handlers = subscribers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Fire an event, dispatching to all registered subscribers.
     * Dispatch is synchronous on the calling thread.
     *
     * @param event the event instance to fire
     * @param <T>   the event type
     */
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        List<Consumer<?>> handlers = subscribers.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) return;
        for (Consumer<?> handler : handlers) {
            ((Consumer<T>) handler).accept(event);
        }
    }

    /**
     * Remove all subscribers for a specific event type.
     * Useful for plugin hot-reload cleanup.
     *
     * @param eventType the event class
     */
    public void clearSubscribers(Class<?> eventType) {
        subscribers.remove(eventType);
    }

    /**
     * Remove all subscribers for all event types.
     */
    public void clearAll() {
        subscribers.clear();
    }

    /**
     * Create a {@link Consumer} sink that fires events on this bus.
     * Useful for wiring into HeroCore services that accept injected consumers.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Consumer<LevelUpEvent> sink = eventBus.asSink(LevelUpEvent.class);
     * // Pass 'sink' to LevelingRegistryImpl constructor
     * }</pre>
     *
     * @param eventType the event class (for type safety)
     * @param <T>       the event type
     * @return a consumer that fires events on this bus
     */
    public <T> Consumer<T> asSink(Class<T> eventType) {
        return this::fire;
    }

    // ── Named subscriptions ──────────────────────────────────────────

    /**
     * Subscribe to events with a named ID. The ID allows easy unsubscription
     * without holding a reference to the consumer.
     * <p>
     * If a subscription with the same {@code subscriptionId} already exists,
     * the old one is replaced.
     *
     * @param eventType      the event class to listen for
     * @param handler        the handler that receives events
     * @param subscriptionId unique ID for this subscription (e.g., {@code "heroes:level-up"})
     * @param <T>            the event type
     */
    @SuppressWarnings("unchecked")
    public <T> void on(Class<T> eventType, Consumer<T> handler, String subscriptionId) {
        // Remove existing subscription with same ID if present
        off(eventType, subscriptionId);
        namedSubscriptions.put(subscriptionId, new NamedSubscription<>(eventType, handler));
        subscribe(eventType, handler);
    }

    /**
     * Unsubscribe a named subscription by its ID.
     *
     * @param eventType      the event class
     * @param subscriptionId the subscription ID used in {@link #on}
     * @param <T>            the event type
     */
    @SuppressWarnings("unchecked")
    public <T> void off(Class<T> eventType, String subscriptionId) {
        NamedSubscription<?> sub = namedSubscriptions.remove(subscriptionId);
        if (sub != null && sub.eventType().equals(eventType)) {
            unsubscribe(eventType, (Consumer<T>) sub.handler());
        }
    }

    // ── Internal ─────────────────────────────────────────────────────

    private record NamedSubscription<T>(Class<T> eventType, Consumer<T> handler) {}
}
