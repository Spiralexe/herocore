package net.herotale.herocore.system.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.api.damage.DamageType;
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.damage.DamageFormulas;

import java.util.Set;

/**
 * Reduces fall damage based on the victim's FALL_DAMAGE_REDUCTION stat.
 * Runs after {@link AttackDamageBonusSystem}.
 */
public class FallDamageReductionSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    public FallDamageReductionSystem() {
        super(HeroCoreDamageEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, AttackDamageBonusSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getDamageType() != DamageType.FALL) return;

        // Read FALL_DAMAGE_REDUCTION from the victim (the entity this event is dispatched on)
        int statIndex = HeroCoreStatTypes.getIndex("HeroCoreFallDamageReduction");
        if (statIndex < 0) return;

        float fallReduction = HeroCoreStatTypes.getStatValue(chunk.getReferenceTo(index), statIndex);
        event.setModifiedAmount(DamageFormulas.applyFallDamageReduction(event.getModifiedAmount(), fallReduction));
    }
}
