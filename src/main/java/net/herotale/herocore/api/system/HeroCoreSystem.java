package net.herotale.herocore.api.system;

/**
 * Base interface for all Hero-Core systems.
 * <p>
 * Systems are discrete, opt-in logic units. Each system performs one specific
 * operation (e.g., resistance mitigation, crit rolling, resource regen).
 * <p>
 * Other developers can disable any system and register their own replacement
 * while still using Hero-Core's shared components and events.
 */
public interface HeroCoreSystem {

    /** Unique namespaced identifier for this system (e.g., "herocore:resistance_mitigation"). */
    String getId();

    /** Whether this system is currently enabled. */
    boolean isEnabled();

    /** Enable or disable this system at runtime. */
    void setEnabled(boolean enabled);
}
