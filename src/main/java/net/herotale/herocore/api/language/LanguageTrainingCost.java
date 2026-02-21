package net.herotale.herocore.api.language;

import java.util.UUID;

/**
 * Represents the cost and requirements for training in a language.
 */
public class LanguageTrainingCost {
    
    private final int goldCost;
    private final int proficiencyGain;
    private final boolean requiresFactionReputation;
    private final int minFactionReputation;
    private final long trainingTimeSeconds;
    private final int maxPointsPerDay;
    
    public LanguageTrainingCost(int goldCost, int proficiencyGain, 
                               boolean requiresFactionReputation, int minFactionReputation,
                               long trainingTimeSeconds, int maxPointsPerDay) {
        this.goldCost = goldCost;
        this.proficiencyGain = proficiencyGain;
        this.requiresFactionReputation = requiresFactionReputation;
        this.minFactionReputation = minFactionReputation;
        this.trainingTimeSeconds = trainingTimeSeconds;
        this.maxPointsPerDay = maxPointsPerDay;
    }
    
    public int getGoldCost() {
        return goldCost;
    }
    
    public int getProficiencyGain() {
        return proficiencyGain;
    }
    
    public boolean requiresFactionReputation() {
        return requiresFactionReputation;
    }
    
    public int getMinFactionReputation() {
        return minFactionReputation;
    }
    
    public long getTrainingTimeSeconds() {
        return trainingTimeSeconds;
    }
    
    public int getMaxPointsPerDay() {
        return maxPointsPerDay;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int goldCost = 100;
        private int proficiencyGain = 5;
        private boolean requiresFactionReputation = false;
        private int minFactionReputation = 0;
        private long trainingTimeSeconds = 0;
        private int maxPointsPerDay = 20;
        
        public Builder goldCost(int cost) {
            this.goldCost = cost;
            return this;
        }
        
        public Builder proficiencyGain(int gain) {
            this.proficiencyGain = gain;
            return this;
        }
        
        public Builder requiresFactionReputation(boolean required) {
            this.requiresFactionReputation = required;
            return this;
        }
        
        public Builder minFactionReputation(int min) {
            this.minFactionReputation = min;
            return this;
        }
        
        public Builder trainingTimeSeconds(long seconds) {
            this.trainingTimeSeconds = seconds;
            return this;
        }
        
        public Builder maxPointsPerDay(int max) {
            this.maxPointsPerDay = max;
            return this;
        }
        
        public LanguageTrainingCost build() {
            return new LanguageTrainingCost(goldCost, proficiencyGain, requiresFactionReputation,
                minFactionReputation, trainingTimeSeconds, maxPointsPerDay);
        }
    }
}
