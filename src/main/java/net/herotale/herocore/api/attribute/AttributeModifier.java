package net.herotale.herocore.api.attribute;

import java.util.Objects;

/**
 * An immutable attribute modifier with a unique ID, source, type, and value.
 * Built via {@link #builder()}.
 */
public final class AttributeModifier {

    private final String id;
    private final RPGAttribute attribute;
    private final double value;
    private final ModifierType type;
    private final ModifierSource source;

    private AttributeModifier(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Modifier id must not be null");
        this.attribute = Objects.requireNonNull(builder.attribute, "Modifier attribute must not be null");
        this.value = builder.value;
        this.type = Objects.requireNonNull(builder.type, "Modifier type must not be null");
        this.source = Objects.requireNonNull(builder.source, "Modifier source must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public RPGAttribute getAttribute() {
        return attribute;
    }

    public double getValue() {
        return value;
    }

    public ModifierType getType() {
        return type;
    }

    public ModifierSource getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeModifier other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AttributeModifier{id=" + id + ", attr=" + attribute + ", value=" + value
                + ", type=" + type + ", source=" + source.getKey() + "}";
    }

    public static final class Builder {

        private String id;
        private RPGAttribute attribute;
        private double value;
        private ModifierType type;
        private ModifierSource source;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder attribute(RPGAttribute attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder type(ModifierType type) {
            this.type = type;
            return this;
        }

        public Builder source(ModifierSource source) {
            this.source = source;
            return this;
        }

        public AttributeModifier build() {
            return new AttributeModifier(this);
        }
    }
}
