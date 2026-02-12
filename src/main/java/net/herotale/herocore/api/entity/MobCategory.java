package net.herotale.herocore.api.entity;

/**
 * Qualitative classification of spawned mobs/NPCs.
 * Used by DifficultyTier for gap-level scaling modifiers.
 */
public enum MobCategory {
    STANDARD,
    ELITE,
    BOSS,
    NPC_FRIENDLY,
    NPC_NEUTRAL
}
