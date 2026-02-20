package net.herotale.herocore.api.factions;

/**
 * Immutable rules governing faction selection and switching.
 * <p>
 * Created by the faction-providing plugin (Heroes) and stored in Herocore
 * so that any downstream plugin can query faction rules without a Heroes dependency.
 */
public class FactionRules {
    private final boolean lockOnSelection;
    private final boolean allowResetViaHeroReset;
    private final int resetCooldownHours;
    private final String defaultFaction;
    private final boolean detachClassOnChange;
    private final boolean detachRaceOnChange;
    private final boolean detachProfessionOnChange;

    /**
     * Construct faction rules with all values specified.
     */
    public FactionRules(boolean lockOnSelection, boolean allowResetViaHeroReset,
                        int resetCooldownHours, String defaultFaction,
                        boolean detachClassOnChange, boolean detachRaceOnChange,
                        boolean detachProfessionOnChange) {
        this.lockOnSelection = lockOnSelection;
        this.allowResetViaHeroReset = allowResetViaHeroReset;
        this.resetCooldownHours = resetCooldownHours;
        this.defaultFaction = defaultFaction;
        this.detachClassOnChange = detachClassOnChange;
        this.detachRaceOnChange = detachRaceOnChange;
        this.detachProfessionOnChange = detachProfessionOnChange;
    }

    /** Default rules — nothing locked, no cooldown, no detach. */
    public FactionRules() {
        this(false, true, 0, null, false, false, false);
    }

    public boolean isLockOnSelection() {
        return lockOnSelection;
    }

    public boolean isAllowResetViaHeroReset() {
        return allowResetViaHeroReset;
    }

    public int getResetCooldownHours() {
        return resetCooldownHours;
    }

    public String getDefaultFaction() {
        return defaultFaction;
    }

    public boolean isDetachClassOnChange() {
        return detachClassOnChange;
    }

    public boolean isDetachRaceOnChange() {
        return detachRaceOnChange;
    }

    public boolean isDetachProfessionOnChange() {
        return detachProfessionOnChange;
    }
}
