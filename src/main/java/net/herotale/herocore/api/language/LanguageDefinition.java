package net.herotale.herocore.api.language;

import java.util.Optional;

/**
 * Defines a language that can be learned and spoken.
 * A language belongs to either a faction or a race (or is independent).
 */
public class LanguageDefinition {
    
    private final LanguageId id;
    private final String displayName;
    private final Optional<String> factionId;
    private final Optional<String> raceId;
    private final int maxProficiency;
    private final DistortionProfile distortionProfile;
    
    public LanguageDefinition(LanguageId id, String displayName, Optional<String> factionId,
                             Optional<String> raceId, int maxProficiency,
                             DistortionProfile distortionProfile) {
        this.id = id;
        this.displayName = displayName;
        this.factionId = factionId;
        this.raceId = raceId;
        this.maxProficiency = maxProficiency;
        this.distortionProfile = distortionProfile;
    }
    
    public LanguageId getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Optional<String> getFactionId() {
        return factionId;
    }
    
    public Optional<String> getRaceId() {
        return raceId;
    }
    
    public int getMaxProficiency() {
        return maxProficiency;
    }
    
    public DistortionProfile getDistortionProfile() {
        return distortionProfile;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LanguageId id;
        private String displayName;
        private Optional<String> factionId = Optional.empty();
        private Optional<String> raceId = Optional.empty();
        private int maxProficiency = 200;
        private DistortionProfile distortionProfile;
        
        public Builder id(LanguageId id) {
            this.id = id;
            return this;
        }
        
        public Builder id(String id) {
            this.id = new LanguageId(id);
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder factionId(String factionId) {
            this.factionId = Optional.ofNullable(factionId);
            return this;
        }
        
        public Builder raceId(String raceId) {
            this.raceId = Optional.ofNullable(raceId);
            return this;
        }
        
        public Builder maxProficiency(int maxProficiency) {
            this.maxProficiency = maxProficiency;
            return this;
        }
        
        public Builder distortionProfile(DistortionProfile profile) {
            this.distortionProfile = profile;
            return this;
        }
        
        public LanguageDefinition build() {
            if (id == null) throw new IllegalStateException("Language ID is required");
            if (displayName == null) throw new IllegalStateException("Display name is required");
            if (distortionProfile == null) throw new IllegalStateException("Distortion profile is required");
            return new LanguageDefinition(id, displayName, factionId, raceId, maxProficiency, distortionProfile);
        }
    }
}
