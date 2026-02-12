package net.herotale.herocore.api.crafting;

import java.util.Objects;

/**
 * A namespaced reference to an item definition, used as a lightweight proxy
 * for Hytale's ItemStack until the official API exposes one.
 * Wraps a string key (e.g., "heroes:iron_ingot") and an amount.
 */
public record ItemRef(String key, int amount) {

    public ItemRef {
        Objects.requireNonNull(key, "ItemRef key must not be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("ItemRef key must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("ItemRef amount must be >= 0");
        }
    }

    /**
     * Create an ItemRef with a default amount of 1.
     */
    public ItemRef(String key) {
        this(key, 1);
    }

    @Override
    public String toString() {
        return key + "x" + amount;
    }
}
