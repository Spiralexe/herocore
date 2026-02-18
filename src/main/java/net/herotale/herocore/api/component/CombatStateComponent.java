package net.herotale.herocore.api.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * ECS Component: Tracks an entity's combat state.
 * <p>
 * Determines whether an entity is "in combat" for resource regen bonuses,
 * stamina delay, and other combat-dependent mechanics.
 * <p>
 * Duration is tracked as {@code secondsSinceLastDamage} accumulated via {@code dt}
 * from the world tick — no wall clock ({@code System.currentTimeMillis()}).
 * This keeps time measurement consistent with the ECS tick system.
 * <p>
 * <b>Not persistent</b> — registered with {@code null} codec. Combat state
 * resets on entity reload.
 */
public class CombatStateComponent implements Component<EntityStore> {

    private boolean inCombat = false;
    private float secondsSinceLastDamage = 0f;

    /** Default constructor required by registration factory. */
    public CombatStateComponent() {}

    /** Copy constructor required by {@link #clone()}. */
    public CombatStateComponent(CombatStateComponent other) {
        this.inCombat = other.inCombat;
        this.secondsSinceLastDamage = other.secondsSinceLastDamage;
    }

    @Override
    public Component<EntityStore> clone() {
        return new CombatStateComponent(this);
    }

    // ── Static ComponentType handle ──────────────────────────────────
    private static ComponentType<EntityStore, CombatStateComponent> type;

    public static ComponentType<EntityStore, CombatStateComponent> getComponentType() {
        return type;
    }

    public static void setComponentType(ComponentType<EntityStore, CombatStateComponent> t) {
        type = t;
    }

    // ── API ──────────────────────────────────────────────────────────

    public boolean isInCombat() {
        return inCombat;
    }

    public void enterCombat() {
        inCombat = true;
        secondsSinceLastDamage = 0f;
    }

    /**
     * Accumulate elapsed time from the world tick.
     * Called by {@code CombatTimeoutSystem} each tick.
     *
     * @param dt delta time in seconds from the world tick
     */
    public void tickElapsed(float dt) {
        if (inCombat) {
            secondsSinceLastDamage += dt;
        }
    }

    public float getSecondsSinceLastDamage() {
        return secondsSinceLastDamage;
    }

    public void setInCombat(boolean v) {
        inCombat = v;
    }
}
