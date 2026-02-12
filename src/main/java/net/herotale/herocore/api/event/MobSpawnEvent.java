package net.herotale.herocore.api.event;

import net.herotale.herocore.api.entity.MobProfile;

import java.util.UUID;

/**
 * Fired when an entity (mob, NPC, boss) spawns in the world.
 * <p>
 * This event is fired BEFORE the entity's StatsComponent is created.
 * Listeners can use this event to:
 * - Create and register the entity's StatsComponent via {@code HeroCore.get().getStatsProvider()}
 * - Apply level-based and tier-based scaling modifiers
 * - Attach additional metadata or behaviors
 */
public class MobSpawnEvent {

    private final UUID entityUUID;
    private final MobProfile profile;
    private final int level;

    public MobSpawnEvent(UUID entityUUID, MobProfile profile, int level) {
        this.entityUUID = entityUUID;
        this.profile = profile;
        this.level = level;
    }

    /**
     * @return The unique ID of the spawned entity
     */
    public UUID getEntityUUID() {
        return entityUUID;
    }

    /**
     * @return The mob profile defining this entity's base attributes and category
     */
    public MobProfile getProfile() {
        return profile;
    }

    /**
     * @return The level at which this entity is spawning
     */
    public int getLevel() {
        return level;
    }
}
