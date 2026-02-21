package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.PlayerLanguageProfile;
import net.herotale.herocore.api.language.LanguageId;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * JSON file-based persistence for player language profiles.
 * Stores data in mods/herocore/player_languages.json
 */
public class JsonLanguagePersistence implements LanguagePersistenceLayer {
    
    private static final Logger LOGGER = Logger.getLogger("HeroCore-Language");
    
    private final Path dataFile;
    private final Gson gson;
    
    public JsonLanguagePersistence(Path heroCorePath) {
        this.dataFile = heroCorePath.resolve("player_languages.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    @Override
    public void saveAllProfiles(List<PlayerLanguageProfile> profiles) {
        try {
            JsonObject root = new JsonObject();
            
            for (PlayerLanguageProfile profile : profiles) {
                JsonObject playerData = serializeProfile(profile);
                root.add(profile.getPlayerUuid().toString(), playerData);
            }
            
            Files.createDirectories(dataFile.getParent());
            Files.write(dataFile, gson.toJson(root).getBytes());
            
        } catch (IOException e) {
            LOGGER.severe("Failed to save language profiles: " + e.getMessage());
        }
    }
    
    @Override
    public List<PlayerLanguageProfile> loadAllProfiles() {
        List<PlayerLanguageProfile> profiles = new ArrayList<>();
        
        if (!Files.exists(dataFile)) {
            return profiles;
        }
        
        try {
            String content = Files.readString(dataFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            for (String uuidStr : root.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    JsonObject data = root.getAsJsonObject(uuidStr);
                    PlayerLanguageProfile profile = deserializeProfile(uuid, data);
                    profiles.add(profile);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid UUID in language data: " + uuidStr);
                }
            }
            
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warning("Failed to load language profiles: " + e.getMessage());
        }
        
        return profiles;
    }
    
    @Override
    public void saveProfile(PlayerLanguageProfile profile) {
        try {
            JsonObject root;
            
            if (Files.exists(dataFile)) {
                String content = Files.readString(dataFile);
                root = JsonParser.parseString(content).getAsJsonObject();
            } else {
                root = new JsonObject();
            }
            
            JsonObject playerData = serializeProfile(profile);
            root.add(profile.getPlayerUuid().toString(), playerData);
            
            Files.createDirectories(dataFile.getParent());
            Files.write(dataFile, gson.toJson(root).getBytes());
            
        } catch (IOException e) {
            LOGGER.severe("Failed to save player language profile: " + e.getMessage());
        }
    }
    
    @Override
    public PlayerLanguageProfile loadProfile(UUID playerUuid) {
        if (!Files.exists(dataFile)) {
            return new PlayerLanguageProfile(playerUuid);
        }
        
        try {
            String content = Files.readString(dataFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            if (root.has(playerUuid.toString())) {
                JsonObject data = root.getAsJsonObject(playerUuid.toString());
                return deserializeProfile(playerUuid, data);
            }
            
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warning("Failed to load player language profile: " + e.getMessage());
        }
        
        return new PlayerLanguageProfile(playerUuid);
    }
    
    private JsonObject serializeProfile(PlayerLanguageProfile profile) {
        JsonObject obj = new JsonObject();
        
        // Save proficiencies
        JsonObject proficiencies = new JsonObject();
        for (Map.Entry<LanguageId, Integer> entry : profile.getProficiencies().entrySet()) {
            proficiencies.addProperty(entry.getKey().toString(), entry.getValue());
        }
        obj.add("proficiencies", proficiencies);
        
        // Save active language
        if (profile.getActiveLanguage().isPresent()) {
            obj.addProperty("activeLanguage", profile.getActiveLanguage().get().toString());
        }
        
        return obj;
    }
    
    private PlayerLanguageProfile deserializeProfile(UUID uuid, JsonObject data) {
        PlayerLanguageProfile profile = new PlayerLanguageProfile(uuid);
        
        // Load proficiencies
        if (data.has("proficiencies")) {
            JsonObject profs = data.getAsJsonObject("proficiencies");
            for (String langId : profs.keySet()) {
                int proficiency = profs.get(langId).getAsInt();
                profile.setProficiency(new LanguageId(langId), proficiency);
            }
        }
        
        // Load active language
        if (data.has("activeLanguage")) {
            String activeLang = data.get("activeLanguage").getAsString();
            profile.setActiveLanguage(new LanguageId(activeLang));
        }
        
        return profile;
    }
}
