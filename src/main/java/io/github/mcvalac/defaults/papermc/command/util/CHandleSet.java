package io.github.mcvalac.defaults.papermc.command.util;

import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for the "get" subcommand, delegating to sub-subcommands.
 */
public class CHandleSet implements IBackpackCommandHandle {

    private final IBackpackCommandHandle changeModelData;

    public CHandleSet(Plugin plugin) {
        this.changeModelData = new CHandleChangeModelData(plugin);
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("model") && args[1].equalsIgnoreCase("data")) {
            String[] subArgs = (args.length <= 2) ? new String[0] : java.util.Arrays.copyOfRange(args, 2, args.length);
            changeModelData.invoke(sender, subArgs);
            return;
        }

        sender.sendMessage(Component.translatable("mcengine.mcbackpack.msg.usage.get.model.data", "/bp get model data <model_data>").color(NamedTextColor.RED));
    }

    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mcbackpack.msg.help.get", "model data <model_data> - Get set subcommands");
    }

    @Override
    public String getPermission() {
        return "mcbackpack.get";
    }
}
