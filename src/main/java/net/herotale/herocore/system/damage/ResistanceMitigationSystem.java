package net.herotale.herocore.system.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.damage.DamageType;
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.damage.DamageFormulas;

import java.util.Map;
import java.util.Set;

/**
 * Applies resistance mitigation per damage type.
 * <p>
 * <b>Physical damage is NOT mitigated here.</b> Physical mitigation is owned by
 * Hytale's native {@code ArmorDamageReduction} system. HeroCore writes physical
 * resistance values via {@code AttributeDerivationSystem} and lets the engine
 * apply them. This avoids double-mitigation.
 * <p>
 * All other damage types (Projectile, Magical, Elemental) are mitigated here.
 * Runs after {@link FallDamageReductionSystem}.
 */
public class ResistanceMitigationSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    /**
     * Maps each DamageType to the HeroCore stat key for percent-based mitigation.
     * Physical is NOT in this map — handled natively.
     */
    private static final Map<DamageType, String> RESIST_STAT_KEYS = Map.ofEntries(
            Map.entry(DamageType.PROJECTILE, "herocore:projectile_resistance_percent"),
            Map.entry(DamageType.MAGICAL,    "herocore:magic_resist"),
            Map.entry(DamageType.FIRE,       "herocore:fire_resistance_percent"),
            Map.entry(DamageType.ICE,        "herocore:ice_resistance_percent"),
            Map.entry(DamageType.LIGHTNING,  "herocore:lightning_resistance_percent"),
            Map.entry(DamageType.POISON,     "herocore:poison_resistance_percent"),
            Map.entry(DamageType.ARCANE,     "herocore:arcane_resistance_percent")
    );

    /**
     * Fallback map: legacy 0–100 scale attributes for backward compatibility.
     */
    private static final Map<DamageType, String> LEGACY_FALLBACK_KEYS = Map.of(
            DamageType.FIRE,      "herocore:elemental_resist_fire",
            DamageType.ICE,       "herocore:elemental_resist_ice",
            DamageType.LIGHTNING, "herocore:elemental_resist_lightning",
            DamageType.POISON,    "herocore:elemental_resist_poison",
            DamageType.ARCANE,    "herocore:elemental_resist_arcane"
    );

    private final float maxResistanceReduction;

    public ResistanceMitigationSystem() {
        this(0.9f);
    }

    public ResistanceMitigationSystem(float maxResistanceReduction) {
        super(HeroCoreDamageEvent.class);
        this.maxResistanceReduction = maxResistanceReduction;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return null;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, FallDamageReductionSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        if (!DamageFormulas.shouldApplyResistance(event.getDamageType())) return;

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);

        // Look up the resist stat for this damage type
        String resistKey = RESIST_STAT_KEYS.get(event.getDamageType());
        if (resistKey == null) return;

        int resistIndex = HeroCoreStatTypes.getIndex(resistKey);
        float resistValue = (resistIndex >= 0) ? HeroCoreStatTypes.getStatValue(victimRef, resistIndex) : 0.0f;

        // Legacy fallback
        if (resistValue <= 0.0f) {
            String fallbackKey = LEGACY_FALLBACK_KEYS.get(event.getDamageType());
            if (fallbackKey != null) {
                int fallbackIndex = HeroCoreStatTypes.getIndex(fallbackKey);
                if (fallbackIndex >= 0) {
                    resistValue = HeroCoreStatTypes.getStatValue(victimRef, fallbackIndex) / 100.0f;
                }
            }
        }

        // Magic penetration
        float magicPen = 0.0f;
        if (event.getAttacker() != null) {
            int penIndex = HeroCoreStatTypes.getIndex("herocore:magic_penetration");
            if (penIndex >= 0) {
                magicPen = HeroCoreStatTypes.getStatValue(event.getAttacker(), penIndex);
            }
        }

        event.setModifiedAmount(DamageFormulas.applyResistance(
                event.getModifiedAmount(), resistValue, magicPen, maxResistanceReduction));
    }
}
