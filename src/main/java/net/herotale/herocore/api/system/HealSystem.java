package net.herotale.herocore.api.system;

import net.herotale.herocore.api.component.StatsComponent;
import net.herotale.herocore.api.heal.HealEvent;

/**
 * A system that reacts to a single aspect of healing calculation.
 * <p>
 * Each implementation handles one discrete step (e.g., healing power scaling,
 * heal crit rolls). Execution order is declared via {@link SystemOrder}.
 * <p>
 * To inject custom logic (e.g., "Healing Sickness" debuff), a third-party
 * plugin implements {@code HealSystem} and orders itself relative to the
 * default HeroCore systems:
 * <pre>{@code
 * @SystemOrder(after = "herocore:healing_received_bonus", before = "herocore:heal_crit")
 * public class HealingSicknessSystem implements HealSystem { … }
 * }</pre>
 *
 * <h3>Default execution order (via {@code @SystemOrder}):</h3>
 * <ol>
 *   <li>{@code herocore:healing_power_scaling} — scales spell/passive heals by HEALING_POWER</li>
 *   <li>{@code herocore:healing_received_bonus} — target's HEALING_RECEIVED_BONUS multiplier</li>
 *   <li>{@code herocore:heal_crit} — heal crit chance / multiplier roll</li>
 * </ol>
 */
public interface HealSystem extends HeroCoreSystem {

    /**
     * React to a {@link HealEvent} posted on an entity.
     *
     * @param event       the mutable heal event
     * @param healerStats the healer's stats component (may be {@code null} for passive/environmental heals)
     * @param targetStats the target's stats component
     */
    void onHeal(HealEvent event, StatsComponent healerStats, StatsComponent targetStats);
}
