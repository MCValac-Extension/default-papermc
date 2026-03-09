package io.github.mcvalac.defaults.papermc.manager;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks backpack close timestamps to prevent rapid reopen spam.
 */
public class BackpackCooldownManager {

    private final Map<UUID, Long> lastClosed = new ConcurrentHashMap<>();
    private final long cooldownMillis;

    public BackpackCooldownManager(Duration cooldownDuration) {
        this.cooldownMillis = cooldownDuration.toMillis();
    }

    /**
     * Records when a player closed a backpack.
     */
    public void markClosed(UUID playerUuid) {
        lastClosed.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Checks if the player is still within the cooldown window.
     */
    public boolean isOnCooldown(UUID playerUuid) {
        Long last = lastClosed.get(playerUuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < cooldownMillis;
    }

    /**
     * Remaining milliseconds before the player can reopen.
     */
    public long getRemainingMillis(UUID playerUuid) {
        Long last = lastClosed.get(playerUuid);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = cooldownMillis - elapsed;
        return Math.max(remaining, 0);
    }
}
