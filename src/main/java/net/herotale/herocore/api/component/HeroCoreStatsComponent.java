package net.herotale.herocore.api.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * <b>Thin ECS Component:</b> Holds only primary RPG attribute base values.
 * 
 * <b>NOT a full attribute engine.</b> This component is data-only storage for:
 * - Strength
 * - Dexterity
 * - Intelligence
 * - Faith
 * - Vitality
 * - Resolve
 * 
 * <b>All derived values (secondary attributes) are computed by {@link
 * net.herotale.herocore.impl.system.AttributeDerivationSystem} and written directly
 * into the entity's {@link com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap}
 * as {@link com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier} entries.</b>
 * 
 * This means:
 * - No parallel modifier stacks in this component
 * - No custom computation logic here
 * - Single source of truth: EntityStatMap for all stat values in gameplay
 * - Bridges are eliminated (no syncing between two systems)
 * 
 * The derivation system ensures everything flows into Hytale's native stat engine,
 * which is the only place gameplay systems read from.
 */
public class HeroCoreStatsComponent implements Component<EntityStore> {

    private float strength;
    private float dexterity;
    private float intelligence;
    private float faith;
    private float vitality;
    private float resolve;

    /** Default constructor required by registration factory. */
    public HeroCoreStatsComponent() {}

    /** Copy constructor required by {@link #clone()}. */
    public HeroCoreStatsComponent(HeroCoreStatsComponent other) {
        this.strength     = other.strength;
        this.dexterity    = other.dexterity;
        this.intelligence = other.intelligence;
        this.faith        = other.faith;
        this.vitality     = other.vitality;
        this.resolve      = other.resolve;
    }

    /** Convenience constructor for tests and direct initialization. */
    public HeroCoreStatsComponent(float strength, float dexterity, float intelligence,
                                  float faith, float vitality, float resolve) {
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.faith = faith;
        this.vitality = vitality;
        this.resolve = resolve;
    }

    @Override
    public Component<EntityStore> clone() {
        return new HeroCoreStatsComponent(this);
    }

    // ── Static ComponentType handle ──────────────────────────────────
    private static ComponentType<EntityStore, HeroCoreStatsComponent> type;

    public static ComponentType<EntityStore, HeroCoreStatsComponent> getComponentType() {
        return type;
    }

    public static void setComponentType(ComponentType<EntityStore, HeroCoreStatsComponent> t) {
        type = t;
    }

    // ── BuilderCodec ─────────────────────────────────────────────────
    // HC_ prefix for global uniqueness across all KeyedCodec identifiers
    public static final BuilderCodec<HeroCoreStatsComponent> CODEC = BuilderCodec
            .builder(HeroCoreStatsComponent.class, HeroCoreStatsComponent::new)
            .append(new KeyedCodec<>("HC_Strength",     Codec.FLOAT), HeroCoreStatsComponent::setStrength,     HeroCoreStatsComponent::getStrength).add()
            .append(new KeyedCodec<>("HC_Dexterity",    Codec.FLOAT), HeroCoreStatsComponent::setDexterity,    HeroCoreStatsComponent::getDexterity).add()
            .append(new KeyedCodec<>("HC_Intelligence", Codec.FLOAT), HeroCoreStatsComponent::setIntelligence, HeroCoreStatsComponent::getIntelligence).add()
            .append(new KeyedCodec<>("HC_Faith",        Codec.FLOAT), HeroCoreStatsComponent::setFaith,        HeroCoreStatsComponent::getFaith).add()
            .append(new KeyedCodec<>("HC_Vitality",     Codec.FLOAT), HeroCoreStatsComponent::setVitality,     HeroCoreStatsComponent::getVitality).add()
            .append(new KeyedCodec<>("HC_Resolve",      Codec.FLOAT), HeroCoreStatsComponent::setResolve,      HeroCoreStatsComponent::getResolve).add()
            .build();

    // ── Primary Attributes ────────────────────────────────────────────

    public float getStrength() { return strength; }
    public void setStrength(float value) { this.strength = value; }

    public float getDexterity() { return dexterity; }
    public void setDexterity(float value) { this.dexterity = value; }

    public float getIntelligence() { return intelligence; }
    public void setIntelligence(float value) { this.intelligence = value; }

    public float getFaith() { return faith; }
    public void setFaith(float value) { this.faith = value; }

    public float getVitality() { return vitality; }
    public void setVitality(float value) { this.vitality = value; }

    public float getResolve() { return resolve; }
    public void setResolve(float value) { this.resolve = value; }

    @Override
    public String toString() {
        return "HeroCoreStatsComponent{" +
                "STR=" + strength +
                ", DEX=" + dexterity +
                ", INT=" + intelligence +
                ", FAITH=" + faith +
                ", VIT=" + vitality +
                ", RES=" + resolve +
                '}';
    }
}
