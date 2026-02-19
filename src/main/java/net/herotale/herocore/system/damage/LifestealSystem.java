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
import net.herotale.herocore.api.heal.HealType;
import net.herotale.herocore.api.heal.HeroCoreHealEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.damage.DamageFormulas;

import java.util.Set;

/**
 * Heals the attacker for a percentage of final damage based on the
 * LIFESTEAL stat. Posts a {@link HeroCoreHealEvent} via the command buffer
 * so it flows through the ECS heal pipeline.
 * Runs after {@link CriticalHitSystem}.
 */
public class LifestealSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    public LifestealSystem() {
        super(HeroCoreDamageEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, CriticalHitSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getAttacker() == null) return;

        int lifestealIndex = HeroCoreStatTypes.getIndex("herocore:lifesteal");
        if (lifestealIndex < 0) return;

        float lifesteal = HeroCoreStatTypes.getStatValue(event.getAttacker(), lifestealIndex);
        float healAmount = DamageFormulas.computeLifestealHeal(event.getModifiedAmount(), lifesteal);

        if (healAmount > 0) {
            // Dispatch heal event on the attacker via the command buffer
            HeroCoreHealEvent healEvent = new HeroCoreHealEvent(
                    event.getAttacker(), HealType.PASSIVE, healAmount);
            cb.invoke(event.getAttacker(), healEvent);
        }
    }
}
