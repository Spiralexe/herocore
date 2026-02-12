package net.herotale.herocore.api.event;

import net.herotale.herocore.api.entity.MobProfile;

import java.util.UUID;

/**
 * Fired when an entity (mob, NPC, boss) dies.
 * <p>
 * This event is fired AFTER the entity has taken lethal damage but BEFORE
 * its StatsComponent is released.
 * <p>
 * Listeners can use this event to:
 * - Clean up entity StatsComponents from your component store
 * - Award XP or loot to the killer
 * - Trigger quest objectives or achievements
 * - Log analytics data
 */
public class MobDeathEvent {

    private final UUID entityUUID;
    private final MobProfile profile;
    private final int level;
    private final UUID killerUUID; // null if environmental death

    public MobDeathEvent(UUID entityUUID, MobProfile profile, int level, UUID killerUUID) {
        this.entityUUID = entityUUID;
        this.profile = profile;
        this.level = level;
        this.killerUUID = killerUUID;
    }

    /**
     * @return The unique ID of the dead entity
     */
    public UUID getEntityUUID() {
        return entityUUID;
    }

    /**
     * @return The mob profile of the dead entity
     */
    public MobProfile getProfile() {
        return profile;
    }

    /**
     * @return The level of the entity at death
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return The UUID of the entity that dealt the killing blow, or null if
     *         the entity died from environmental damage (fall, fire, drowning, etc.)
     */
    public UUID getKillerUUID() {
        return killerUUID;
    }

    /**
     * @return True if the entity was killed by another entity (player or mob)
     */
    public boolean hasKiller() {
        return killerUUID != null;
    }
}
