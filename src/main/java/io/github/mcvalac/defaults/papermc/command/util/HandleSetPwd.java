package io.github.mcvalac.defaults.papermc.command.util;

import io.github.mcvalac.defaults.papermc.command.IBackpackCommandHandle;
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
 * Command handler for setting a password on an unlocked backpack.
 */
public class HandleSetPwd implements IBackpackCommandHandle {

    /** Service provider for database operations. */
    private final MCBackpackProvider provider;

    /** NamespacedKey used to retrieve the UUID from the backpack item. */
    private final NamespacedKey uuidKey;

    /**
     * Constructs the set password handler.
     *
     * @param plugin   The main plugin instance.
     * @param provider The backend provider instance.
     */
    public HandleSetPwd(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    /**
     * Executes the set password logic.
     * Checks if the backpack is already locked before setting a new password.
     *
     * @param sender The command sender (must be a Player holding a backpack).
     * @param args   Command arguments: &lt;password&gt;.
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
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.usage.setpwd", "/bp setpwd <password>").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.not_holding", "You must hold a backpack in your main hand.").color(NamedTextColor.RED);
            player.sendMessage(msg);
            return;
        }

        String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        String rawPassword = args[0];

        // First, load the backpack data to check if a password exists
        provider.open(uuid).thenAccept(data -> {
            if (data == null) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.not_found", "Backpack not found.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }

            // CHANGED: Use pwdHash to verify lock status instead of isLocked()
            if (data.getPwdHash() != null && !data.getPwdHash().isEmpty()) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.exists", "This backpack already has a password.").color(NamedTextColor.RED);
                player.sendMessage(msg);

                Component hint = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.exists.hint", "Use /bp changepwd or /bp deletepwd.").color(NamedTextColor.GRAY);
                player.sendMessage(hint);
                return;
            }

            // Proceed to set password if none exists
            provider.setPwd(uuid, rawPassword).thenRun(() -> {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.set", "Backpack password set.").color(NamedTextColor.GREEN);
                player.sendMessage(msg);
            });
        });
    }

    /**
     * Retrieves the help string for the set password command.
     *
     * @return The usage syntax.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.setpwd", "<password> - Set password for held backpack");
    }

    /**
     * Retrieves the permission node.
     *
     * @return null, indicating no permission is required.
     */
    @Override
    public String getPermission() { return null; }
}
