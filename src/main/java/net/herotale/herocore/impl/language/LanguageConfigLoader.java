package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads language definitions from JSON configuration.
 * Default config is included as a resource; can be overridden by mods/herocore/languages.json
 */
public class LanguageConfigLoader {
    
    private static final Logger LOGGER = Logger.getLogger("HeroCore-Language");
    private final Gson gson = new Gson();
    
    /**
     * Load default languages from built-in resource.
     */
    public List<LanguageDefinition> loadDefaults() {
        List<LanguageDefinition> languages = new ArrayList<>();
        
        // Built-in default languages
        languages.add(createImperiumCommon());
        languages.add(createDominionCommon());
        languages.add(createElfTongue());
        languages.add(createOrcish());
        
        return languages;
    }
    
    /**
     * Load languages from JSON config file.
     * Falls back to defaults if file doesn't exist.
     */
    public List<LanguageDefinition> loadFromFile(Path configPath) {
        if (!Files.exists(configPath)) {
            LOGGER.info("Language config not found at " + configPath + ", using defaults");
            return loadDefaults();
        }
        
        try {
            String content = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            List<LanguageDefinition> languages = new ArrayList<>();
            
            if (root.has("languages")) {
                JsonArray langArray = root.getAsJsonArray("languages");
                for (JsonElement elem : langArray) {
                    try {
                        JsonObject langObj = elem.getAsJsonObject();
                        LanguageDefinition def = deserializeLanguage(langObj);
                        languages.add(def);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to parse language definition: " + e.getMessage());
                    }
                }
            }
            
            return languages;
            
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warning("Failed to load language config, using defaults: " + e.getMessage());
            return loadDefaults();
        }
    }
    
    /**
     * Create default Imperium Common language.
     */
    private LanguageDefinition createImperiumCommon() {
        DistortionProfile profile = DistortionProfile.builder()
            .phonemeSet("aeioustr")
            .syllablePool(List.of(
                "im", "pe", "ri", "um", "va", "lor", "do", "mi", "nus",
                "tem", "pus", "reg", "nox", "sol", "lux", "rex", "res"
            ))
            .noiseWords(List.of(
                "valoren", "imperis", "doraxen", "templen", "rexion",
                "luxera", "dominus", "solaris", "noctis", "regalis",
                "tempora", "verius", "saltor", "minara", "tertis"
            ))
            .build();
        
        return LanguageDefinition.builder()
            .id("imperium_common")
            .displayName("Imperial Common")
            .factionId("Imperium")
            .maxProficiency(200)
            .distortionProfile(profile)
            .build();
    }
    
    /**
     * Create default Dominion Common language.
     */
    private LanguageDefinition createDominionCommon() {
        DistortionProfile profile = DistortionProfile.builder()
            .phonemeSet("krgbhwth")
            .syllablePool(List.of(
                "kek", "wrah", "buah", "grok", "krash", "thuk", "wrog",
                "brah", "grul", "thrax", "wuk", "krul", "grax", "brok"
            ))
            .noiseWords(List.of(
                "kroglak", "wrahbuk", "buargok", "thrakur", "gromlas",
                "wukleth", "braxgul", "kronum", "thukkah", "graxxul",
                "brogrim", "kraweth", "thraxgul", "wugrakh", "bralkor"
            ))
            .build();
        
        return LanguageDefinition.builder()
            .id("dominion_common")
            .displayName("Dominion Tongue")
            .factionId("Dominion")
            .maxProficiency(200)
            .distortionProfile(profile)
            .build();
    }
    
    /**
     * Create default Elf Tongue language (race-based).
     */
    private LanguageDefinition createElfTongue() {
        DistortionProfile profile = DistortionProfile.builder()
            .phonemeSet("aeiouyn")
            .syllablePool(List.of(
                "ae", "ir", "on", "yn", "ael", "syn", "tir", "nes",
                "lor", "min", "eth", "vel", "gal", "ath", "wen"
            ))
            .noiseWords(List.of(
                "aelynthir", "synnesthor", "lorianthel", "minethgal",
                "athwendorn", "elaynthir", "velsynthon", "galenthor",
                "nesierthel", "tiraelthen", "wynasthor", "etherelyn",
                "alanthir", "sylethron", "mirendel"
            ))
            .build();
        
        return LanguageDefinition.builder()
            .id("elf_tongue")
            .displayName("Elven")
            .raceId("Elf")
            .maxProficiency(200)
            .distortionProfile(profile)
            .build();
    }
    
    /**
     * Create default Orcish language (race-based).
     */
    private LanguageDefinition createOrcish() {
        DistortionProfile profile = DistortionProfile.builder()
            .phonemeSet("grkhbrw")
            .syllablePool(List.of(
                "gor", "mak", "dug", "ugh", "lok", "rek", "gruk",
                "mog", "brek", "grok", "tuk", "wak", "drak", "buk"
            ))
            .noiseWords(List.of(
                "gormagok", "dugbreka", "ughloktuk", "rekmogwar",
                "graktuh", "moglukdrag", "brekowak", "ukkardom",
                "dogthruk", "makgrosh", "tugmolar", "brakogum",
                "lokdegrim", "wargokdul", "mukthrog"
            ))
            .build();
        
        return LanguageDefinition.builder()
            .id("orcish")
            .displayName("Orcish")
            .raceId("Orc")
            .maxProficiency(200)
            .distortionProfile(profile)
            .build();
    }
    
    private LanguageDefinition deserializeLanguage(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String displayName = obj.get("displayName").getAsString();
        
        Optional<String> factionId = Optional.empty();
        if (obj.has("factionId") && !obj.get("factionId").isJsonNull()) {
            factionId = Optional.of(obj.get("factionId").getAsString());
        }
        
        Optional<String> raceId = Optional.empty();
        if (obj.has("raceId") && !obj.get("raceId").isJsonNull()) {
            raceId = Optional.of(obj.get("raceId").getAsString());
        }
        
        int maxProficiency = obj.has("maxProficiency") ? obj.get("maxProficiency").getAsInt() : 200;
        
        // Parse distortion profile
        JsonObject distortionObj = obj.getAsJsonObject("distortion");
        String phonemeSet = distortionObj.get("phonemeSet").getAsString();
        
        List<String> syllables = gson.fromJson(
            distortionObj.get("syllablePool"),
            new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
        );
        
        List<String> noiseWords = gson.fromJson(
            distortionObj.get("noiseWords"),
            new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
        );
        
        DistortionProfile profile = new DistortionProfile(phonemeSet, syllables, noiseWords);
        
        return LanguageDefinition.builder()
            .id(id)
            .displayName(displayName)
            .factionId(factionId.orElse(null))
            .raceId(raceId.orElse(null))
            .maxProficiency(maxProficiency)
            .distortionProfile(profile)
            .build();
    }
}
