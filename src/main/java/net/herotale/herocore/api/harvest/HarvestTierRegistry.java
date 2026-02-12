package net.herotale.herocore.api.harvest;

/**
 * Registry for harvest tiers — maps tool/block combinations to attribute
 * requirements (e.g., minimum MINING_SPEED to mine a certain block tier).
 */
public interface HarvestTierRegistry {

    /**
     * Register a harvest tier.
     *
     * @param tierId   unique tier ID (e.g., {@code "herocore:iron_tier"})
     * @param minMiningSpeed minimum MINING_SPEED attribute required
     */
    void register(String tierId, double minMiningSpeed);

    /**
     * Check if a player's mining speed meets the required tier.
     *
     * @param tierId       the tier to check against
     * @param miningSpeed  the player's current MINING_SPEED value
     * @return true if the player meets or exceeds the requirement
     */
    boolean meetsRequirement(String tierId, double miningSpeed);

    /**
     * Get the minimum mining speed for a tier.
     *
     * @param tierId the tier ID
     * @return the minimum mining speed, or 0 if tier not found
     */
    double getRequirement(String tierId);
}
