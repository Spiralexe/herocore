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

import net.herotale.herocore.api.damage.DamageFlag;
import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.damage.DamageFormulas;

import java.util.Random;
import java.util.Set;

/**
 * Rolls critical hits based on the attacker's CRIT_CHANCE and
 * applies the CRIT_DAMAGE_MULTIPLIER.
 * Runs after {@link ResistanceMitigationSystem}.
 */
public class CriticalHitSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    private final float fallbackCritMultiplier;
    private final Random random;

    public CriticalHitSystem(float fallbackCritMultiplier) {
        this(fallbackCritMultiplier, new Random());
    }

    public CriticalHitSystem(float fallbackCritMultiplier, Random random) {
        super(HeroCoreDamageEvent.class);
        this.fallbackCritMultiplier = fallbackCritMultiplier;
        this.random = random;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return null;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, ResistanceMitigationSystem.class));
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getAttacker() == null) return;

        int critChanceIndex = HeroCoreStatTypes.getIndex("herocore:crit_chance");
        if (critChanceIndex < 0) return;

        float critChance = HeroCoreStatTypes.getStatValue(event.getAttacker(), critChanceIndex);
        if (DamageFormulas.rollCrit(critChance, random.nextFloat())) {
            int critMultIndex = HeroCoreStatTypes.getIndex("herocore:crit_damage_multiplier");
            float critMultiplier = (critMultIndex >= 0)
                    ? HeroCoreStatTypes.getStatValue(event.getAttacker(), critMultIndex)
                    : 0.0f;
            if (critMultiplier <= 0) {
                critMultiplier = fallbackCritMultiplier;
            }
            event.setModifiedAmount(DamageFormulas.applyCritMultiplier(event.getModifiedAmount(), critMultiplier));
            event.setCrit(true);
        }
    }
}
