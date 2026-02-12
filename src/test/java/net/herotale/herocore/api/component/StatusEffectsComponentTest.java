package net.herotale.herocore.api.component;

import net.herotale.herocore.api.status.StatusEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StatusEffectsComponent}.
 */
class StatusEffectsComponentTest {

    private StatusEffectsComponent status;

    @BeforeEach
    void setUp() {
        status = new StatusEffectsComponent();
    }

    @Test
    void applyAndCheck() {
        StatusEffect effect = StatusEffect.builder()
                .id("poison")
                .duration(5000)
                .source("combat:venom-blade")
                .stacks(1)
                .build();
        status.apply(effect);
        assertTrue(status.has("poison"));
        assertEquals(1, status.getStacks("poison"));
    }

    @Test
    void stacksAccumulate() {
        StatusEffect effect = StatusEffect.builder()
                .id("bleed")
                .duration(5000)
                .source("combat:slash")
                .stacks(2)
                .build();
        status.apply(effect);
        status.apply(effect);
        assertEquals(4, status.getStacks("bleed"));
    }

    @Test
    void remove_removesSpecificStatus() {
        status.apply(StatusEffect.builder()
                .id("poison").duration(5000).source("a:b").stacks(1).build());
        status.apply(StatusEffect.builder()
                .id("bleed").duration(5000).source("a:b").stacks(1).build());

        status.remove("poison");
        assertFalse(status.has("poison"));
        assertTrue(status.has("bleed"));
    }

    @Test
    void removeBySource_removesAllFromSource() {
        status.apply(StatusEffect.builder()
                .id("buff1").duration(5000).source("skill:rage").stacks(1).build());
        status.apply(StatusEffect.builder()
                .id("buff2").duration(5000).source("skill:rage").stacks(1).build());
        status.apply(StatusEffect.builder()
                .id("debuff1").duration(5000).source("enemy:curse").stacks(1).build());

        status.removeBySource("skill:rage");
        assertFalse(status.has("buff1"));
        assertFalse(status.has("buff2"));
        assertTrue(status.has("debuff1"));
    }

    @Test
    void clearAll_removesEverything() {
        status.apply(StatusEffect.builder()
                .id("a").duration(5000).source("x:y").stacks(1).build());
        status.apply(StatusEffect.builder()
                .id("b").duration(5000).source("x:y").stacks(1).build());

        status.clearAll();
        assertFalse(status.has("a"));
        assertFalse(status.has("b"));
    }

    @Test
    void has_returnsFalse_forUnknown() {
        assertFalse(status.has("nonexistent"));
    }

    @Test
    void getStacks_returns0_forUnknown() {
        assertEquals(0, status.getStacks("nonexistent"));
    }

    @Test
    void getActiveEffects_returnsAllActive() {
        status.apply(StatusEffect.builder()
                .id("buff").duration(5000).source("a:b").stacks(1).build());
        status.apply(StatusEffect.builder()
                .id("debuff").duration(5000).source("c:d").stacks(2).build());

        Collection<StatusEffect> effects = status.getActiveEffects();
        assertEquals(2, effects.size());
    }

    @Test
    void permanentEffect_neverExpires() {
        status.apply(StatusEffect.builder()
                .id("aura").duration(-1).source("passive:glow").stacks(1).build());

        assertTrue(status.has("aura"));
        status.tick(); // should not remove permanent effects
        assertTrue(status.has("aura"));
    }
}
