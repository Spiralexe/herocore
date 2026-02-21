package net.herotale.herocore.api.language;

import java.util.UUID;
import java.util.Optional;

/**
 * Extended LanguageService with training and NPC integration.
 */
public interface TrainingRegistry {
    
    /**
     * Register a training cost profile for a language.
     * Can be called multiple times to override.
     */
    void registerTrainingCost(LanguageId languageId, LanguageTrainingCost cost);
    
    /**
     * Get the training cost for a language.
     */
    Optional<LanguageTrainingCost> getTrainingCost(LanguageId languageId);
    
    /**
     * Get a default training cost (used if no specific cost is registered).
     */
    LanguageTrainingCost getDefaultTrainingCost();
    
    /**
     * Train a player in a language if they meet requirements.
     * Returns true if training was successful.
     * 
     * Checks:
     * - Gold cost (must have enough)
     * - Faction reputation (if required)
     * - Daily training limit
     * - Max proficiency cap
     */
    boolean trainPlayer(UUID playerUuid, LanguageId languageId, 
                       int playerGold, Optional<Integer> factionReputation);
    
    /**
     * Get how many points a player can still train today.
     */
    int getRemainingDailyPointsForPlayer(UUID playerUuid, LanguageId languageId);
    
    /**
     * Reset daily training limits (call once per day).
     */
    void resetDailyLimits();
}
