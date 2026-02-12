package net.herotale.herocore.api.attribute;

import java.util.Objects;

/**
 * Identifies the source of a modifier for bulk removal.
 * Namespaced: {@code "plugin:context_name"} (e.g., {@code "heroes:race_dwarf"}).
 */
public final class ModifierSource {

    private final String key;

    private ModifierSource(String key) {
        this.key = Objects.requireNonNull(key, "ModifierSource key must not be null");
    }

    /**
     * Create a source from a namespaced key.
     *
     * @param key the source key (e.g., {@code "guilds:rank_veteran"})
     * @return the modifier source
     */
    public static ModifierSource of(String key) {
        return new ModifierSource(key);
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifierSource other)) return false;
        return key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "ModifierSource{" + key + "}";
    }
}
