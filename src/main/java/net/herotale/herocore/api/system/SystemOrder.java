package net.herotale.herocore.api.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the execution ordering of a {@link HeroCoreSystem} relative to other systems.
 * <p>
 * This mirrors Hytale's native system scheduling.  Third-party plugins can inject
 * their own systems by referencing HeroCore system IDs in their own ordering
 * annotations — e.g., {@code @SystemOrder(after = "herocore:resistance_mitigation")}
 * to run between armor mitigation and the next step.
 * <p>
 * When Hytale stabilises its built-in {@code @System} ordering API, this
 * annotation should be replaced or adapted to delegate to the native mechanism.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @SystemOrder(after = "herocore:attack_damage_bonus")
 * public class ResistanceMitigationSystem implements DamageSystem { … }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SystemOrder {

    /**
     * System IDs that this system must run <b>after</b>.
     * The engine guarantees those systems have already processed the event
     * before this system sees it.
     */
    String[] after() default {};

    /**
     * System IDs that this system must run <b>before</b>.
     * The engine guarantees this system processes the event before those
     * listed here.
     */
    String[] before() default {};
}
