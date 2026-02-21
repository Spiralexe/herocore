package net.herotale.herocore.api.language;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Represents a player's language proficiency and active language.
 * Stored per player, persisted to database.
 */
public class PlayerLanguageProfile {
    
    private final UUID playerUuid;
    private final Map<LanguageId, Integer> proficiencyMap;
    private LanguageId activeLanguage;
    
    public PlayerLanguageProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.proficiencyMap = new HashMap<>();
        this.activeLanguage = null;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Get proficiency level for a language (0-200, or 0 if not learned).
     */
    public int getProficiency(LanguageId languageId) {
        return proficiencyMap.getOrDefault(languageId, 0);
    }
    
    /**
     * Set proficiency level for a language (clamped to 0-200).
     */
    public void setProficiency(LanguageId languageId, int proficiency) {
        int clamped = Math.max(0, Math.min(200, proficiency));
        if (clamped == 0) {
            proficiencyMap.remove(languageId);
        } else {
            proficiencyMap.put(languageId, clamped);
        }
    }
    
    /**
     * Add proficiency points to a language (clamped to 0-200).
     */
    public void addProficiency(LanguageId languageId, int amount) {
        int current = getProficiency(languageId);
        setProficiency(languageId, current + amount);
    }
    
    /**
     * Get all proficiencies as an immutable map.
     */
    public Map<LanguageId, Integer> getProficiencies() {
        return Map.copyOf(proficiencyMap);
    }
    
    /**
     * Set the active language (must have proficiency > 0).
     */
    public void setActiveLanguage(LanguageId languageId) {
        if (languageId != null && getProficiency(languageId) > 0) {
            this.activeLanguage = languageId;
        } else if (languageId == null) {
            this.activeLanguage = null;
        }
        // Otherwise silently reject
    }
    
    /**
     * Get the active language (may be empty if not set).
     */
    public Optional<LanguageId> getActiveLanguage() {
        return Optional.ofNullable(activeLanguage);
    }
    
    /**
     * Reset all proficiencies for this player.
     */
    public void resetAll() {
        proficiencyMap.clear();
        activeLanguage = null;
    }
}
