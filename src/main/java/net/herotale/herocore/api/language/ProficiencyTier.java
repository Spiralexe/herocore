package net.herotale.herocore.api.language;

/**
 * Proficiency tier for language comprehension.
 * Determines how much text distortion is applied to messages from a speaker.
 */
public enum ProficiencyTier {
    NONE(0, 40, "Heavy distortion"),           // 0–40
    SEVERE(41, 80, "Severe distortion"),       // 41–80
    PARTIAL(81, 120, "Partial clarity"),       // 81–120
    MOSTLY_CLEAR(121, 160, "Mostly clear"),    // 121–160
    NEARLY_FLUENT(161, 199, "Nearly fluent"),  // 161–199
    FLUENT(200, 200, "Perfect");               // 200
    
    private final int minProficiency;
    private final int maxProficiency;
    private final String description;
    
    ProficiencyTier(int minProficiency, int maxProficiency, String description) {
        this.minProficiency = minProficiency;
        this.maxProficiency = maxProficiency;
        this.description = description;
    }
    
    public int getMinProficiency() {
        return minProficiency;
    }
    
    public int getMaxProficiency() {
        return maxProficiency;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the proficiency tier for a given proficiency value (0-200).
     */
    public static ProficiencyTier fromProficiency(int proficiency) {
        if (proficiency <= 0) return NONE;
        if (proficiency <= 40) return NONE;
        if (proficiency <= 80) return SEVERE;
        if (proficiency <= 120) return PARTIAL;
        if (proficiency <= 160) return MOSTLY_CLEAR;
        if (proficiency <= 199) return NEARLY_FLUENT;
        return FLUENT;
    }
    
    /**
     * Get clarity chance (0.0–1.0) for this tier.
     * This is used to determine if a word should be left intact or distorted.
     */
    public double getClarityChance(int proficiency) {
        if (proficiency >= 200) return 1.0;
        return Math.max(0.0, Math.min(1.0, proficiency / 200.0));
    }
}
