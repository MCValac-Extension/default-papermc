package io.github.mcvalac.defaults.papermc.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Contract for backpack subcommand handlers.
 */
public interface IBackpackCommandHandle {

    /**
     * Executes the subcommand.
     *
     * @param sender The command sender.
     * @param args   The arguments after subcommand name.
     */
    void invoke(CommandSender sender, String[] args);

    /**
     * Gets subcommand help text.
     *
     * @return Help component.
     */
    Component getHelp();

    /**
     * Gets required permission for this subcommand.
     *
     * @return Permission node, or null if no permission required.
     */
    String getPermission();
}
