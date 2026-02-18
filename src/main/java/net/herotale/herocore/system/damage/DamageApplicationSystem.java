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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.CombatStateComponent;
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;

import java.util.Set;

/**
 * Final stage of the damage pipeline — applies the computed damage to the
 * target entity's health stat via {@link EntityStatMap}.
 * <p>
 * Runs AFTER {@link MinimumDamageSystem} so all modifiers (crit, resistance,
 * lifesteal, min-damage floor) have already been applied.
 * <p>
 * Also enters the entity into combat state via {@link CombatStateComponent}.
 */
public class DamageApplicationSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    public DamageApplicationSystem() {
        super(HeroCoreDamageEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return null;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, MinimumDamageSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;

        float finalDamage = event.getModifiedAmount();
        if (finalDamage <= 0f) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        // Apply damage to health via EntityStatMap
        EntityStatMap statMap = store.getComponent(targetRef,
                EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap != null) {
            int healthIndex = DefaultEntityStatTypes.getHealth();
            if (healthIndex >= 0) {
                statMap.subtractStatValue(healthIndex, finalDamage);
            }
        }

        // Mark entity as in combat
        CombatStateComponent combat = store.getComponent(targetRef,
                CombatStateComponent.getComponentType());
        if (combat != null) {
            combat.enterCombat();
        }
    }
}
