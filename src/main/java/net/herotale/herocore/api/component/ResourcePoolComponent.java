package net.herotale.herocore.api.component;

import net.herotale.herocore.api.resource.ResourceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * ECS Component: Per-entity resource pool storage.
 * <p>
 * Tracks current values for Health, Mana, Stamina, Oxygen, and Ammo.
 * Max values are derived from {@link StatsComponent} attributes.
 * <p>
 * This replaces the old {@code ResourceRegenManager}'s internal {@code PlayerResources}.
 */
public class ResourcePoolComponent {

    private final Map<ResourceType, Double> currentValues = new EnumMap<>(ResourceType.class);

    public ResourcePoolComponent() {
        for (ResourceType type : ResourceType.values()) {
            currentValues.put(type, 0.0);
        }
    }

    /** Get the current value of a resource. */
    public double get(ResourceType type) {
        return currentValues.getOrDefault(type, 0.0);
    }

    /** Set the current value of a resource. */
    public void set(ResourceType type, double value) {
        currentValues.put(type, Math.max(0.0, value));
    }

    /** Add to a resource (clamped to 0 minimum). */
    public void add(ResourceType type, double amount) {
        set(type, get(type) + amount);
    }

    /** Subtract from a resource (clamped to 0 minimum). */
    public void subtract(ResourceType type, double amount) {
        set(type, get(type) - amount);
    }

    /** Clamp a resource to a maximum value. */
    public void clampToMax(ResourceType type, double max) {
        if (get(type) > max) {
            set(type, max);
        }
    }

    // ── Convenience accessors ────────────────────────────────────────

    public double getHealth()  { return get(ResourceType.HEALTH); }
    public double getMana()    { return get(ResourceType.MANA); }
    public double getStamina() { return get(ResourceType.STAMINA); }
    public double getOxygen()  { return get(ResourceType.OXYGEN); }
    public double getAmmo()    { return get(ResourceType.AMMO); }

    public void setHealth(double v)  { set(ResourceType.HEALTH, v); }
    public void setMana(double v)    { set(ResourceType.MANA, v); }
    public void setStamina(double v) { set(ResourceType.STAMINA, v); }
    public void setOxygen(double v)  { set(ResourceType.OXYGEN, v); }
    public void setAmmo(double v)    { set(ResourceType.AMMO, v); }
}
