package net.herotale.herocore.api.event;

import java.util.UUID;

/**
 * Fired when an entity morphs/transforms.
 */
public class MorphEvent {

    private final UUID entity;
    private final String fromMorphId;
    private final String toMorphId;

    public MorphEvent(UUID entity, String fromMorphId, String toMorphId) {
        this.entity = entity;
        this.fromMorphId = fromMorphId;
        this.toMorphId = toMorphId;
    }

    public UUID getEntity() {
        return entity;
    }

    public String getFromMorphId() {
        return fromMorphId;
    }

    public String getToMorphId() {
        return toMorphId;
    }
}
