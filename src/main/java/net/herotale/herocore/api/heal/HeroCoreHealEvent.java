package net.herotale.herocore.api.heal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * ECS heal event dispatched via {@code commandBuffer.invoke(targetRef, event)}.
 * <p>
 * Extends Hytale's native {@link CancellableEcsEvent} so the engine handles
 * dispatch, ordering, and cancellation automatically. Heal pipeline stages
 * are registered as {@code EntityEventSystem<EntityStore, HeroCoreHealEvent>}
 * with {@code SystemDependency} for ordering.
 */
public class HeroCoreHealEvent extends CancellableEcsEvent {

    @Nullable
    private final Ref<EntityStore> healer;
    private final HealType healType;
    private final float rawAmount;
    private float modifiedAmount;
    private boolean isCrit;

    public HeroCoreHealEvent(@Nullable Ref<EntityStore> healer, HealType healType, float rawAmount) {
        this.healer = healer;
        this.healType = healType;
        this.rawAmount = rawAmount;
        this.modifiedAmount = rawAmount;
    }

    @Nullable
    public Ref<EntityStore> getHealer() { return healer; }
    public HealType getHealType() { return healType; }
    public float getRawAmount() { return rawAmount; }
    public float getModifiedAmount() { return modifiedAmount; }
    public void setModifiedAmount(float amount) { this.modifiedAmount = amount; }
    public boolean isCrit() { return isCrit; }
    public void setCrit(boolean crit) { this.isCrit = crit; }
}
