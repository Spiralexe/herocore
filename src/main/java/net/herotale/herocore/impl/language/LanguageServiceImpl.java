package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of LanguageService.
 * All proficiency data is cached in memory. Persistence is handled externally.
 */
public class LanguageServiceImpl implements LanguageService {
    
    private final Map<LanguageId, LanguageDefinition> languages = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerLanguageProfile> playerProfiles = new ConcurrentHashMap<>();
    private final LanguagePersistenceLayer persistence;
    
    public LanguageServiceImpl(LanguagePersistenceLayer persistence) {
        this.persistence = persistence;
    }
    
    @Override
    public void addProficiency(UUID playerUuid, LanguageId languageId, int amount) {
        PlayerLanguageProfile profile = getOrCreateProfile(playerUuid);
        profile.addProficiency(languageId, amount);
    }
    
    @Override
    public int getProficiency(UUID playerUuid, LanguageId languageId) {
        return getOrCreateProfile(playerUuid).getProficiency(languageId);
    }
    
    @Override
    public void setProficiency(UUID playerUuid, LanguageId languageId, int proficiency) {
        getOrCreateProfile(playerUuid).setProficiency(languageId, proficiency);
    }
    
    @Override
    public void setActiveLanguage(UUID playerUuid, LanguageId languageId) {
        getOrCreateProfile(playerUuid).setActiveLanguage(languageId);
    }
    
    @Override
    public Optional<LanguageId> getActiveLanguage(UUID playerUuid) {
        return getOrCreateProfile(playerUuid).getActiveLanguage();
    }
    
    @Override
    public PlayerLanguageProfile getProfile(UUID playerUuid) {
        return getOrCreateProfile(playerUuid);
    }
    
    @Override
    public void initializePlayer(UUID playerUuid, Optional<String> factionId, Optional<String> raceId) {
        PlayerLanguageProfile profile = getOrCreateProfile(playerUuid);
        
        // Grant full proficiency (200) for faction and race languages
        for (LanguageDefinition lang : languages.values()) {
            if (lang.getFactionId().isPresent() && lang.getFactionId().equals(factionId)) {
                profile.setProficiency(lang.getId(), 200);
                // Set as active if not already set
                if (profile.getActiveLanguage().isEmpty()) {
                    profile.setActiveLanguage(lang.getId());
                }
            } else if (lang.getRaceId().isPresent() && lang.getRaceId().equals(raceId)) {
                profile.setProficiency(lang.getId(), 200);
                // Optionally set as active
                if (profile.getActiveLanguage().isEmpty()) {
                    profile.setActiveLanguage(lang.getId());
                }
            }
        }
    }
    
    @Override
    public String processMessage(String original, LanguageId senderLanguageId, int receiverProficiency) {
        LanguageDefinition language = languages.get(senderLanguageId);
        if (language == null) {
            // Language not found, return original
            return original;
        }
        
        return TextDistortionEngine.processMessage(original, receiverProficiency, language.getDistortionProfile());
    }
    
    @Override
    public Optional<LanguageDefinition> getLanguage(LanguageId languageId) {
        return Optional.ofNullable(languages.get(languageId));
    }
    
    @Override
    public void registerLanguage(LanguageDefinition definition) {
        languages.put(definition.getId(), definition);
    }
    
    @Override
    public Collection<LanguageDefinition> getAllLanguages() {
        return languages.values();
    }
    
    @Override
    public void saveAll() {
        persistence.saveAllProfiles(new ArrayList<>(playerProfiles.values()));
    }
    
    @Override
    public void loadAll() {
        List<PlayerLanguageProfile> loaded = persistence.loadAllProfiles();
        playerProfiles.clear();
        for (PlayerLanguageProfile profile : loaded) {
            playerProfiles.put(profile.getPlayerUuid(), profile);
        }
    }
    
    private PlayerLanguageProfile getOrCreateProfile(UUID playerUuid) {
        return playerProfiles.computeIfAbsent(playerUuid, uuid -> new PlayerLanguageProfile(uuid));
    }
}
