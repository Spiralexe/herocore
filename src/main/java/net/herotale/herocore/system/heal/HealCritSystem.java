package net.herotale.herocore.system.heal;

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

import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.api.heal.HeroCoreHealEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.heal.HealFormulas;

import java.util.Random;
import java.util.Set;

/**
 * Rolls a critical-hit check for healing events.
 * If the healer's HEAL_CRIT_CHANCE succeeds, multiplies healing by HEAL_CRIT_MULTIPLIER.
 * Falls back to multiplier of 1.5 if the stat is not defined.
 * Runs after HealingReceivedBonusSystem.
 */
public class HealCritSystem extends EntityEventSystem<EntityStore, HeroCoreHealEvent> {

    private static final float FALLBACK_CRIT_MULTIPLIER = 1.5f;

    private final Random random;

    public HealCritSystem() {
        this(new Random());
    }

    public HealCritSystem(Random random) {
        super(HeroCoreHealEvent.class);
        this.random = random;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, HealingReceivedBonusSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreHealEvent event) {
        if (event.isCancelled()) return;

        // Read crit stats from healer if available, otherwise from target
        Ref<EntityStore> statSource = event.getHealer() != null
                ? event.getHealer()
                : chunk.getReferenceTo(index);

        int critChanceIndex = HeroCoreStatTypes.getIndex("herocore:heal_crit_chance");
        int critMultIndex = HeroCoreStatTypes.getIndex("herocore:heal_crit_multiplier");

        float critChance = critChanceIndex >= 0 ? HeroCoreStatTypes.getStatValue(statSource, critChanceIndex) : 0f;
        float critMultiplier = critMultIndex >= 0 ? HeroCoreStatTypes.getStatValue(statSource, critMultIndex) : FALLBACK_CRIT_MULTIPLIER;

        if (critMultiplier <= 0f) {
            critMultiplier = FALLBACK_CRIT_MULTIPLIER;
        }

        if (HealFormulas.rollHealCrit(critChance, random.nextFloat())) {
            event.setModifiedAmount(HealFormulas.applyHealCritMultiplier(event.getModifiedAmount(), critMultiplier));
            event.setCrit(true);
        }
    }
}
