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
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.impl.damage.DamageFormulas;

import java.util.Set;

/**
 * Enforces a configurable minimum damage floor.
 * Runs last in the damage pipeline — after all other modifiers.
 */
public class MinimumDamageSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    private final float minimumDamage;

    public MinimumDamageSystem(float minimumDamage) {
        super(HeroCoreDamageEvent.class);
        this.minimumDamage = minimumDamage;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, LifestealSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        event.setModifiedAmount(DamageFormulas.applyMinimumDamage(event.getModifiedAmount(), minimumDamage));
    }
}
