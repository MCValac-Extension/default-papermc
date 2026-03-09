package io.github.mcvalac.defaults.papermc.command.util;

import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for removing password protection from a backpack.
 */
public class HandleDeletePwd implements IBackpackCommandHandle {

    /** Service provider for database operations. */
    private final MCBackpackProvider provider;

    /** NamespacedKey used to retrieve the UUID from the backpack item. */
    private final NamespacedKey uuidKey;

    /**
     * Constructs the delete password handler.
     *
     * @param plugin   The main plugin instance.
     * @param provider The backend provider instance.
     */
    public HandleDeletePwd(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    /**
     * Executes logic to remove the password.
     * Requires the current password to authorize deletion.
     *
     * @param sender The command sender (must be a Player holding a backpack).
     * @param args   Command arguments: &lt;password&gt;.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Component msg = Component.translatable("mcengine.mcbackpack.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return;
        }

        Player player = (Player) sender;

        boolean isOp = player.isOp();

        if (!isOp && args.length < 1) {
            Component msg = Component.translatable("mcengine.mcbackpack.msg.usage.deletepwd", "/bp deletepwd <password>").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            Component msg = Component.translatable("mcengine.mcbackpack.msg.error.not_holding", "You must hold a backpack in your main hand.").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        final String inputRaw = args.length > 0 ? args[0] : null;
        final String providedPwd = isOp ? null : inputRaw; // ops bypass verification downstream

        provider.open(uuid).thenAccept(data -> {
            if (data == null) {
                Component msg = Component.translatable("mcengine.mcbackpack.msg.error.not_found", "Backpack not found.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }

            if (!data.isLocked()) {
                Component msg = Component.text("Backpack doesn't have a password yet.").color(NamedTextColor.YELLOW);
                player.sendMessage(msg);
                return;
            }

            provider.deletePwd(uuid, providedPwd).thenAccept(success -> {
                if (success) {
                    Component msg = Component.translatable("mcengine.mcbackpack.msg.password.removed", "Backpack password removed.").color(NamedTextColor.GREEN);
                    player.sendMessage(msg);
                } else {
                    Component msg = Component.translatable("mcengine.mcbackpack.msg.password.incorrect", "Incorrect password").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                }
            });
        });
    }

    /**
     * Retrieves the help string for the delete password command.
     *
     * @return The usage syntax.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mcbackpack.msg.help.deletepwd", "<password> - Unlock held backpack");
    }

    /**
     * Retrieves the permission node.
     *
     * @return null, indicating no permission is required.
     */
    @Override
    public String getPermission() { return null; }
}
