package net.herotale.herocore.api.damage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * ECS damage event dispatched via {@code commandBuffer.invoke(targetRef, event)}.
 * <p>
 * Extends Hytale's native {@link CancellableEcsEvent} so the engine handles
 * dispatch, ordering, and cancellation automatically. Damage pipeline stages
 * are registered as {@code EntityEventSystem<EntityStore, HeroCoreDamageEvent>}
 * with {@code SystemDependency} for ordering.
 * <p>
 * Uses {@link Ref} for the attacker (live entity handle), not UUID.
 */
public class HeroCoreDamageEvent extends CancellableEcsEvent {

    @Nullable
    private final Ref<EntityStore> attacker;
    private final DamageType damageType;
    private final float rawAmount;
    private float modifiedAmount;
    private boolean isCrit;

    public HeroCoreDamageEvent(@Nullable Ref<EntityStore> attacker, DamageType damageType, float rawAmount) {
        this.attacker = attacker;
        this.damageType = damageType;
        this.rawAmount = rawAmount;
        this.modifiedAmount = rawAmount;
    }

    @Nullable
    public Ref<EntityStore> getAttacker() { return attacker; }
    public DamageType getDamageType() { return damageType; }
    public float getRawAmount() { return rawAmount; }
    public float getModifiedAmount() { return modifiedAmount; }
    public void setModifiedAmount(float amount) { this.modifiedAmount = amount; }
    public boolean isCrit() { return isCrit; }
    public void setCrit(boolean crit) { this.isCrit = crit; }
}
