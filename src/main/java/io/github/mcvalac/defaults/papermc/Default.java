package io.github.mcvalac.defaults.papermc;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.defaults.papermc.command.MCBackpackCommandManager;
import io.github.mcvalac.defaults.papermc.listener.util.HandleChatInput;
import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryClose;
import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryOpen;
import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryRestrict;
import io.github.mcvalac.defaults.papermc.listener.util.LHandleChangeModelData;
import io.github.mcvalac.defaults.papermc.listener.util.LHandleChangeTexture;
import io.github.mcvalac.defaults.papermc.listener.util.LHandleCreateBackpack;
import io.github.mcvalac.defaults.papermc.manager.BackpackCooldownManager;
import io.github.mcvalac.defaults.papermc.manager.PasswordInputManager;
import io.github.mcvalac.defaults.papermc.tabcompleter.MCBackpackTabCompleter;
import io.github.mcvalac.mcextension.api.IMCExtension;
import io.github.mcvalac.mcextension.common.MCExtensionLogger;
import io.github.mcvalac.mcutil.MCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Main entry point for the Default MCBackpack extension.
 * <p>
 * This class implements the IMCExtension interface to integrate with
 * the MCBackpack extension system, registering the /bp command and all listeners.
 * </p>
 */
public class Default implements IMCExtension {

    private final MCExtensionLogger logger = new MCExtensionLogger("MCBackpack", "Default");
    private MCBackpackCommandManager commandManager;
    private PasswordInputManager passwordManager;
    private BackpackCooldownManager cooldownManager;
    
    // ADDED: Keep track of registered listeners to unregister them on disable
    private final List<Listener> registeredListeners = new ArrayList<>();

    /**
     * Called when the extension is loaded by MCBackpack.
     * Initializes the provider, managers, listeners, and commands.
     *
     * @param plugin   The host JavaPlugin.
     * @param executor The shared executor service.
     */
    @Override
    public void onLoad(JavaPlugin plugin, Executor executor) {
        MCBackpackProvider provider = MCBackpackProvider.getProvider();

        if (provider == null) {
            logger.error("MCBackpackProvider is not initialized. Extension cannot load.");
            return;
        }

        // Initialize managers
        this.passwordManager = new PasswordInputManager();
        this.cooldownManager = new BackpackCooldownManager(Duration.ofSeconds(2));

        // Initialize command manager
        this.commandManager = new MCBackpackCommandManager(plugin, provider);

        // Register listeners
        registerListener(plugin, provider);

        // Register /bp command
        registerCommand(plugin);

        logger.info("Extension loaded successfully.");
    }

    /**
     * Registers all event listeners for this extension.
     *
     * @param plugin   The host JavaPlugin.
     * @param provider The MCBackpack provider instance.
     */
    private void registerListener(JavaPlugin plugin, MCBackpackProvider provider) {
        // Clear just in case
        registeredListeners.clear();

        // Add all listeners to our tracking list
        registeredListeners.add(new HandleInventoryOpen(plugin, provider, passwordManager, cooldownManager));
        registeredListeners.add(new HandleInventoryClose(provider, cooldownManager));
        registeredListeners.add(new HandleChatInput(plugin, passwordManager));
        registeredListeners.add(new HandleInventoryRestrict(plugin));
        registeredListeners.add(new LHandleChangeTexture(plugin, provider));
        registeredListeners.add(new LHandleChangeModelData(plugin));
        registeredListeners.add(new LHandleCreateBackpack(plugin, provider));

        // Register them all with Bukkit
        for (Listener listener : registeredListeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    /**
     * Registers the /bp command dynamically into the Bukkit CommandMap.
     */
    private void registerCommand(JavaPlugin plugin) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Remove old ghost commands from previous loads/reloads to prevent NPEs
            try {
                Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
                knownCommands.remove("bp");
                knownCommands.remove("backpack");
                knownCommands.remove(plugin.getName().toLowerCase() + ":bp");
                knownCommands.remove(plugin.getName().toLowerCase() + ":backpack");
            } catch (Exception ignored) {
                // Ignore if knownCommands structure varies on some server forks
            }

            MCBackpackTabCompleter tabCompleter = new MCBackpackTabCompleter(commandManager);

            Command cmd = new Command("bp", "MCBackpack commands", "/bp help", Collections.singletonList("backpack")) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    // Prevent NPE if command executes during an uninitialized state
                    if (commandManager == null) {
                        sender.sendMessage(Component.text("MCBackpack command system is currently reloading or unavailable.").color(NamedTextColor.RED));
                        return true;
                    }
                    return commandManager.dispatch(sender, args);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    // Prevent NPE for tab complete
                    if (commandManager == null) return Collections.emptyList();
                    return tabCompleter.onTabComplete(sender, this, alias, args);
                }
            };

            commandMap.register(plugin.getName(), cmd);

        } catch (Exception e) {
            logger.error("Failed to register /bp command: " + e.getMessage());
        }
    }

    /**
     * Called when the extension is disabled.
     *
     * @param plugin   The host JavaPlugin.
     * @param executor The shared executor service.
     */
    @Override
    public void onDisable(JavaPlugin plugin, Executor executor) {
        // FIX: Unregister all listeners specific to this extension to prevent duplicates on reload
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();

        this.commandManager = null;
        this.passwordManager = null;
        this.cooldownManager = null;
        logger.info("Extension disabled.");
    }

    private String loadVersionFromYml() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("extension.yml");
        if (stream == null) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        return config.getString("version");
    }

    @Override
    public boolean checkUpdate(String url, String token) {
        String version = loadVersionFromYml();
        if (version == null) {
            version = "0.0.0";
            logger.error("Could not load version from extension.yml");
        }

        try {
            return MCUtil.compareVersion(
                "github",
                version,
                "MCValac-Extension",
                "default-papermc",
                token
            );
        } catch (Exception e) {
            logger.error("Update check failed: " + e.getMessage());
            return false;
        }
    }
}
