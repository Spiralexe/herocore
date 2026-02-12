package net.herotale.herocore.api.component;

/**
 * ECS Component: Tracks an entity's combat state.
 * <p>
 * Determines whether an entity is "in combat" for resource regen bonuses,
 * stamina delay, and other combat-dependent mechanics.
 */
public class CombatStateComponent {

    private long lastCombatTimestamp;
    private long staminaDelayUntil;
    private boolean forcedOutOfCombat;
    private final long combatTimeoutMs;

    public CombatStateComponent(long combatTimeoutMs) {
        this.combatTimeoutMs = combatTimeoutMs;
        this.lastCombatTimestamp = 0;
        this.staminaDelayUntil = 0;
        this.forcedOutOfCombat = false;
    }

    /** Mark the entity as entering combat now. */
    public void enterCombat() {
        lastCombatTimestamp = System.currentTimeMillis();
        forcedOutOfCombat = false;
    }

    /** Force the entity out of combat immediately (e.g., meditate skill). */
    public void forceOutOfCombat() {
        forcedOutOfCombat = true;
    }

    /** Check if the entity is currently in combat. */
    public boolean isInCombat() {
        if (forcedOutOfCombat) return false;
        if (lastCombatTimestamp == 0) return false;
        return (System.currentTimeMillis() - lastCombatTimestamp) < combatTimeoutMs;
    }

    /** Get remaining combat timeout in milliseconds (0 if out of combat). */
    public long getCombatTimeoutRemaining() {
        if (forcedOutOfCombat) return 0;
        if (lastCombatTimestamp == 0) return 0;
        long remaining = combatTimeoutMs - (System.currentTimeMillis() - lastCombatTimestamp);
        return Math.max(0, remaining);
    }

    /** Trigger stamina regen delay. */
    public void triggerStaminaDelay(long delayMs) {
        staminaDelayUntil = System.currentTimeMillis() + delayMs;
    }

    /** Check if stamina regen is currently delayed. */
    public boolean isStaminaRegenDelayed() {
        return System.currentTimeMillis() < staminaDelayUntil;
    }

    public long getCombatTimeoutMs() {
        return combatTimeoutMs;
    }
}
