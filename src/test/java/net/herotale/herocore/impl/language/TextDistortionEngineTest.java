package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TextDistortionEngineTest {
    
    private DistortionProfile profile;
    
    @BeforeEach
    public void setup() {
        profile = DistortionProfile.builder()
            .phonemeSet("aeioustr")
            .syllablePool(List.of("im", "pe", "ri", "um", "test", "word"))
            .noiseWords(List.of("blah", "grok", "zzz", "waah"))
            .build();
    }
    
    @Test
    public void testPerfectFluency() {
        String original = "Hello everyone";
        String result = TextDistortionEngine.processMessage(original, 200, profile);
        assertEquals(original, result, "At 200 proficiency, message should be unchanged");
    }
    
    @Test
    public void testNoUnderstanding() {
        String original = "Hello everyone";
        String result = TextDistortionEngine.processMessage(original, 0, profile);
        assertNotEquals(original, result, "At 0 proficiency, message should be distorted");
        
        // Should contain words, not be empty
        assertFalse(result.isBlank(), "Distorted message should not be empty");
    }
    
    @Test
    public void testPartialUnderstanding() {
        String original = "Hello world test";
        String result = TextDistortionEngine.processMessage(original, 100, profile);
        
        // At 50% clarity, some words should match
        String[] originalWords = original.split(" ");
        String[] resultWords = result.split(" ");
        
        assertEquals(originalWords.length, resultWords.length, "Word count should match");
        
        // At least some variation should occur (probabilistic, but highly likely)
        // We can't test deterministically due to randomness
    }
    
    @Test
    public void testMessageStructurePreserved() {
        String original = "This is a test message with many words";
        String result = TextDistortionEngine.processMessage(original, 50, profile);
        
        String[] originalWords = original.split(" ");
        String[] resultWords = result.split(" ");
        
        assertEquals(originalWords.length, resultWords.length, "Word count should be preserved");
    }
    
    @Test
    public void testProficiencyTierCalculation() {
        assertEquals(ProficiencyTier.NONE, ProficiencyTier.fromProficiency(20));
        assertEquals(ProficiencyTier.SEVERE, ProficiencyTier.fromProficiency(60));
        assertEquals(ProficiencyTier.PARTIAL, ProficiencyTier.fromProficiency(100));
        assertEquals(ProficiencyTier.MOSTLY_CLEAR, ProficiencyTier.fromProficiency(140));
        assertEquals(ProficiencyTier.NEARLY_FLUENT, ProficiencyTier.fromProficiency(180));
        assertEquals(ProficiencyTier.FLUENT, ProficiencyTier.fromProficiency(200));
    }
    
    @Test
    public void testClarityChanceCalculation() {
        ProficiencyTier tier = ProficiencyTier.PARTIAL;
        double clarity = tier.getClarityChance(100);
        assertEquals(0.5, clarity, 0.01, "100/200 = 0.5");
    }
}
