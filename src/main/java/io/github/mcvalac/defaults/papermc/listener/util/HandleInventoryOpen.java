package io.github.mcvalac.defaults.papermc.listener.util;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.defaults.papermc.manager.BackpackCooldownManager;
import io.github.mcvalac.defaults.papermc.manager.PasswordInputManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.Base64;

/**
 * Listens for player interactions to open backpacks.
 */
public class HandleInventoryOpen implements Listener {

    private final MCBackpackProvider provider;
    private final PasswordInputManager passwordManager;
    private final BackpackCooldownManager cooldownManager;
    private final NamespacedKey uuidKey;

    public HandleInventoryOpen(Plugin plugin, MCBackpackProvider provider, PasswordInputManager passwordManager, BackpackCooldownManager cooldownManager) {
        this.provider = provider;
        this.passwordManager = passwordManager;
        this.cooldownManager = cooldownManager;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Player player = event.getPlayer();
        String backpackUuid;

        if (meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            backpackUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        } else {
            return;
        }

        event.setCancelled(true);

        if (cooldownManager.isOnCooldown(player.getUniqueId())) {
            long remainingMs = cooldownManager.getRemainingMillis(player.getUniqueId());
            long remainingSeconds = Math.max(1, (long) Math.ceil(remainingMs / 1000.0));
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.cooldown", "Please wait before opening your backpack again")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(" (" + remainingSeconds + "s)"));
            player.sendMessage(msg);
            return;
        }

        if (passwordManager.isPending(player.getUniqueId())) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.enter", "Please enter your backpack password in chat").color(NamedTextColor.YELLOW);
            player.sendMessage(msg);
            return;
        }

        Component loading = Component.translatable("mcvalac.mcbackpack.extension.default.msg.item.open", "Opening backpack...").color(NamedTextColor.GRAY);
        player.sendMessage(loading);

        // CHANGED: Pass player UUID to provider
        provider.open(backpackUuid, player.getUniqueId().toString()).thenAccept(data -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("MCBackpack"), () -> {

                if (data == null) {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.load", "Could not load backpack data").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                    return;
                }

                boolean hasPassword = (data.getPwdHash() != null && !data.getPwdHash().isEmpty());

                if (hasPassword && !player.isOp()) {
                    Component locked = Component.translatable("mcvalac.mcbackpack.extension.default.msg.locked", "This backpack is locked").color(NamedTextColor.RED);
                    player.sendMessage(locked);

                    Component instr = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.instruction", "Type your password in chat, or type 'cancel' to abort").color(NamedTextColor.YELLOW);
                    player.sendMessage(instr);

                    passwordManager.addPending(player.getUniqueId(), data);
                    return;
                }

                try {
                    Component title = Component.translatable("mcvalac.mcbackpack.extension.default.gui.title", "Backpack");

                    Inventory backpackInv;
                    if (data.getContent() == null || data.getContent().isEmpty()) {
                        backpackInv = Bukkit.createInventory(new BackpackHolder(backpackUuid), data.getSize(), title);
                    } else {
                        backpackInv = fromBase64(data.getContent(), data.getSize(), backpackUuid, title);
                    }

                    player.openInventory(backpackInv);

                } catch (IOException e) {
                    e.printStackTrace();
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.deserialize", "Failed to read backpack contents").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                }
            });
        });
    }

    private Inventory fromBase64(String base64, int size, String uuid, Component title) throws IOException {
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(uuid), size, title);

        if (base64 == null || base64.isEmpty()) return inventory;

        byte[] data = Base64.getDecoder().decode(base64);
        ItemStack[] items = ItemStack.deserializeItemsFromBytes(data);

        inventory.setContents(items);
        return inventory;
    }

    public static class BackpackHolder implements org.bukkit.inventory.InventoryHolder {
        private final String uuid;
        public BackpackHolder(String uuid) { this.uuid = uuid; }
        public String getUuid() { return uuid; }
        @Override public Inventory getInventory() { return null; }
    }
}
