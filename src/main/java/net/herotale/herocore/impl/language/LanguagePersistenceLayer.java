package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.PlayerLanguageProfile;
import java.util.List;

/**
 * Interface for persisting player language data.
 * Implementations handle database/file I/O.
 */
public interface LanguagePersistenceLayer {
    
    /**
     * Save all player language profiles to persistence.
     */
    void saveAllProfiles(List<PlayerLanguageProfile> profiles);
    
    /**
     * Load all player language profiles from persistence.
     */
    List<PlayerLanguageProfile> loadAllProfiles();
    
    /**
     * Save a single player's profile.
     */
    void saveProfile(PlayerLanguageProfile profile);
    
    /**
     * Load a single player's profile by UUID.
     */
    PlayerLanguageProfile loadProfile(java.util.UUID playerUuid);
}
