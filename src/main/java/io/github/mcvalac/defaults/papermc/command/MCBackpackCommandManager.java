package io.github.mcvalac.defaults.papermc.command;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.defaults.papermc.command.util.CHandleChangeTexture;
import io.github.mcvalac.defaults.papermc.command.util.CHandleCreateBackpack;
import io.github.mcvalac.defaults.papermc.command.util.CHandleSet;
import io.github.mcvalac.defaults.papermc.command.util.HandleChangePwd;
import io.github.mcvalac.defaults.papermc.command.util.HandleDeletePwd;
import io.github.mcvalac.defaults.papermc.command.util.HandleHelp;
import io.github.mcvalac.defaults.papermc.command.util.HandleSetPwd;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages all backpack subcommands and dispatches execution.
 */
public class MCBackpackCommandManager {

    private final Map<String, IBackpackCommandHandle> subcommands = new LinkedHashMap<>();

    /**
     * Constructs the command manager and registers all subcommands.
     *
     * @param plugin   The main plugin instance.
     * @param provider The backend provider instance.
     */
    public MCBackpackCommandManager(Plugin plugin, MCBackpackProvider provider) {
        subcommands.put("create", new CHandleCreateBackpack(plugin));
        subcommands.put("texture", new CHandleChangeTexture(plugin));
        subcommands.put("get", new CHandleSet(plugin));
        subcommands.put("setpwd", new HandleSetPwd(plugin, provider));
        subcommands.put("changepwd", new HandleChangePwd(plugin, provider));
        subcommands.put("deletepwd", new HandleDeletePwd(plugin, provider));
        subcommands.put("help", new HandleHelp(this));
    }

    /**
     * Dispatches the command to the appropriate subcommand handler.
     *
     * @param sender The command sender.
     * @param args   The full command arguments.
     * @return {@code true} if the command was handled.
     */
    public boolean dispatch(CommandSender sender, String[] args) {
        if (args.length == 0) {
            subcommands.get("help").invoke(sender, args);
            return true;
        }

        String sub = args[0].toLowerCase();
        IBackpackCommandHandle handle = subcommands.get(sub);

        if (handle == null) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.unknown_command", "Unknown subcommand.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            subcommands.get("help").invoke(sender, args);
            return true;
        }

        // Permission check
        if (handle.getPermission() != null && !sender.hasPermission(handle.getPermission())) {
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.no_permission", "You do not have permission to use this command.").color(NamedTextColor.RED);
            sender.sendMessage(msg);
            return true;
        }

        // Strip the subcommand name from args
        String[] subArgs = (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        handle.invoke(sender, subArgs);
        return true;
    }

    /**
     * Returns the map of registered subcommands.
     *
     * @return An unmodifiable view of the subcommand map.
     */
    public Map<String, IBackpackCommandHandle> getSubcommands() {
        return subcommands;
    }
}
