package net.herotale.herocore.api.zone;

/**
 * How a zone modifier is triggered.
 */
public enum ZoneTrigger {
    /** Applied when a player enters the defined region boundary. */
    REGION_ENTRY,
    /** Applied when a player is within a specified radius of a point. */
    PROXIMITY
}
