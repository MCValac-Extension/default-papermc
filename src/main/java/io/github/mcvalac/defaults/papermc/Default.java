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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
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
        plugin.getServer().getPluginManager().registerEvents(
            new HandleInventoryOpen(plugin, provider, passwordManager, cooldownManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new HandleInventoryClose(provider, cooldownManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new HandleChatInput(plugin, passwordManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new HandleInventoryRestrict(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new LHandleChangeTexture(plugin, provider), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new LHandleChangeModelData(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new LHandleCreateBackpack(plugin, provider), plugin);

        // Register /bp command
        registerCommand(plugin);

        logger.info("Extension loaded successfully.");
    }

    /**
     * Registers the /bp command dynamically into the Bukkit CommandMap.
     */
    private void registerCommand(JavaPlugin plugin) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            MCBackpackTabCompleter tabCompleter = new MCBackpackTabCompleter(commandManager);

            Command cmd = new Command("bp", "MCBackpack commands", "/bp help", Collections.singletonList("backpack")) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return commandManager.dispatch(sender, args);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
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
