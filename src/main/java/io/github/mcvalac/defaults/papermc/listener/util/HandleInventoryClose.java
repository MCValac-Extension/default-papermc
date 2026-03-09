package io.github.mcvalac.defaults.papermc.listener.util;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryOpen.BackpackHolder;
import io.github.mcvalac.defaults.papermc.manager.BackpackCooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Listens for inventory close events to save backpack data.
 * <p>
 * Detects when a custom Backpack inventory is closed, serializes the contents,
 * and saves them asynchronously to the database provider.
 * </p>
 */
public class HandleInventoryClose implements Listener {

    /** Service provider for saving data. */
    private final MCBackpackProvider provider;

    /** Manager for tracking close cooldowns. */
    private final BackpackCooldownManager cooldownManager;

    /**
     * Constructs the inventory close listener.
     *
     * @param provider        The backend provider instance.
     * @param cooldownManager The cooldown tracker.
     */
    public HandleInventoryClose(MCBackpackProvider provider, BackpackCooldownManager cooldownManager) {
        this.provider = provider;
        this.cooldownManager = cooldownManager;
    }

    /**
     * Handles the inventory close event.
     * Checks if the inventory is a {@link BackpackHolder}, serializes it, and initiates a save.
     *
     * @param event The inventory close event.
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof BackpackHolder holder) {
            String uuid = holder.getUuid();

            try {
                String base64Content = toBase64(inventory);

                // Save Async
                provider.save(uuid, base64Content).thenRun(() -> {
                    cooldownManager.markClosed(event.getPlayer().getUniqueId());
                    provider.markClosed(uuid);
                });

            } catch (IOException e) {
                e.printStackTrace();
                Component msg = Component.translatable("mcengine.mcbackpack.msg.error.save", "Could not save backpack contents").color(NamedTextColor.RED);
                event.getPlayer().sendMessage(msg);
            }
        }
    }

    /**
     * Serializes an Inventory object to a Base64 encoded string.
     *
     * @param inventory The inventory to serialize.
     * @return A Base64 string representation of the inventory contents.
     * @throws IOException If serialization fails.
     */
    private String toBase64(Inventory inventory) throws IOException {
        byte[] data = ItemStack.serializeItemsAsBytes(Arrays.asList(inventory.getContents()));
        return Base64.getEncoder().encodeToString(data);
    }
}
