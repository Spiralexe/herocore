package net.herotale.herocore.impl.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads {@link CoreConfig} from hero-core-defaults.json on the classpath.
 */
public final class CoreConfigLoader {

    private static final Logger LOG = Logger.getLogger(CoreConfigLoader.class.getName());
    private static final String RESOURCE_PATH = "/hero-core-defaults.json";

    private CoreConfigLoader() {}

    /**
     * Load the config from classpath. Falls back to sensible defaults on failure.
     */
    public static CoreConfig load() {
        try (InputStream is = CoreConfigLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                LOG.warning("hero-core-defaults.json not found on classpath. Using built-in defaults.");
                return fallback();
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            return parse(root);
        } catch (Exception e) {
            LOG.warning("Failed to parse hero-core-defaults.json: " + e.getMessage() + ". Using built-in defaults.");
            return fallback();
        }
    }

    private static CoreConfig parse(JsonObject root) {
        // Modifier stacking
        JsonObject ms = root.getAsJsonObject("modifierStacking");
        CoreConfig.ModifierStackingConfig modifierStacking = new CoreConfig.ModifierStackingConfig(
                getDouble(ms, "maxPercentAdditive", 3.0),
                getDouble(ms, "multiplicativeCap", 5.0),
                getString(ms, "overridePriority", "HIGHEST_WINS")
        );

        // Damage system defaults
        JsonObject dp = root.getAsJsonObject("damage");
        Map<String, String> resistMapping = new HashMap<>();
        if (dp != null && dp.has("resistanceMapping")) {
            for (Map.Entry<String, JsonElement> entry : dp.getAsJsonObject("resistanceMapping").entrySet()) {
                resistMapping.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        CoreConfig.DamageConfig damage = new CoreConfig.DamageConfig(
                getDouble(dp, "critDamageBaseMultiplier", 1.5),
                getDouble(dp, "minimumDamage", 1.0),
                getDouble(dp, "maxResistanceReduction", 0.9),
                resistMapping
        );

        // Heal system defaults
        JsonObject hp = root.getAsJsonObject("heal");
        CoreConfig.HealConfig heal = new CoreConfig.HealConfig(
                getDouble(hp, "healCritBaseMultiplier", 1.5),
                getBoolean(hp, "healingPowerScalesRegenTick", false),
                getBoolean(hp, "healingPowerScalesPassive", true)
        );

        // Resource regen
        JsonObject rr = root.getAsJsonObject("resourceRegen");
        CoreConfig.ResourceRegenConfig resourceRegen = new CoreConfig.ResourceRegenConfig(
                getLong(rr, "tickIntervalMs", 2000),
                getDouble(rr, "outOfCombatBonusMultiplier", 3.0),
                getLong(rr, "combatTimeoutMs", 8000)
        );

        // Leveling
        JsonObject lv = root.getAsJsonObject("leveling");
        Map<String, Double> sourceWeights = new HashMap<>();
        if (lv != null && lv.has("sourceWeights")) {
            for (Map.Entry<String, JsonElement> entry : lv.getAsJsonObject("sourceWeights").entrySet()) {
                sourceWeights.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
        CoreConfig.LevelingConfig leveling = new CoreConfig.LevelingConfig(
                getInt(lv, "defaultMaxLevel", 60),
                sourceWeights
        );

        // Attribute derivation
        JsonObject ad = root.getAsJsonObject("attributeDerivation");
        CoreConfig.AttributeDerivationConfig attributeDerivation = parseAttributeDerivation(ad);

        // Attribute defaults
        Map<String, Double> attrDefaults = new HashMap<>();
        if (root.has("attributes")) {
            JsonObject attrs = root.getAsJsonObject("attributes");
            if (attrs.has("defaults")) {
                for (Map.Entry<String, JsonElement> entry : attrs.getAsJsonObject("defaults").entrySet()) {
                    attrDefaults.put(entry.getKey(), entry.getValue().getAsDouble());
                }
            }
        }

        // System overrides — per-system enable/disable
        Map<String, Boolean> systemOverrides = new HashMap<>();
        if (root.has("systems")) {
            JsonObject systems = root.getAsJsonObject("systems");
            for (Map.Entry<String, JsonElement> entry : systems.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject systemConfig = entry.getValue().getAsJsonObject();
                    if (systemConfig.has("enabled")) {
                        systemOverrides.put(entry.getKey(), systemConfig.get("enabled").getAsBoolean());
                    }
                }
            }
        }

        return new CoreConfig(
                modifierStacking,
                damage,
                heal,
                resourceRegen,
                leveling,
                attributeDerivation,
                attrDefaults,
                systemOverrides
        );
    }

    private static CoreConfig fallback() {
        return new CoreConfig(
                new CoreConfig.ModifierStackingConfig(3.0, 5.0, "HIGHEST_WINS"),
                new CoreConfig.DamageConfig(1.5, 1.0, 0.9, Map.of(
                        "PHYSICAL", "ARMOR",
                        "FIRE", "ELEMENTAL_RESIST_FIRE",
                        "ICE", "ELEMENTAL_RESIST_ICE",
                        "LIGHTNING", "ELEMENTAL_RESIST_LIGHTNING",
                        "POISON", "ELEMENTAL_RESIST_POISON",
                        "ARCANE", "ELEMENTAL_RESIST_ARCANE"
                )),
                new CoreConfig.HealConfig(1.5, false, true),
                new CoreConfig.ResourceRegenConfig(2000, 3.0, 8000),
                new CoreConfig.LevelingConfig(60, Map.of(
                        "KILL", 1.0, "QUEST", 1.0,
                        "CRAFTING", 0.8, "GATHERING", 0.7,
                        "COMBAT_USE", 0.5, "EXPLORATION", 0.6,
                        "SOCIAL", 1.0, "ADMIN", 1.0
                )),
                defaultAttributeDerivation(),
                Map.ofEntries(
                        Map.entry("MAX_HEALTH", 100.0), Map.entry("MAX_MANA", 50.0), Map.entry("MAX_STAMINA", 100.0),
                        Map.entry("HEALTH_REGEN", 1.0), Map.entry("MANA_REGEN", 2.0), Map.entry("STAMINA_REGEN", 5.0),
                        Map.entry("MOVE_SPEED", 1.0), Map.entry("ATTACK_SPEED", 1.0), Map.entry("MINING_SPEED", 1.0),
                        Map.entry("CRIT_CHANCE", 0.05), Map.entry("CRIT_DAMAGE_MULTIPLIER", 1.5),
                        Map.entry("HEAL_CRIT_CHANCE", 0.0), Map.entry("HEAL_CRIT_MULTIPLIER", 1.5)
                ),
                Map.of() // systemOverrides — all enabled by default
        );
    }

            private static CoreConfig.AttributeDerivationConfig parseAttributeDerivation(JsonObject ad) {
            JsonObject vit = ad != null ? ad.getAsJsonObject("vitality") : null;
            CoreConfig.VitalityDerivation vitality = new CoreConfig.VitalityDerivation(
                getDouble(vit, "healthPerPoint", 10.0),
                getDouble(vit, "healthBase", 100.0),
                getDouble(vit, "healthRegenPerPoint", 0.1),
                getDouble(vit, "healthRegenBase", 1.0),
                getDouble(vit, "armorPercentPerPoint", 0.002),
                getDouble(vit, "fallDamageReductionPerPoint", 0.01),
                getDouble(vit, "fallDamageReductionCap", 0.50)
            );

            JsonObject str = ad != null ? ad.getAsJsonObject("strength") : null;
            CoreConfig.StrengthDerivation strength = new CoreConfig.StrengthDerivation(
                getDouble(str, "baseAttackDamagePerPoint", 0.5),
                getDouble(str, "blockStrengthPerPoint", 2.0)
            );

            JsonObject dex = ad != null ? ad.getAsJsonObject("dexterity") : null;
            CoreConfig.DexterityDerivation dexterity = new CoreConfig.DexterityDerivation(
                getDouble(dex, "critChancePerPoint", 0.005),
                getDouble(dex, "attackSpeedPercentPerPoint", 0.01),
                getDouble(dex, "dodgeRatingPerPoint", 1.0)
            );

            JsonObject intel = ad != null ? ad.getAsJsonObject("intelligence") : null;
            CoreConfig.IntelligenceDerivation intelligence = new CoreConfig.IntelligenceDerivation(
                getDouble(intel, "spellPowerPercentPerPoint", 0.01),
                getDouble(intel, "manaPerPoint", 10.0),
                getDouble(intel, "manaBase", 50.0),
                getDouble(intel, "spellCritChancePerPoint", 0.004),
                getDouble(intel, "spellCritMultiplierPerPoint", 0.002)
            );

            JsonObject faith = ad != null ? ad.getAsJsonObject("faith") : null;
            CoreConfig.FaithDerivation faithDerivation = new CoreConfig.FaithDerivation(
                getDouble(faith, "healingPowerPercentPerPoint", 0.015),
                getDouble(faith, "manaPerPoint", 8.0),
                getDouble(faith, "manaBase", 30.0),
                getDouble(faith, "manaRegenPerPoint", 0.15),
                getDouble(faith, "manaRegenBase", 0.5),
                getDouble(faith, "healCritChancePerPoint", 0.003),
                getDouble(faith, "shieldStrengthPerPoint", 3.0),
                getDouble(faith, "buffStrengthPercentPerPoint", 0.005)
            );

            JsonObject res = ad != null ? ad.getAsJsonObject("resolve") : null;
            CoreConfig.ResolveDerivation resolve = new CoreConfig.ResolveDerivation(
                getDouble(res, "ccResistancePerPoint", 0.01),
                getDouble(res, "ccResistanceCap", 0.75),
                getDouble(res, "debuffResistancePerPoint", 0.008),
                getDouble(res, "debuffResistanceCap", 0.60),
                getDouble(res, "threatGenerationPercentPerPoint", 0.02),
                getDouble(res, "staminaRegenPerPoint", 0.1),
                getDouble(res, "magicResistPercentPerPoint", 0.003)
            );

            return new CoreConfig.AttributeDerivationConfig(
                vitality,
                strength,
                dexterity,
                intelligence,
                faithDerivation,
                resolve
            );
            }

            private static CoreConfig.AttributeDerivationConfig defaultAttributeDerivation() {
            return parseAttributeDerivation(null);
            }

    private static double getDouble(JsonObject obj, String key, double fallback) {
        if (obj != null && obj.has(key)) return obj.get(key).getAsDouble();
        return fallback;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        if (obj != null && obj.has(key)) return obj.get(key).getAsInt();
        return fallback;
    }

    private static long getLong(JsonObject obj, String key, long fallback) {
        if (obj != null && obj.has(key)) return obj.get(key).getAsLong();
        return fallback;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj != null && obj.has(key)) return obj.get(key).getAsBoolean();
        return fallback;
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key)) return obj.get(key).getAsString();
        return fallback;
    }
}
