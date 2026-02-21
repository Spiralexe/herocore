package net.herotale.herocore.api.language;

/**
 * Identifier for a language. Languages can be faction-based or race-based.
 * Examples: "imperium_common", "dominion_common", "elf_tongue"
 */
public record LanguageId(String value) {
    
    public LanguageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Language ID cannot be null or blank");
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
