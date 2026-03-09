package io.github.mcvalac.defaults.papermc.listener.util;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

/**
 * Listens for interactions with "Creation Backpack" items.
 * <p>
 * These special items contain size and texture data but no UUID. When used, this listener
 * generates a UUID, creates the database record, and converts the item into a fully
 * functional backpack (handling stack splitting if necessary).
 * </p>
 */
public class LHandleCreateBackpack implements Listener {

    /** The main plugin instance. */
    private final Plugin plugin;

    /** The backend provider for creating records. */
    private final MCBackpackProvider provider;

    /** Key for the unique backpack ID. */
    private final NamespacedKey uuidKey;

    /** Key for the backpack size. */
    private final NamespacedKey sizeKey;

    /** Key for the backpack texture. */
    private final NamespacedKey textureKey;
    private final NamespacedKey modelDataKey;

    /**
     * Constructs the creation listener.
     *
     * @param plugin   The main plugin instance.
     * @param provider The backend provider instance.
     */
    public LHandleCreateBackpack(Plugin plugin, MCBackpackProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
        this.sizeKey = new NamespacedKey(plugin, "backpack_size");
        this.textureKey = new NamespacedKey(plugin, "backpack_texture");
        this.modelDataKey = new NamespacedKey(plugin, "backpack_model_data");
    }

    /**
     * Handles right-click interactions to initialize creation items.
     * <p>
     * Runs with LOW priority to ensure it processes before the standard open listener.
     * </p>
     *
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.LOW)
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

        // Check if this is a Creation Item (Must have Size)
        if (!meta.getPersistentDataContainer().has(sizeKey, PersistentDataType.INTEGER)) {
            return;
        }

        // Check if it already has a UUID (If yes, it's already created, ignore it)
        if (meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            return;
        }

        // --- FIX: Prevent Block Placement ---
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Check if inventory has space before creating (only needed if stacking)
        if (item.getAmount() > 1 && player.getInventory().firstEmpty() == -1) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.inventory.full", "Your inventory is full.").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        // --- Logic: Initialize New Backpack ---

        String newUuid = UUID.randomUUID().toString();
        int size = meta.getPersistentDataContainer().get(sizeKey, PersistentDataType.INTEGER);
        String texture = meta.getPersistentDataContainer().getOrDefault(textureKey, PersistentDataType.STRING, "");
        String modelData = meta.getPersistentDataContainer().get(modelDataKey, PersistentDataType.STRING);

        // 1. Create Database Record (Async)
        provider.create(newUuid, texture, size).thenAccept(v -> {
            // 2. Transform Item (Must be Sync)
            Bukkit.getScheduler().runTask(plugin, () -> {

                // Re-fetch item in hand to ensure safety after async delay
                ItemStack handItem = player.getInventory().getItemInMainHand();

                // Verify the player is still holding a valid creation item
                if (handItem.getType() != Material.PLAYER_HEAD || handItem.getItemMeta() == null ||
                    !handItem.getItemMeta().getPersistentDataContainer().has(sizeKey, PersistentDataType.INTEGER)) {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.item.moved", "Backpack item moved before initialization completed.").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                    return;
                }

                // --- Stack Splitting Logic ---

                // Create the final initialized backpack (Clone 1)
                ItemStack newBackpack = handItem.clone();
                newBackpack.setAmount(1);
                ItemMeta newMeta = newBackpack.getItemMeta();

                // Stamp UUID
                newMeta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, newUuid);

                if (modelData != null && !modelData.isBlank()) {
                    newMeta.getPersistentDataContainer().set(modelDataKey, PersistentDataType.STRING, modelData);

                    CustomModelDataComponent customModelData = newMeta.getCustomModelDataComponent();
                    customModelData.setStrings(List.of(modelData));
                    newMeta.setCustomModelDataComponent(customModelData);
                }

                // Rename to "Backpack {size}" using Adventure API
                newMeta.displayName(Component.translatable("mcvalac.mcbackpack.extension.default.item.name", "Backpack").color(NamedTextColor.GOLD).append(Component.text(" " + size)));
                newBackpack.setItemMeta(newMeta);

                // Handle the original stack
                if (handItem.getAmount() > 1) {
                    handItem.setAmount(handItem.getAmount() - 1);

                    var overflow = player.getInventory().addItem(newBackpack);
                    if (!overflow.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), newBackpack);
                    }
                } else {
                    player.getInventory().setItemInMainHand(newBackpack);
                }

                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.initialized", "Backpack initialized successfully.").color(NamedTextColor.GREEN);
                player.sendMessage(msg);
            });
        });
    }
}
