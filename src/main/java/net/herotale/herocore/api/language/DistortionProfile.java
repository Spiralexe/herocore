package net.herotale.herocore.api.language;

import java.util.List;

/**
 * Configuration for how a language distorts text based on proficiency.
 * Uses deterministic word substitution, not semantic translation.
 */
public class DistortionProfile {
    
    private final String phonemeSet;
    private final List<String> syllablePool;
    private final List<String> noiseWords;
    private final int noiseWordsPoolSize;
    
    public DistortionProfile(String phonemeSet, List<String> syllablePool, 
                           List<String> noiseWords) {
        this.phonemeSet = phonemeSet;
        this.syllablePool = List.copyOf(syllablePool);
        this.noiseWords = List.copyOf(noiseWords);
        this.noiseWordsPoolSize = noiseWords.size();
    }
    
    public String getPhonemeSet() {
        return phonemeSet;
    }
    
    public List<String> getSyllablePool() {
        return syllablePool;
    }
    
    public List<String> getNoiseWords() {
        return noiseWords;
    }
    
    public String getRandomNoiseWord(java.util.Random random) {
        if (noiseWords.isEmpty()) {
            return "...";
        }
        return noiseWords.get(random.nextInt(noiseWordsPoolSize));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String phonemeSet;
        private List<String> syllablePool;
        private List<String> noiseWords;
        
        public Builder phonemeSet(String phonemeSet) {
            this.phonemeSet = phonemeSet;
            return this;
        }
        
        public Builder syllablePool(List<String> syllablePool) {
            this.syllablePool = syllablePool;
            return this;
        }
        
        public Builder noiseWords(List<String> noiseWords) {
            this.noiseWords = noiseWords;
            return this;
        }
        
        public DistortionProfile build() {
            if (phonemeSet == null) throw new IllegalStateException("Phoneme set is required");
            if (syllablePool == null || syllablePool.isEmpty()) {
                throw new IllegalStateException("Syllable pool cannot be empty");
            }
            if (noiseWords == null || noiseWords.isEmpty()) {
                throw new IllegalStateException("Noise words cannot be empty");
            }
            return new DistortionProfile(phonemeSet, syllablePool, noiseWords);
        }
    }
}
