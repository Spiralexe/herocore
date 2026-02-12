package net.herotale.herocore.impl.bridge;

import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridge that reads MOVEMENT_SPEED from a {@link StatsComponent} and
 * translates it into Hytale ECS component updates.
 * <p>
 * Call {@link #apply(StatsComponent)} whenever the entity's stats change.
 * This is no longer a listener — the calling system decides when to invoke it.
 */
public class MovementSpeedBridge {

    private static final Logger LOG = Logger.getLogger(MovementSpeedBridge.class.getName());
    private final UUID entityUuid;

    public MovementSpeedBridge(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public RPGAttribute attribute() {
        return RPGAttribute.MOVE_SPEED;
    }

    /**
     * Push the current MOVE_SPEED value from stats into Hytale's movement component.
     */
    public void apply(StatsComponent stats) {
        double newValue = stats.getValue(RPGAttribute.MOVE_SPEED);

        PlayerRef playerRef = Universe.get().getPlayer(entityUuid);
        if (playerRef == null) {
            LOG.fine(() -> String.format("MovementSpeedBridge[%s]: missing PlayerRef", entityUuid));
            return;
        }

        MovementManager movementManager = playerRef.getComponent(MovementManager.getComponentType());
        if (movementManager == null) {
            LOG.fine(() -> String.format("MovementSpeedBridge[%s]: MovementManager missing", entityUuid));
            return;
        }

        MovementSettings defaults = movementManager.getDefaultSettings();
        MovementSettings settings = movementManager.getSettings();
        if (defaults == null || settings == null) {
            LOG.fine(() -> String.format("MovementSpeedBridge[%s]: MovementSettings missing", entityUuid));
            return;
        }

        float requestedMultiplier = (float) newValue;
        float clampedMultiplier = requestedMultiplier;
        if (settings.maxSpeedMultiplier > 0.0F) {
            clampedMultiplier = Math.min(clampedMultiplier, settings.maxSpeedMultiplier);
        }
        if (settings.minSpeedMultiplier > 0.0F) {
            clampedMultiplier = Math.max(clampedMultiplier, settings.minSpeedMultiplier);
        }

        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier * clampedMultiplier;
        settings.backwardWalkSpeedMultiplier = defaults.backwardWalkSpeedMultiplier * clampedMultiplier;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier * clampedMultiplier;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier * clampedMultiplier;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier * clampedMultiplier;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier * clampedMultiplier;
        settings.forwardCrouchSpeedMultiplier = defaults.forwardCrouchSpeedMultiplier * clampedMultiplier;
        settings.backwardCrouchSpeedMultiplier = defaults.backwardCrouchSpeedMultiplier * clampedMultiplier;
        settings.strafeCrouchSpeedMultiplier = defaults.strafeCrouchSpeedMultiplier * clampedMultiplier;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier * clampedMultiplier;

        movementManager.update(playerRef.getPacketHandler());
        float loggedMultiplier = clampedMultiplier;
        LOG.fine(
            () -> String.format(
                "MovementSpeedBridge[%s]: applied %.3f (clamped=%.3f)",
                entityUuid,
                newValue,
                loggedMultiplier));
    }
}
