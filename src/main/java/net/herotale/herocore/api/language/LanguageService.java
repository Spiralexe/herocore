package net.herotale.herocore.api.language;

import java.util.UUID;
import java.util.Optional;

/**
 * Core language service API for Herocore.
 * Exposes training, proficiency queries, and message processing.
 */
public interface LanguageService {
    
    /**
     * Add proficiency points to a player for a specific language.
     * Value is clamped to 0-200 total.
     */
    void addProficiency(UUID playerUuid, LanguageId languageId, int amount);
    
    /**
     * Get proficiency level for a player in a language.
     */
    int getProficiency(UUID playerUuid, LanguageId languageId);
    
    /**
     * Set proficiency for a player (clamped to 0-200).
     */
    void setProficiency(UUID playerUuid, LanguageId languageId, int proficiency);
    
    /**
     * Set the active language for a player (must have proficiency > 0).
     */
    void setActiveLanguage(UUID playerUuid, LanguageId languageId);
    
    /**
     * Get the active language for a player.
     */
    Optional<LanguageId> getActiveLanguage(UUID playerUuid);
    
    /**
     * Get the player's full language profile.
     */
    PlayerLanguageProfile getProfile(UUID playerUuid);
    
    /**
     * Initialize a player with starting languages (called on join).
     * Grants full proficiency (200) in faction/race languages.
     */
    void initializePlayer(UUID playerUuid, Optional<String> factionId, Optional<String> raceId);
    
    /**
     * Process a message based on sender's active language and receiver's proficiency.
     * Pure function: (original, senderLanguage, receiverProficiency) → distorted message
     * 
     * @param original The original message text
     * @param senderLanguageId The language the sender is speaking in
     * @param receiverProficiency The receiver's proficiency in that language
     * @return The processed message (possibly distorted)
     */
    String processMessage(String original, LanguageId senderLanguageId, int receiverProficiency);
    
    /**
     * Look up a language definition by ID.
     */
    Optional<LanguageDefinition> getLanguage(LanguageId languageId);
    
    /**
     * Register a language definition.
     */
    void registerLanguage(LanguageDefinition definition);
    
    /**
     * Get all registered languages.
     */
    java.util.Collection<LanguageDefinition> getAllLanguages();
    
    /**
     * Force a full save of all player language data to persistence.
     */
    void saveAll();
    
    /**
     * Force a load of all player language data from persistence.
     */
    void loadAll();
}
