package net.herotale.herocore.impl.persistence;

import net.herotale.herocore.api.attribute.RPGAttribute;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AttributeSerializer.
 */
class AttributeSerializerTest {

    @Test
    void roundTrip_preservesValues() {
        Map<RPGAttribute, Double> original = new EnumMap<>(RPGAttribute.class);
        original.put(RPGAttribute.STRENGTH, 42.0);
        original.put(RPGAttribute.MAX_HEALTH, 500.0);
        original.put(RPGAttribute.CRIT_CHANCE, 0.15);

        String json = AttributeSerializer.serialize(original);
        Map<RPGAttribute, Double> restored = AttributeSerializer.deserialize(json);

        assertEquals(original, restored);
    }

    @Test
    void emptyMap_serializesAndDeserializes() {
        Map<RPGAttribute, Double> empty = new EnumMap<>(RPGAttribute.class);
        String json = AttributeSerializer.serialize(empty);
        Map<RPGAttribute, Double> restored = AttributeSerializer.deserialize(json);
        assertTrue(restored.isEmpty());
    }

    @Test
    void unknownAttribute_isSkippedOnDeserialize() {
        String json = "{\"NONEXISTENT_ATTR\": 99.0, \"STRENGTH\": 10.0}";
        Map<RPGAttribute, Double> result = AttributeSerializer.deserialize(json);
        assertEquals(1, result.size());
        assertEquals(10.0, result.get(RPGAttribute.STRENGTH));
    }
}
