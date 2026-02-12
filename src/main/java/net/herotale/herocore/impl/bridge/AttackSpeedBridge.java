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
 * Bridge that reads ATTACK_SPEED from a {@link StatsComponent} and
 * translates it into Hytale ECS component updates.
 * <p>
 * Call {@link #apply(StatsComponent)} whenever the entity's stats change.
 */
public class AttackSpeedBridge {

    private static final Logger LOG = Logger.getLogger(AttackSpeedBridge.class.getName());
    private static final String STAT_NAME = "AttackSpeed";
    private final UUID entityUuid;

    public AttackSpeedBridge(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public RPGAttribute attribute() {
        return RPGAttribute.ATTACK_SPEED;
    }

    /**
     * Push the current ATTACK_SPEED value from stats into Hytale's EntityStatMap.
     */
    public void apply(StatsComponent stats) {
        double newValue = stats.getValue(RPGAttribute.ATTACK_SPEED);

        PlayerRef playerRef = Universe.get().getPlayer(entityUuid);
        if (playerRef == null) {
            LOG.fine(() -> String.format("AttackSpeedBridge[%s]: missing PlayerRef", entityUuid));
            return;
        }

        EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) {
            LOG.fine(() -> String.format("AttackSpeedBridge[%s]: EntityStatMap missing", entityUuid));
            return;
        }

        int statIndex = EntityStatType.getAssetMap().getIndex(STAT_NAME);
        if (statIndex == Integer.MIN_VALUE || statMap.get(statIndex) == null) {
            LOG.fine(() -> String.format("AttackSpeedBridge[%s]: stat '%s' not found", entityUuid, STAT_NAME));
            return;
        }

        statMap.setStatValue(statIndex, (float) newValue);
        LOG.fine(() -> String.format("AttackSpeedBridge[%s]: applied %.3f", entityUuid, newValue));
    }
}
