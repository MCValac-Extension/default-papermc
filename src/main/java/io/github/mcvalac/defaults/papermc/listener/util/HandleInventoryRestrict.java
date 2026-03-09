package io.github.mcvalac.defaults.papermc.listener.util;

import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryOpen.BackpackHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Enforces restrictions on backpack usage within inventories.
 * <p>
 * Specifically, this listener prevents players from placing a backpack item inside
 * an active backpack GUI (Recursive Storage Prevention).
 * </p>
 */
public class HandleInventoryRestrict implements Listener {

    /** NamespacedKey used to identify valid backpacks. */
    private final NamespacedKey sizeKey;

    /**
     * Constructs the restriction listener.
     *
     * @param plugin The main plugin instance.
     */
    public HandleInventoryRestrict(Plugin plugin) {
        this.sizeKey = new NamespacedKey(plugin, "backpack_size");
    }

    /**
     * Prevents dragging a backpack over the top inventory slots.
     *
     * @param event The inventory drag event.
     */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BackpackHolder)) {
            return;
        }

        if (isBackpack(event.getOldCursor())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.recursive", "You cannot put a backpack inside another backpack.").color(NamedTextColor.RED);
                    event.getWhoClicked().sendMessage(msg);
                    return;
                }
            }
        }
    }

    /**
     * Prevents clicking, shifting, or swapping backpacks into the inventory.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BackpackHolder)) {
            return;
        }

        // Prevent dropping the backpack while the GUI is open (Key Drop)
        if (event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT) {
            if (isBackpack(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent dropping the backpack while the GUI is open (Cursor/Mouse Drop)
        if (event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
            if (isBackpack(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }

        // 1. Direct Click or Swap on Top Inventory
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (isBackpack(event.getCursor()) ||
               (event.getClick() == ClickType.NUMBER_KEY && isBackpack(event.getWhoClicked().getInventory().getItem(event.getHotbarButton())))) {

                 event.setCancelled(true);
                 Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.recursive", "You cannot put a backpack inside another backpack.").color(NamedTextColor.RED);
                 event.getWhoClicked().sendMessage(msg);
                 return;
            }
        }

        // 2. Shift-Clicking from Bottom Inventory (Player) -> Top
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                if (isBackpack(event.getCurrentItem())) {
                    event.setCancelled(true);
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.recursive", "You cannot put a backpack inside another backpack.").color(NamedTextColor.RED);
                    event.getWhoClicked().sendMessage(msg);
                    return;
                }
            }
        }
    }

    /**
     * Helper to check if item is a Backpack (by checking for Size Key).
     *
     * @param item The item to check.
     * @return {@code true} if the item is a backpack, {@code false} otherwise.
     */
    private boolean isBackpack(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(sizeKey, PersistentDataType.INTEGER);
    }
}
