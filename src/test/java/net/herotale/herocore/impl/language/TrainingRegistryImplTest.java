package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TrainingRegistryImplTest {
    
    private TrainingRegistry registry;
    private LanguageService languageService;
    private LanguageId testLanguage = new LanguageId("test_lang");
    private UUID testPlayer = UUID.randomUUID();
    
    @BeforeEach
    public void setup() {
        LanguagePersistenceLayer persistence = new MockPersistenceLayer();
        languageService = new LanguageServiceImpl(persistence);
        registry = new TrainingRegistryImpl(languageService);
        
        // Set player's starting proficiency
        languageService.setProficiency(testPlayer, testLanguage, 0);
    }
    
    @Test
    public void testDefaultTrainingCost() {
        LanguageTrainingCost cost = registry.getDefaultTrainingCost();
        assertNotNull(cost);
        assertEquals(100, cost.getGoldCost());
        assertEquals(5, cost.getProficiencyGain());
        assertEquals(20, cost.getMaxPointsPerDay());
    }
    
    @Test
    public void testRegisterCustomCost() {
        LanguageTrainingCost custom = LanguageTrainingCost.builder()
            .goldCost(50)
            .proficiencyGain(10)
            .maxPointsPerDay(30)
            .build();
        
        registry.registerTrainingCost(testLanguage, custom);
        
        assertTrue(registry.getTrainingCost(testLanguage).isPresent());
        assertEquals(50, registry.getTrainingCost(testLanguage).get().getGoldCost());
    }
    
    @Test
    public void testSuccessfulTraining() {
        boolean success = registry.trainPlayer(testPlayer, testLanguage, 200, Optional.empty());
        assertTrue(success, "Training should succeed with enough gold");
        
        // Check proficiency increased
        assertEquals(5, languageService.getProficiency(testPlayer, testLanguage));
    }
    
    @Test
    public void testInsufficientGold() {
        boolean success = registry.trainPlayer(testPlayer, testLanguage, 50, Optional.empty());
        assertFalse(success, "Training should fail with insufficient gold");
        
        // Proficiency should not change
        assertEquals(0, languageService.getProficiency(testPlayer, testLanguage));
    }
    
    @Test
    public void testDailyLimitTracking() {
        // Train 4 times (4 * 5 = 20 points = max per day)
        for (int i = 0; i < 4; i++) {
            boolean success = registry.trainPlayer(testPlayer, testLanguage, 500, Optional.empty());
            assertTrue(success, "Training " + i + " should succeed");
        }
        
        // Fifth training should fail (daily limit)
        boolean success = registry.trainPlayer(testPlayer, testLanguage, 500, Optional.empty());
        assertFalse(success, "Training should fail due to daily limit");
        
        // Proficiency should be 20
        assertEquals(20, languageService.getProficiency(testPlayer, testLanguage));
    }
    
    @Test
    public void testMaxProficiencyCap() {
        languageService.setProficiency(testPlayer, testLanguage, 198);
        
        // Try to train 10 points (default is 5, but try with multiple trainings)
        boolean success = registry.trainPlayer(testPlayer, testLanguage, 500, Optional.empty());
        assertTrue(success, "First training should succeed");
        
        // Now at 203, should be capped at 200
        assertEquals(200, languageService.getProficiency(testPlayer, testLanguage), 
            "Proficiency should not exceed 200");
    }
    
    @Test
    public void testResetDailyLimits() {
        // Use all daily points
        for (int i = 0; i < 4; i++) {
            registry.trainPlayer(testPlayer, testLanguage, 500, Optional.empty());
        }
        
        int remaining = registry.getRemainingDailyPointsForPlayer(testPlayer, testLanguage);
        assertEquals(0, remaining, "Should have 0 points remaining");
        
        // Reset
        registry.resetDailyLimits();
        
        remaining = registry.getRemainingDailyPointsForPlayer(testPlayer, testLanguage);
        assertEquals(20, remaining, "After reset, should have full daily limit");
    }
    
    /**
     * Mock persistence layer for testing (no-op).
     */
    private static class MockPersistenceLayer implements LanguagePersistenceLayer {
        @Override
        public void saveAllProfiles(java.util.List<PlayerLanguageProfile> profiles) {}
        
        @Override
        public java.util.List<PlayerLanguageProfile> loadAllProfiles() {
            return new ArrayList<>();
        }
        
        @Override
        public void saveProfile(PlayerLanguageProfile profile) {}
        
        @Override
        public PlayerLanguageProfile loadProfile(UUID playerUuid) {
            return new PlayerLanguageProfile(playerUuid);
        }
    }
}
