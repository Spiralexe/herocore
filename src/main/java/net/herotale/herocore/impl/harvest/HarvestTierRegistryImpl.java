package net.herotale.herocore.impl.harvest;

import net.herotale.herocore.api.harvest.HarvestTierRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link HarvestTierRegistry}.
 */
public class HarvestTierRegistryImpl implements HarvestTierRegistry {

    private final Map<String, Double> tiers = new ConcurrentHashMap<>();

    @Override
    public void register(String tierId, double minMiningSpeed) {
        tiers.put(tierId, minMiningSpeed);
    }

    @Override
    public boolean meetsRequirement(String tierId, double miningSpeed) {
        Double required = tiers.get(tierId);
        if (required == null) return true; // unknown tier = no restriction
        return miningSpeed >= required;
    }

    @Override
    public double getRequirement(String tierId) {
        return tiers.getOrDefault(tierId, 0.0);
    }
}
