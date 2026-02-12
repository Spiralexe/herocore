package net.herotale.herocore.impl.bridge;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.herotale.herocore.api.attribute.RPGAttribute;
import net.herotale.herocore.api.component.StatsComponent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridge that reads MINING_SPEED from a {@link StatsComponent} and
 * translates it into Hytale ECS component updates.
 * <p>
 * Call {@link #apply(StatsComponent)} whenever the entity's stats change.
 */
public class MiningSpeedBridge {

    private static final Logger LOG = Logger.getLogger(MiningSpeedBridge.class.getName());
    private static final String STAT_NAME = "MiningSpeed";
    private final UUID entityUuid;

    public MiningSpeedBridge(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public RPGAttribute attribute() {
        return RPGAttribute.MINING_SPEED;
    }

    /**
     * Push the current MINING_SPEED value from stats into Hytale's EntityStatMap.
     */
    public void apply(StatsComponent stats) {
        double newValue = stats.getValue(RPGAttribute.MINING_SPEED);

        PlayerRef playerRef = Universe.get().getPlayer(entityUuid);
        if (playerRef == null) {
            LOG.fine(() -> String.format("MiningSpeedBridge[%s]: missing PlayerRef", entityUuid));
            return;
        }

        EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) {
            LOG.fine(() -> String.format("MiningSpeedBridge[%s]: EntityStatMap missing", entityUuid));
            return;
        }

        int statIndex = EntityStatType.getAssetMap().getIndex(STAT_NAME);
        if (statIndex == Integer.MIN_VALUE || statMap.get(statIndex) == null) {
            LOG.fine(() -> String.format("MiningSpeedBridge[%s]: stat '%s' not found", entityUuid, STAT_NAME));
            return;
        }

        statMap.setStatValue(statIndex, (float) newValue);
        LOG.fine(() -> String.format("MiningSpeedBridge[%s]: applied %.3f", entityUuid, newValue));
    }
}
