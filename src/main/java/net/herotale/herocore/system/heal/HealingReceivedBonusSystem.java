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

import java.util.Set;

/**
 * Applies the HEALING_RECEIVED_BONUS stat of the target (entity being healed)
 * to increase (or decrease) incoming healing.
 * Runs after HealingPowerScalingSystem.
 */
public class HealingReceivedBonusSystem extends EntityEventSystem<EntityStore, HeroCoreHealEvent> {

    public HealingReceivedBonusSystem() {
        super(HeroCoreHealEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, HealingPowerScalingSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreHealEvent event) {
        if (event.isCancelled()) return;

        // The target of the heal is the entity that owns this event (chunk entity)
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        int bonusIndex = HeroCoreStatTypes.getIndex("herocore:healing_received_bonus");
        if (bonusIndex < 0) return;

        float bonus = HeroCoreStatTypes.getStatValue(targetRef, bonusIndex);
        event.setModifiedAmount(HealFormulas.applyHealingReceivedBonus(event.getModifiedAmount(), bonus));
    }
}
