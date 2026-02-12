package net.herotale.herocore.api.crafting;

import java.util.Objects;

/**
 * A namespaced reference to a recipe definition.
 * Wraps a string key (e.g., "heroes:iron_sword") to provide type safety
 * over raw String identifiers in crafting events.
 */
public record RecipeRef(String key) {

    public RecipeRef {
        Objects.requireNonNull(key, "RecipeRef key must not be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("RecipeRef key must not be blank");
        }
    }

    @Override
    public String toString() {
        return key;
    }
}
