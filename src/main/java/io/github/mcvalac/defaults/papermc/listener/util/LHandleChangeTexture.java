package io.github.mcvalac.defaults.papermc.listener.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Listens for inventory interactions to apply textures to backpacks.
 * <p>
 * When a player clicks a Backpack item using a "Backpack Texture Applicator",
 * this listener updates the backpack's texture visually and persists the change to the database.
 * </p>
 */
public class LHandleChangeTexture implements Listener {

    /** The backend provider for saving texture updates. */
    private final MCBackpackProvider provider;

    /** NamespacedKey to identify the Applicator item. */
    private final NamespacedKey applicatorKey;

    /** NamespacedKey to identify valid Backpacks. */
    private final NamespacedKey uuidKey;

    /** NamespacedKey to update the texture stored in the backpack item. */
    private final NamespacedKey textureKey;

    /**
     * Constructs the texture change listener.
     * @param plugin   The main plugin instance.
     * @param provider The data provider.
     */
    public LHandleChangeTexture(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.applicatorKey = new NamespacedKey(plugin, "backpack_texture_applicator");
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
        this.textureKey = new NamespacedKey(plugin, "backpack_texture");
    }

    /**
     * Handles inventory clicks to apply the texture.
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Check if Cursor is the Applicator
        if (cursor == null || cursor.getType() != Material.PLAYER_HEAD) return;
        if (cursor.getItemMeta() == null || !cursor.getItemMeta().getPersistentDataContainer().has(applicatorKey, PersistentDataType.STRING)) {
            return;
        }

        // Check if Clicked Item is a Backpack (Has UUID)
        if (current == null || current.getType() != Material.PLAYER_HEAD) return;
        if (current.getItemMeta() == null || !current.getItemMeta().getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            return;
        }

        // It's a match: Apply Texture
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        String newTexture = cursor.getItemMeta().getPersistentDataContainer().get(applicatorKey, PersistentDataType.STRING);
        String uuid = current.getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);

        // 1. Update Visuals (ItemMeta)
        SkullMeta backpackMeta = (SkullMeta) current.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", newTexture));
        backpackMeta.setPlayerProfile(profile);

        backpackMeta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, newTexture);

        current.setItemMeta(backpackMeta);

        // 2. Consume Applicator
        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(cursor);
        } else {
            event.getView().setCursor(null);
        }

        // 3. Update Database (Async)
        provider.setTexture(uuid, newTexture).thenRun(() -> {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.texture.applied", "Backpack texture applied.").color(NamedTextColor.GREEN);
            player.sendMessage(msg);
        });

        // 4. Force Inventory Update
        player.updateInventory();
    }
}
