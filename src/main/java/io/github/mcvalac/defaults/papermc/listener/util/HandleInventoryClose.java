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
 */
public class HandleInventoryClose implements Listener {

    private final MCBackpackProvider provider;
    private final BackpackCooldownManager cooldownManager;

    public HandleInventoryClose(MCBackpackProvider provider, BackpackCooldownManager cooldownManager) {
        this.provider = provider;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof BackpackHolder holder) {
            String uuid = holder.getUuid();

            try {
                String base64Content = toBase64(inventory);

                // CHANGED: Pass player UUID to provider
                provider.save(uuid, base64Content, event.getPlayer().getUniqueId().toString()).thenRun(() -> {
                    cooldownManager.markClosed(event.getPlayer().getUniqueId());
                });

            } catch (IOException e) {
                e.printStackTrace();
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.save", "Could not save backpack contents").color(NamedTextColor.RED);
                event.getPlayer().sendMessage(msg);
            }
        }
    }

    private String toBase64(Inventory inventory) throws IOException {
        byte[] data = ItemStack.serializeItemsAsBytes(Arrays.asList(inventory.getContents()));
        return Base64.getEncoder().encodeToString(data);
    }
}
