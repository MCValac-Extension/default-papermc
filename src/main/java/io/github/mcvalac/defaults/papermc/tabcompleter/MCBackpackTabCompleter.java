package io.github.mcvalac.defaults.papermc.tabcompleter;

import io.github.mcvalac.mcbackpack.api.command.IBackpackCommandHandle;
import io.github.mcvalac.defaults.papermc.command.MCBackpackCommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles tab completion for the main backpack command.
 * <p>
 * This class dynamically generates suggestions based on the user's permissions
 * and the specific subcommand arguments (e.g., valid backpack sizes).
 * </p>
 */
public class MCBackpackTabCompleter implements TabCompleter {

    /** The command manager used to retrieve registered subcommands. */
    private final MCBackpackCommandManager manager;

    /** A predefined list of valid backpack sizes (multiples of 9) for auto-completion. */
    private final List<String> backpackSizes = Arrays.asList("9", "18", "27", "36", "45", "54");

    /**
     * Constructs the tab completer.
     *
     * @param manager The command manager instance.
     */
    public MCBackpackTabCompleter(MCBackpackCommandManager manager) {
        this.manager = manager;
    }

    /**
     * Generates a list of suggested arguments for the command line.
     *
     * @param sender  The entity sending the command.
     * @param command The command object.
     * @param alias   The alias used.
     * @param args    The arguments currently typed.
     * @return A list of suggestions, or an empty list if none are applicable.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        // Argument 1: Subcommand Selection
        if (args.length == 1) {
            for (Map.Entry<String, IBackpackCommandHandle> entry : manager.getSubcommands().entrySet()) {
                String name = entry.getKey();
                IBackpackCommandHandle handle = entry.getValue();

                // Only suggest if player has permission
                if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                    suggestions.add(name);
                }
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }

        // Argument 2+: Subcommand Specific Args
        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("create")) {
                return StringUtil.copyPartialMatches(args[1], backpackSizes, new ArrayList<>());
            } else if (sub.equals("get")) {
                return StringUtil.copyPartialMatches(args[1], Collections.singletonList("model"), new ArrayList<>());
            } else if (sub.equals("setpwd") || sub.equals("checkpwd") || sub.equals("deletepwd")) {
                return Collections.singletonList("<password>");
            } else if (sub.equals("changepwd")) {
                return Collections.singletonList("<oldPassword>");
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("changepwd")) {
                return Collections.singletonList("<newPassword>");
            } else if (sub.equals("get") && args[1].equalsIgnoreCase("model")) {
                return StringUtil.copyPartialMatches(args[2], Collections.singletonList("data"), new ArrayList<>());
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("get") && args[1].equalsIgnoreCase("model") && args[2].equalsIgnoreCase("data")) {
                return Collections.singletonList("<string>");
            }
        }

        return Collections.emptyList();
    }
}
