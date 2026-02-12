package net.herotale.herocore.impl.persistence;

import net.herotale.herocore.api.attribute.RPGAttribute;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

/**
 * Serializes / deserializes base attribute values to/from JSON.
 * Only base values are persisted — modifiers are re-applied at runtime by their sources.
 */
public class AttributeSerializer {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type MAP_TYPE = new TypeToken<Map<String, Double>>() {}.getType();

    /**
     * Serialize base attribute values to a JSON string.
     *
     * @param baseValues map of attribute → base value
     * @return JSON string
     */
    public static String serialize(Map<RPGAttribute, Double> baseValues) {
        Map<String, Double> stringMap = new java.util.LinkedHashMap<>();
        for (Map.Entry<RPGAttribute, Double> entry : baseValues.entrySet()) {
            stringMap.put(entry.getKey().name(), entry.getValue());
        }
        return GSON.toJson(stringMap);
    }

    /**
     * Deserialize base attribute values from a JSON string.
     *
     * @param json JSON string
     * @return map of attribute → base value
     */
    public static Map<RPGAttribute, Double> deserialize(String json) {
        Map<String, Double> stringMap = GSON.fromJson(json, MAP_TYPE);
        Map<RPGAttribute, Double> result = new EnumMap<>(RPGAttribute.class);
        if (stringMap != null) {
            for (Map.Entry<String, Double> entry : stringMap.entrySet()) {
                try {
                    RPGAttribute attr = RPGAttribute.valueOf(entry.getKey());
                    result.put(attr, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    // Unknown attribute — skip (may have been removed in an update)
                }
            }
        }
        return result;
    }
}
