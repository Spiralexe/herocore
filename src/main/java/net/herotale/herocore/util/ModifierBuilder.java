package net.herotale.herocore.util;

import net.herotale.herocore.api.attribute.*;

/**
 * Fluent builder for creating {@link AttributeModifier} instances.
 * Reduces boilerplate when registering modifiers from items, skills, etc.
 *
 * <p>Usage:
 * <pre>{@code
 * AttributeModifier mod = ModifierBuilder.of(RPGAttribute.STRENGTH)
 *     .flat(15.0)
 *     .source("equipment", "iron-sword")
 *     .id("iron-sword-str")
 *     .build();
 * }</pre>
 */
public final class ModifierBuilder {

    private final RPGAttribute attribute;
    private double value;
    private ModifierType type = ModifierType.FLAT;
    private String sourceNamespace;
    private String sourceKey;
    private String id;

    private ModifierBuilder(RPGAttribute attribute) {
        this.attribute = attribute;
    }

    public static ModifierBuilder of(RPGAttribute attribute) {
        return new ModifierBuilder(attribute);
    }

    public ModifierBuilder flat(double value) {
        this.value = value;
        this.type = ModifierType.FLAT;
        return this;
    }

    public ModifierBuilder percentAdditive(double value) {
        this.value = value;
        this.type = ModifierType.PERCENT_ADDITIVE;
        return this;
    }

    public ModifierBuilder percentMultiplicative(double value) {
        this.value = value;
        this.type = ModifierType.PERCENT_MULTIPLICATIVE;
        return this;
    }

    public ModifierBuilder override(double value) {
        this.value = value;
        this.type = ModifierType.OVERRIDE;
        return this;
    }

    public ModifierBuilder source(String namespace, String key) {
        this.sourceNamespace = namespace;
        this.sourceKey = key;
        return this;
    }

    public ModifierBuilder id(String id) {
        this.id = id;
        return this;
    }

    public AttributeModifier build() {
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Modifier id is required");
        }
        if (sourceNamespace == null || sourceKey == null) {
            throw new IllegalStateException("Modifier source is required");
        }
        return AttributeModifier.builder()
                .id(id)
                .attribute(attribute)
                .value(value)
                .type(type)
                .source(ModifierSource.of(sourceNamespace + ":" + sourceKey))
                .build();
    }
}
