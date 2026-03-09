package io.github.mcvalac.defaults.papermc.manager;

import io.github.mcvalac.mcbackpack.api.model.BackpackData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the state of players who are in the process of entering a backpack password.
 * <p>
 * This class acts as a temporary store, linking a player to the specific backpack data
 * they are attempting to unlock.
 * </p>
 */
public class PasswordInputManager {

    /**
     * Map storing pending authentication requests.
     * Key: Player UUID
     * Value: The BackpackData waiting to be unlocked.
     */
    private final Map<UUID, BackpackData> pendingAuth = new HashMap<>();

    /**
     * Adds a player to the pending list.
     *
     * @param playerUuid The UUID of the player.
     * @param data       The backpack data they are trying to access.
     */
    public void addPending(UUID playerUuid, BackpackData data) {
        pendingAuth.put(playerUuid, data);
    }

    /**
     * Retrieves the backpack data associated with a pending player.
     *
     * @param playerUuid The UUID of the player.
     * @return The pending {@link BackpackData}, or null if not found.
     */
    public BackpackData getPending(UUID playerUuid) {
        return pendingAuth.get(playerUuid);
    }

    /**
     * Removes a player from the pending list.
     *
     * @param playerUuid The UUID of the player to remove.
     */
    public void removePending(UUID playerUuid) {
        pendingAuth.remove(playerUuid);
    }

    /**
     * Checks if a player is currently waiting to input a password.
     *
     * @param playerUuid The UUID of the player.
     * @return {@code true} if the player is pending authentication, {@code false} otherwise.
     */
    public boolean isPending(UUID playerUuid) {
        return pendingAuth.containsKey(playerUuid);
    }
}
