package net.herotale.herocore.api.factions;

import java.util.Set;

public interface Faction {
    String getId();
    String getDisplayName();
    Set<String> getPermissionNodes();
    boolean isFriendlyFireAllowed();
}
