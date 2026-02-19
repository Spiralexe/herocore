package net.herotale.herocore.system.heal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.herotale.herocore.api.component.HeroCoreStatsComponent;
import net.herotale.herocore.api.heal.HeroCoreHealEvent;
import net.herotale.herocore.impl.HeroCoreStatTypes;
import net.herotale.herocore.impl.heal.HealFormulas;

/**
 * Scales healing amounts by the healer's HEALING_POWER stat.
 * Applies to spell and passive heals. Optionally scales regen ticks.
 * Runs first among heal systems — no ordering dependencies.
 */
public class HealingPowerScalingSystem extends EntityEventSystem<EntityStore, HeroCoreHealEvent> {

    private final boolean scalesRegenTick;

    public HealingPowerScalingSystem(boolean scalesRegenTick) {
        super(HeroCoreHealEvent.class);
        this.scalesRegenTick = scalesRegenTick;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HeroCoreStatsComponent.getComponentType();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> cb, HeroCoreHealEvent event) {
        if (event.isCancelled()) return;
        if (event.getHealer() == null) return;

        int hpIndex = HeroCoreStatTypes.getIndex("herocore:healing_power");
        if (hpIndex < 0) return;

        float healingPower = HeroCoreStatTypes.getStatValue(event.getHealer(), hpIndex);
        event.setModifiedAmount(HealFormulas.applyHealingPowerScaling(
                event.getModifiedAmount(), healingPower, event.getHealType(), scalesRegenTick));
    }
}
