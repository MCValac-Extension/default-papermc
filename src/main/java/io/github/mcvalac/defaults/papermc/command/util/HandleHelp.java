package io.github.mcvalac.defaults.papermc.command.util;

import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import io.github.mcvalac.defaults.papermc.command.MCBackpackCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Command handler that displays a help menu listing all available subcommands.
 */
public class HandleHelp implements IBackpackCommandHandle {

    /** The command manager used to retrieve the list of registered commands. */
    private final MCBackpackCommandManager manager;

    /**
     * Constructs the help command handler.
     *
     * @param manager The manager containing registered subcommands.
     */
    public HandleHelp(MCBackpackCommandManager manager) {
        this.manager = manager;
    }

    /**
     * Executes the help logic.
     * Generates a clickable list of commands based on the player's permissions.
     *
     * @param sender The command sender.
     * @param args   Ignored.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        Component header = Component.translatable("mcengine.mcbackpack.msg.help.header", "MCBackpack Commands")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
        sender.sendMessage(header);

        for (Map.Entry<String, IBackpackCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            IBackpackCommandHandle handle = entry.getValue();

            if (name.equalsIgnoreCase("help")) continue;

            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                String fullCommand = "/bp " + name;

                // Translatable hover
                Component hoverText = Component.translatable("mcengine.mcbackpack.msg.help.hover", "Click to suggest command").color(NamedTextColor.GREEN);

                Component cmdText = Component.text(fullCommand + " ")
                        .color(NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.suggestCommand(fullCommand + " "))
                        .hoverEvent(HoverEvent.showText(hoverText));

                // Fetch help component (Translatable) and style it
                Component helpMsg = handle.getHelp().color(NamedTextColor.GRAY);

                Component builder = cmdText.append(helpMsg);
                sender.sendMessage(builder);
            }
        }
    }

    /**
     * Retrieves the help string for the help command.
     *
     * @return The usage syntax.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mcbackpack.msg.help.help", "- View this help menu");
    }

    /**
     * Retrieves the permission node for the help command.
     *
     * @return null, indicating no permission is required.
     */
    @Override
    public String getPermission() { return null; }
}
