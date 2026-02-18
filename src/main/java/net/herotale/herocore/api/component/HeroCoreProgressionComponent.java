package net.herotale.herocore.api.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * ECS Component: Tracks an entity's level, XP, and progression state.
 * <p>
 * Hytale has no native leveling system, so HeroCore owns this entirely.
 * Persisted via {@link BuilderCodec}.
 */
public class HeroCoreProgressionComponent implements Component<EntityStore> {

    private int level = 1;
    private float currentXP = 0f;
    private float xpToNextLevel = 100f;

    /** Default constructor required by registration factory. */
    public HeroCoreProgressionComponent() {}

    /** Copy constructor required by {@link #clone()}. */
    public HeroCoreProgressionComponent(HeroCoreProgressionComponent other) {
        this.level = other.level;
        this.currentXP = other.currentXP;
        this.xpToNextLevel = other.xpToNextLevel;
    }

    @Override
    public Component<EntityStore> clone() {
        return new HeroCoreProgressionComponent(this);
    }

    // ── Static ComponentType handle ──────────────────────────────────
    private static ComponentType<EntityStore, HeroCoreProgressionComponent> type;

    public static ComponentType<EntityStore, HeroCoreProgressionComponent> getComponentType() {
        return type;
    }

    public static void setComponentType(ComponentType<EntityStore, HeroCoreProgressionComponent> t) {
        type = t;
    }

    // ── BuilderCodec ─────────────────────────────────────────────────
    public static final BuilderCodec<HeroCoreProgressionComponent> CODEC = BuilderCodec
            .builder(HeroCoreProgressionComponent.class, HeroCoreProgressionComponent::new)
            .append(new KeyedCodec<>("HC_Level",         Codec.INTEGER), HeroCoreProgressionComponent::setLevel,         HeroCoreProgressionComponent::getLevel).add()
            .append(new KeyedCodec<>("HC_CurrentXP",     Codec.FLOAT),   HeroCoreProgressionComponent::setCurrentXP,     HeroCoreProgressionComponent::getCurrentXP).add()
            .append(new KeyedCodec<>("HC_XPToNextLevel", Codec.FLOAT),   HeroCoreProgressionComponent::setXpToNextLevel, HeroCoreProgressionComponent::getXpToNextLevel).add()
            .build();

    // ── API ──────────────────────────────────────────────────────────

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public float getCurrentXP() { return currentXP; }
    public void setCurrentXP(float currentXP) { this.currentXP = currentXP; }

    public float getXpToNextLevel() { return xpToNextLevel; }
    public void setXpToNextLevel(float xpToNextLevel) { this.xpToNextLevel = xpToNextLevel; }

    /**
     * Add XP to the current total.
     *
     * @param amount XP to add
     */
    public void addXP(float amount) {
        this.currentXP += amount;
    }

    /**
     * Perform a level-up: increment level, subtract XP cost, recalculate next threshold.
     *
     * @param nextLevelXP the XP required for the new level (computed by LevelingCurves)
     */
    public void levelUp(float nextLevelXP) {
        currentXP -= xpToNextLevel;
        level++;
        xpToNextLevel = nextLevelXP;
    }

    @Override
    public String toString() {
        return "HeroCoreProgressionComponent{level=" + level +
                ", xp=" + currentXP + "/" + xpToNextLevel + '}';
    }
}
