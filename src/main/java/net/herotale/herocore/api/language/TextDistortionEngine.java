package net.herotale.herocore.api.language;

import java.util.Random;

/**
 * Pure function for processing text distortion based on proficiency.
 * Stateless and deterministic for performance.
 */
public class TextDistortionEngine {
    
    private static final Random RANDOM = new Random();
    
    private TextDistortionEngine() {
        // Utility class
    }
    
    /**
     * Process a message, applying distortion based on proficiency tier.
     * Pure function: deterministic based on input, no side effects.
     * 
     * @param original The original message text
     * @param proficiency Proficiency level (0-200)
     * @param profile The language's distortion profile
     * @return The processed (possibly distorted) message
     */
    public static String processMessage(String original, int proficiency, DistortionProfile profile) {
        // Perfect fluency — no distortion
        if (proficiency >= 200) {
            return original;
        }
        
        // Clarity chance determines word preservation vs distortion
        double clarityChance = ProficiencyTier.fromProficiency(proficiency).getClarityChance(proficiency);
        
        // Split message into words (preserving spaces)
        String[] words = original.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            
            // Decide whether to keep or distort this word
            if (RANDOM.nextDouble() < clarityChance) {
                // Keep the word intact
                result.append(word);
            } else {
                // Distort the word
                result.append(distortWord(word, profile));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Distort a single word using the language's phoneme and syllable profiles.
     * Simple algorithm: replace word with noise word or syllable combination.
     */
    private static String distortWord(String word, DistortionProfile profile) {
        // 70% chance: use a noise word
        // 30% chance: use scrambled syllables
        if (RANDOM.nextDouble() < 0.7) {
            return profile.getRandomNoiseWord(RANDOM);
        }
        
        // Generate pseudo-word from syllables
        int syllableCount = Math.max(1, word.length() / 3);
        StringBuilder distorted = new StringBuilder();
        java.util.List<String> syllables = profile.getSyllablePool();
        
        for (int i = 0; i < syllableCount; i++) {
            if (syllables.isEmpty()) {
                distorted.append("?");
            } else {
                distorted.append(syllables.get(RANDOM.nextInt(syllables.size())));
            }
        }
        
        return distorted.toString();
    }
}
