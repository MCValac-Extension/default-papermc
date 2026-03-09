package io.github.mcvalac.defaults.papermc.command.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.mcvalac.defaults.papermc.command.IBackpackCommandHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Base64;
import java.util.UUID;

/**
 * Command handler for giving the player a "Texture Applicator" item.
 * <p>
 * This command generates a special player head with a specific texture stored in its
 * persistent data container. When clicked on a valid backpack in the inventory,
 * the listener will apply this texture to the backpack.
 * </p>
 */
public class CHandleChangeTexture implements IBackpackCommandHandle {

    /** NamespacedKey used to store the target texture in the applicator item. */
    private final NamespacedKey applicatorKey;

    /**
     * Constructs a new texture change command handler.
     * @param plugin The main plugin instance.
     */
    public CHandleChangeTexture(Plugin plugin) {
        this.applicatorKey = new NamespacedKey(plugin, "backpack_texture_applicator");
    }

    /**
     * Executes the logic to give the applicator item.
     * @param sender The entity sending the command (must be a Player).
     * @param args   The arguments: [texture_base64].
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.usage.texture", "/bp texture <base64_texture>").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        String texture = args[0];

        // Create the Applicator Item
        ItemStack applicator = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) applicator.getItemMeta();

        if (meta != null) {
            // 1. Apply Texture (Visual)
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                // Simple validation to prevent crashes with invalid base64
                new String(Base64.getDecoder().decode(texture));

                profile.setProperty(new ProfileProperty("textures", texture));
                meta.setPlayerProfile(profile);
            } catch (IllegalArgumentException e) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.invalid_texture", "Invalid texture value.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }

            // 2. Set Name
            meta.displayName(Component.translatable("mcvalac.mcbackpack.extension.default.item.applicator.name", "Backpack Texture Applicator").color(NamedTextColor.LIGHT_PURPLE));

            // 3. Store Data for Listener
            meta.getPersistentDataContainer().set(applicatorKey, PersistentDataType.STRING, texture);

            applicator.setItemMeta(meta);
        }

        // Give item
        var overflow = player.getInventory().addItem(applicator);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), applicator);
        }

        Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.texture.received", "Texture applicator received.").color(NamedTextColor.GREEN);
        player.sendMessage(msg);
    }

    /**
     * Retrieves the help component.
     * @return The usage syntax.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.texture", "<base64_texture> - Get a backpack texture applicator");
    }

    /**
     * Retrieves the permission node.
     * @return The permission string.
     */
    @Override
    public String getPermission() {
        return "mcbackpack.texture";
    }
}
