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
 * Command handler for changing the password of an existing backpack.
 */
public class HandleChangePwd implements IBackpackCommandHandle {

    /** Service provider for database operations. */
    private final MCBackpackProvider provider;

    /** NamespacedKey used to retrieve the UUID from the backpack item. */
    private final NamespacedKey uuidKey;

    /**
     * Constructs the change password handler.
     *
     * @param plugin   The main plugin instance.
     * @param provider The backend provider instance.
     */
    public HandleChangePwd(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    /**
     * Executes logic to change the password.
     * Verifies the old password matches before setting the new one.
     *
     * @param sender The command sender (must be a Player holding a backpack).
     * @param args   Command arguments: &lt;oldPassword&gt; &lt;newPassword&gt;.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.usage.changepwd", "/bp changepwd <old_password> <new_password>").color(NamedTextColor.RED);
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
        String oldPwdRaw = args[0];
        String newPwdRaw = args[1];

        provider.open(uuid).thenAccept(data -> {
            if (data == null) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.not_found", "Backpack not found.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }

            if (!data.isLocked()) {
                Component msg = Component.text("Backpack doesn't have a password yet.").color(NamedTextColor.YELLOW);
                player.sendMessage(msg);
                return;
            }

            provider.checkPwd(uuid, oldPwdRaw).thenAccept(isValid -> {
                if (isValid) {
                    provider.changePwd(uuid, newPwdRaw).thenRun(() -> {
                        Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.changed", "Backpack password changed.").color(NamedTextColor.GREEN);
                        player.sendMessage(msg);
                    });
                } else {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.old.incorrect", "Old password is incorrect.").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                }
            });
        });
    }

    /**
     * Retrieves the help string for the change password command.
     *
     * @return The usage syntax.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.changepwd", "<old> <new> - Change password for held backpack");
    }

    /**
     * Retrieves the permission node.
     *
     * @return null, indicating no permission is required.
     */
    @Override
    public String getPermission() { return null; }
}
