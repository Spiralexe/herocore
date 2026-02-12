package net.herotale.herocore.api.leveling;

/**
 * Enum representing where XP originated from, for analytics and multiplier rules.
 */
public enum XPSource {
    KILL,
    QUEST,
    CRAFTING,
    GATHERING,
    COMBAT_USE,
    EXPLORATION,
    SOCIAL,
    ADMIN
}
