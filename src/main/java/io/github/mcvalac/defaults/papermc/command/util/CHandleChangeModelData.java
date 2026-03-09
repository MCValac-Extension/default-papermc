package io.github.mcvalac.defaults.papermc.command.util;

import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for giving the player a "Model Data Applicator" item.
 */
public class CHandleChangeModelData implements IBackpackCommandHandle {

    private final NamespacedKey applicatorKey;

    public CHandleChangeModelData(Plugin plugin) {
        this.applicatorKey = new NamespacedKey(plugin, "backpack_model_data_applicator");
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.translatable("mcvalac.mcbackpack.extension.default.msg.only_players", "Only players can use this command.").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(Component.translatable("mcvalac.mcbackpack.extension.default.msg.usage.get.model.data", "/bp get model data <model_data>").color(NamedTextColor.RED));
            return;
        }

        String modelData = String.join(" ", args);

        ItemStack applicator = new ItemStack(Material.PAPER);
        ItemMeta meta = applicator.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.translatable("mcvalac.mcbackpack.extension.default.item.model_data_applicator.name", "Backpack Model Data Applicator").color(NamedTextColor.LIGHT_PURPLE));
            meta.getPersistentDataContainer().set(applicatorKey, PersistentDataType.STRING, modelData);
            applicator.setItemMeta(meta);
        }

        var overflow = player.getInventory().addItem(applicator);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), applicator);
        }

        player.sendMessage(Component.translatable("mcvalac.mcbackpack.extension.default.msg.model_data.received", "Model data applicator received.").color(NamedTextColor.GREEN));
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcvalac.mcbackpack.extension.default.msg.help.get.model.data", "model data <model_data> - Get a model data applicator");
    }

    @Override
    public String getPermission() {
        return "mcbackpack.get";
    }
}
