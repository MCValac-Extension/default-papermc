package io.github.mcvalac.defaults.papermc.listener.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Listens for inventory interactions to apply model data to backpacks.
 */
public class LHandleChangeModelData implements Listener {

    private final NamespacedKey applicatorKey;
    private final NamespacedKey uuidKey;
    private final NamespacedKey modelDataKey;

    public LHandleChangeModelData(Plugin plugin) {
        this.applicatorKey = new NamespacedKey(plugin, "backpack_model_data_applicator");
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
        this.modelDataKey = new NamespacedKey(plugin, "backpack_model_data");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType() != Material.PAPER) return;
        if (cursor.getItemMeta() == null || !cursor.getItemMeta().getPersistentDataContainer().has(applicatorKey, PersistentDataType.STRING)) {
            return;
        }

        if (current == null || current.getType() != Material.PLAYER_HEAD) return;
        if (current.getItemMeta() == null || !current.getItemMeta().getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        String modelData = cursor.getItemMeta().getPersistentDataContainer().get(applicatorKey, PersistentDataType.STRING);
        if (modelData == null || modelData.isBlank()) {
            player.sendMessage(Component.translatable("mcengine.mcbackpack.msg.error.model_data.invalid", "Invalid model data.").color(NamedTextColor.RED));
            return;
        }

        ItemMeta backpackMeta = current.getItemMeta();
        backpackMeta.getPersistentDataContainer().set(modelDataKey, PersistentDataType.STRING, modelData);

        CustomModelDataComponent customModelData = backpackMeta.getCustomModelDataComponent();
        customModelData.setStrings(List.of(modelData));
        backpackMeta.setCustomModelDataComponent(customModelData);
        current.setItemMeta(backpackMeta);

        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(cursor);
        } else {
            event.getView().setCursor(null);
        }

        player.sendMessage(Component.translatable("mcengine.mcbackpack.msg.model_data.applied", "Model data applied.").color(NamedTextColor.GREEN));
        player.updateInventory();
    }
}
