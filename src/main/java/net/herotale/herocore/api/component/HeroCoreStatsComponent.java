package net.herotale.herocore.api.component;

import com.hypixel.hytale.component.Component;
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

    private double strength = 0.0;
    private double dexterity = 0.0;
    private double intelligence = 0.0;
    private double faith = 0.0;
    private double vitality = 0.0;
    private double resolve = 0.0;

    public HeroCoreStatsComponent() {
    }

    public HeroCoreStatsComponent(double strength, double dexterity, double intelligence,
                                  double faith, double vitality, double resolve) {
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.faith = faith;
        this.vitality = vitality;
        this.resolve = resolve;
    }

    // ── Primary Attributes ────────────────────────────────────────────

    public double getStrength() { return strength; }
    public void setStrength(double value) { this.strength = value; }

    public double getDexterity() { return dexterity; }
    public void setDexterity(double value) { this.dexterity = value; }

    public double getIntelligence() { return intelligence; }
    public void setIntelligence(double value) { this.intelligence = value; }

    public double getFaith() { return faith; }
    public void setFaith(double value) { this.faith = value; }

    public double getVitality() { return vitality; }
    public void setVitality(double value) { this.vitality = value; }

    public double getResolve() { return resolve; }
    public void setResolve(double value) { this.resolve = value; }

    // ── Component interface ────────────────────────────────────────────

    @Override
    public HeroCoreStatsComponent clone() {
        return new HeroCoreStatsComponent(strength, dexterity, intelligence, faith, vitality, resolve);
    }

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
