package net.herotale.herocore.api.system;

import net.herotale.herocore.api.damage.DamageEvent;
import net.herotale.herocore.api.component.StatsComponent;

/**
 * A system that reacts to a single aspect of damage calculation.
 * <p>
 * Each implementation handles one discrete step (e.g., armor mitigation, crit rolls).
 * Execution order is declared via {@link SystemOrder} — <b>not</b> a numeric priority.
 * The Hytale event bus invokes systems in the topological order of their declared
 * {@code after}/{@code before} constraints.
 * <p>
 * Third-party developers can inject custom logic between any two HeroCore steps
 * simply by declaring their own system with the appropriate ordering:
 * <pre>{@code
 * @SystemOrder(
 *     after  = "herocore:resistance_mitigation",
 *     before = "herocore:critical_hit"
 * )
 * public class ShieldAbsorptionSystem implements DamageSystem { … }
 * }</pre>
 *
 * <h3>Default execution order (via {@code @SystemOrder}):</h3>
 * <ol>
 *   <li>{@code herocore:attack_damage_bonus} — adds base attack damage scaling</li>
 *   <li>{@code herocore:fall_damage_reduction} — reduces fall damage from Vitality</li>
 *   <li>{@code herocore:resistance_mitigation} — armor / elemental resist</li>
 *   <li>{@code herocore:critical_hit} — crit chance / multiplier roll</li>
 *   <li>{@code herocore:lifesteal} — heals attacker based on final damage</li>
 *   <li>{@code herocore:minimum_damage} — enforces configured damage floor</li>
 * </ol>
 */
public interface DamageSystem extends HeroCoreSystem {

    /**
     * React to a {@link DamageEvent} posted on an entity.
     * <p>
     * Implementations read and mutate the event (e.g., reduce
     * {@code modifiedAmount}, add flags, cancel). They should
     * <b>not</b> call other systems — the event bus handles ordering.
     *
     * @param event         the mutable damage event
     * @param attackerStats the attacker's stats component (may be {@code null} for environmental damage)
     * @param victimStats   the victim's stats component
     */
    void onDamage(DamageEvent event, StatsComponent attackerStats, StatsComponent victimStats);
}
