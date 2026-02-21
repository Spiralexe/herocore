package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TrainingRegistry with daily limit tracking.
 */
public class TrainingRegistryImpl implements TrainingRegistry {
    
    private final LanguageService languageService;
    private final Map<LanguageId, LanguageTrainingCost> trainingCosts = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailyPointsTracking = new ConcurrentHashMap<>(); // key: "uuid:languageId"
    private final LanguageTrainingCost defaultCost;
    
    public TrainingRegistryImpl(LanguageService languageService) {
        this.languageService = languageService;
        this.defaultCost = LanguageTrainingCost.builder()
            .goldCost(100)
            .proficiencyGain(5)
            .maxPointsPerDay(20)
            .build();
    }
    
    @Override
    public void registerTrainingCost(LanguageId languageId, LanguageTrainingCost cost) {
        trainingCosts.put(languageId, cost);
    }
    
    @Override
    public Optional<LanguageTrainingCost> getTrainingCost(LanguageId languageId) {
        return Optional.ofNullable(trainingCosts.get(languageId));
    }
    
    @Override
    public LanguageTrainingCost getDefaultTrainingCost() {
        return defaultCost;
    }
    
    @Override
    public boolean trainPlayer(UUID playerUuid, LanguageId languageId, 
                              int playerGold, Optional<Integer> factionReputation) {
        
        LanguageTrainingCost cost = trainingCosts.getOrDefault(languageId, defaultCost);
        
        // Check gold
        if (playerGold < cost.getGoldCost()) {
            return false;
        }
        
        // Check faction reputation if required
        if (cost.requiresFactionReputation()) {
            if (factionReputation.isEmpty() || factionReputation.get() < cost.getMinFactionReputation()) {
                return false;
            }
        }
        
        // Check daily limit
        String key = playerUuid.toString() + ":" + languageId;
        int remainingToday = getRemainingDailyPointsForPlayer(playerUuid, languageId);
        
        if (cost.getProficiencyGain() > remainingToday) {
            return false;
        }
        
        // Check if already at max proficiency (200)
        int currentProf = languageService.getProficiency(playerUuid, languageId);
        if (currentProf >= 200) {
            return false; // Already maxed out
        }
        
        // All checks passed — apply training (will be clamped to 200 by PlayerLanguageProfile)
        languageService.addProficiency(playerUuid, languageId, cost.getProficiencyGain());
        
        // Track daily points
        int used = dailyPointsTracking.getOrDefault(key, 0);
        dailyPointsTracking.put(key, used + cost.getProficiencyGain());
        
        return true;
    }
    
    @Override
    public int getRemainingDailyPointsForPlayer(UUID playerUuid, LanguageId languageId) {
        String key = playerUuid.toString() + ":" + languageId;
        LanguageTrainingCost cost = trainingCosts.getOrDefault(languageId, defaultCost);
        
        int used = dailyPointsTracking.getOrDefault(key, 0);
        return Math.max(0, cost.getMaxPointsPerDay() - used);
    }
    
    @Override
    public void resetDailyLimits() {
        dailyPointsTracking.clear();
    }
}
