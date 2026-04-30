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

public class HandleDeletePwd implements IBackpackCommandHandle {

    private final MCBackpackProvider provider;
    private final NamespacedKey uuidKey;

    public HandleDeletePwd(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return;
        }

        Player player = (Player) sender;
        boolean isOp = player.isOp();

        if (!isOp && args.length < 1) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.usage.deletepwd", "/bp deletepwd <password>").color(NamedTextColor.RED);
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
        final String inputRaw = args.length > 0 ? args[0] : null;
        final String providedPwd = isOp ? null : inputRaw; 

        // CHANGED: Pass player UUID to provider
        provider.open(uuid, player.getUniqueId().toString()).thenAccept(data -> {
            if (data == null) {
                Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.not_found", "Backpack not found.").color(NamedTextColor.RED);
                player.sendMessage(msg);
                return;
            }

            boolean hasPassword = (data.getPwdHash() != null && !data.getPwdHash().isEmpty()) || data.isLocked();

            if (!hasPassword) {
                Component msg = Component.text("Backpack doesn't have a password yet.").color(NamedTextColor.YELLOW);
                player.sendMessage(msg);
                return;
            }

            provider.deletePwd(uuid, providedPwd).thenAccept(success -> {
                if (success) {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.removed", "Backpack password removed.").color(NamedTextColor.GREEN);
                    player.sendMessage(msg);
                } else {
                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.incorrect", "Incorrect password").color(NamedTextColor.RED);
                    player.sendMessage(msg);
                }
            });
        });
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.deletepwd", "<password> - Unlock held backpack");
    }

    @Override
    public String getPermission() { return null; }
}
