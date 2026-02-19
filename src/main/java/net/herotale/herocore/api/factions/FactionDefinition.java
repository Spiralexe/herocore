package net.herotale.herocore.api.factions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FactionDefinition implements Faction {
    private final String id;
    private final String displayName;
    private final Set<String> permissionNodes;
    private final boolean friendlyFireAllowed;

    public FactionDefinition(String id, String displayName, Set<String> permissionNodes, boolean friendlyFireAllowed) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.permissionNodes = Collections.unmodifiableSet(permissionNodes != null ? new HashSet<>(permissionNodes) : new HashSet<>());
        this.friendlyFireAllowed = friendlyFireAllowed;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Set<String> getPermissionNodes() {
        return permissionNodes;
    }

    @Override
    public boolean isFriendlyFireAllowed() {
        return friendlyFireAllowed;
    }
}
