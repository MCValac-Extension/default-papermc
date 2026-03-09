package io.github.mcvalac.defaults.papermc.command.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Command handler for creating a new "Backpack Creation" item.
 * <p>
 * This command gives the player a special player head containing NBT data (size, texture)
 * but no UUID yet. The UUID is assigned when the player first uses the item.
 * </p>
 */
public class CHandleCreateBackpack implements IBackpackCommandHandle {

    /** The main plugin instance. */
    private final Plugin plugin;

    /** NamespacedKey used to store the backpack size in the item's persistent data. */
    private final NamespacedKey sizeKey;

    /** NamespacedKey used to store the Base64 texture string in the item's persistent data. */
    private final NamespacedKey textureKey;
    private final NamespacedKey modelDataKey;

    /**
     * Constructs a new creation handler.
     * @param plugin The main plugin instance.
     */
    public CHandleCreateBackpack(Plugin plugin) {
        this.plugin = plugin;
        this.sizeKey = new NamespacedKey(plugin, "backpack_size");
        this.textureKey = new NamespacedKey(plugin, "backpack_texture");
        this.modelDataKey = new NamespacedKey(plugin, "backpack_model_data");
    }

    /**
     * Executes the create command logic.
     * Validates input size, parses texture (if present), creates the skull item, and gives it to the player.
     * @param sender The source of the command (must be a Player).
     * @param args   Command arguments: [size] [texture].
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return;
        }

        Player player = (Player) sender;
        int size = 27; // Default size

        // Fetch default texture from config
        String texture = plugin.getConfig().getString("backpack.default");
        if (texture == null) {
            // Fallback default if config is missing
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTE2NWVlMTNhNjA2ZTFiNDQ2OTVhZjQ2YzM5YjUyY2U2NjY1N2E0YzRhNjIzZDBiMjgyYTdiOGNlMDUwOTQwNCJ9fX0=";
        }

        // Parse Size
        if (args.length > 0) {
            try {
                size = Integer.parseInt(args[0]);
                if (size % 9 != 0 || size < 9 || size > 54) {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.size.invalid", "Size must be a multiple of 9 between 9 and 54.").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                    return;
                }
            } catch (NumberFormatException e) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.number.invalid", "Invalid number format.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }
        }

        // Parse Texture (Optional override)
        if (args.length > 1) {
            texture = args[1];
        }

        String modelData = null;
        if (args.length > 2) {
            modelData = args[2];
        }

        // Create the Creation Item
        ItemStack backpackItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) backpackItem.getItemMeta();

        if (meta != null) {
            // 1. Apply Texture (Visual)
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                String decoded = new String(Base64.getDecoder().decode(texture));
                if (decoded.contains("http")) {
                    profile.setProperty(new ProfileProperty("textures", texture));
                    meta.setPlayerProfile(profile);
                }
            } catch (IllegalArgumentException ignored) { }

            // 2. Set Name: "Backpack Creation {size}" using Adventure API
            meta.displayName(Component.translatable("mcvalac.mcbackpack.extension.default.item.creation.name", "Backpack Creation").color(NamedTextColor.AQUA).append(Component.text(" " + size)));

            // 3. Store Data in NBT for Listener
            meta.getPersistentDataContainer().set(sizeKey, PersistentDataType.INTEGER, size);
            meta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, texture);

            if (modelData != null && !modelData.isBlank()) {
                meta.getPersistentDataContainer().set(modelDataKey, PersistentDataType.STRING, modelData);

                CustomModelDataComponent customModelData = meta.getCustomModelDataComponent();
                customModelData.setStrings(List.of(modelData));
                meta.setCustomModelDataComponent(customModelData);
            }

            backpackItem.setItemMeta(meta);
        }

        player.getInventory().addItem(backpackItem);

        Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.creation.received", "Backpack creation item received.").color(NamedTextColor.GREEN);
        player.sendMessage(msg);
    }

    /**
     * Retrieves the help string for the create command.
     * @return The usage syntax "[size] [texture] - Get a new backpack item".
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.create", "[size] [texture] - Get a new backpack item");
    }

    /**
     * Retrieves the permission node required to create a backpack.
     * @return The permission string "mcbackpack.create".
     */
    @Override
    public String getPermission() { return "mcbackpack.create"; }
}
