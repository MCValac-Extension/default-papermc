package io.github.mcvalac.defaults.papermc;

import io.github.mcvalac.mcextension.api.IMCExtension;
import io.github.mcvalac.mcextension.common.MCExtensionLogger;
import io.github.mcvalac.mcutil.MCUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;

public class Default implements IMCExtension {

    private final MCExtensionLogger logger = new MCExtensionLogger("MCBackpack", "Default");

    @Override
    public void onLoad(JavaPlugin plugin, Executor executor) {
        logger.info("Extension loaded successfully.");
    }

    /**
     * Called when the extension is disabled.
     *
     * @param plugin   The host JavaPlugin.
     * @param executor The shared executor service.
     */
    @Override
    public void onDisable(JavaPlugin plugin, Executor executor) {
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
