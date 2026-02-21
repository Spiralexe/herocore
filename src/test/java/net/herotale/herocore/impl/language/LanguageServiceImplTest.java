package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageServiceImplTest {
    
    private LanguageService service;
    private LanguageDefinition testLanguage;
    private UUID testPlayer = UUID.randomUUID();
    
    @BeforeEach
    public void setup() {
        DistortionProfile profile = DistortionProfile.builder()
            .phonemeSet("test")
            .syllablePool(List.of("test"))
            .noiseWords(List.of("test"))
            .build();
        
        testLanguage = LanguageDefinition.builder()
            .id("test_lang")
            .displayName("Test Language")
            .factionId("TestFaction")
            .maxProficiency(200)
            .distortionProfile(profile)
            .build();
        
        LanguagePersistenceLayer persistence = new MockPersistenceLayer();
        service = new LanguageServiceImpl(persistence);
        service.registerLanguage(testLanguage);
    }
    
    @Test
    public void testProficiencyTracking() {
        assertEquals(0, service.getProficiency(testPlayer, testLanguage.getId()));
        
        service.addProficiency(testPlayer, testLanguage.getId(), 50);
        assertEquals(50, service.getProficiency(testPlayer, testLanguage.getId()));
        
        service.addProficiency(testPlayer, testLanguage.getId(), 50);
        assertEquals(100, service.getProficiency(testPlayer, testLanguage.getId()));
    }
    
    @Test
    public void testProficiencyMaxCap() {
        service.setProficiency(testPlayer, testLanguage.getId(), 300);
        assertEquals(200, service.getProficiency(testPlayer, testLanguage.getId()), "Proficiency should be capped at 200");
    }
    
    @Test
    public void testSetActiveLanguage() {
        assertTrue(service.getActiveLanguage(testPlayer).isEmpty(), "No active language initially");
        
        service.setProficiency(testPlayer, testLanguage.getId(), 100);
        service.setActiveLanguage(testPlayer, testLanguage.getId());
        
        assertTrue(service.getActiveLanguage(testPlayer).isPresent());
        assertEquals(testLanguage.getId(), service.getActiveLanguage(testPlayer).get());
    }
    
    @Test
    public void testCannotSetActiveLanguageWithZeroProficiency() {
        LanguageId langId = testLanguage.getId();
        service.setActiveLanguage(testPlayer, langId);
        
        // Should reject because proficiency is 0
        assertTrue(service.getActiveLanguage(testPlayer).isEmpty(), 
            "Cannot set active language with 0 proficiency");
    }
    
    @Test
    public void testPlayerInitialization() {
        service.initializePlayer(testPlayer, Optional.of("TestFaction"), Optional.empty());
        
        // Should grant full proficiency in faction language
        assertEquals(200, service.getProficiency(testPlayer, testLanguage.getId()));
    }
    
    @Test
    public void testGetProfile() {
        service.addProficiency(testPlayer, testLanguage.getId(), 75);
        
        PlayerLanguageProfile profile = service.getProfile(testPlayer);
        assertNotNull(profile);
        assertEquals(75, profile.getProficiency(testLanguage.getId()));
    }
    
    @Test
    public void testLanguageRegistry() {
        assertTrue(service.getLanguage(testLanguage.getId()).isPresent());
        assertFalse(service.getLanguage(new LanguageId("nonexistent")).isPresent());
        
        assertEquals(1, service.getAllLanguages().size());
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
