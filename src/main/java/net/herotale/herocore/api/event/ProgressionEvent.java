package net.herotale.herocore.api.event;

import java.util.Objects;
import java.util.UUID;

/**
 * A lightweight pub/sub event for "something noteworthy happened".
 * Quest systems, achievement systems, guild challenges, and any observer
 * can subscribe without knowing about each other.
 */
public class ProgressionEvent {

    private final UUID player;
    private String category;
    private String tag;
    private int quantity;

    public ProgressionEvent(UUID player) {
        this.player = Objects.requireNonNull(player);
        this.quantity = 1;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getCategory() {
        return category;
    }

    public ProgressionEvent category(String category) {
        this.category = category;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public ProgressionEvent tag(String tag) {
        this.tag = tag;
        return this;
    }

    public int getQuantity() {
        return quantity;
    }

    public ProgressionEvent quantity(int quantity) {
        this.quantity = quantity;
        return this;
    }
}
