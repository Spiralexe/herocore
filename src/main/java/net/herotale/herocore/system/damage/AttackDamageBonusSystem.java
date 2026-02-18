package net.herotale.herocore.system.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.damage.HeroCoreDamageEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.damage.DamageFormulas;

/**
 * Adds base attack-damage scaling from the attacker's ATTACK_DAMAGE stat.
 * <p>
 * Runs first in the damage pipeline — no ordering dependencies.
 * Reads ATTACK_DAMAGE from the attacker's {@code EntityStatMap} via
 * {@link HeroCoreStatTypes}.
 */
public class AttackDamageBonusSystem extends EntityEventSystem<EntityStore, HeroCoreDamageEvent> {

    public AttackDamageBonusSystem() {
        super(HeroCoreDamageEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return null;
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getAttacker() == null) return;

        // Read ATTACK_DAMAGE from the attacker's EntityStatMap
        int statIndex = HeroCoreStatTypes.getIndex("herocore:attack_damage");
        if (statIndex < 0) return;

        float attackDamage = HeroCoreStatTypes.getStatValue(event.getAttacker(), statIndex);
        event.setModifiedAmount(DamageFormulas.applyAttackDamageBonus(event.getModifiedAmount(), attackDamage));
    }
}
