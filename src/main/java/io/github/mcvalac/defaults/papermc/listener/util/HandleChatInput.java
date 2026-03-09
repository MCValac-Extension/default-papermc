package io.github.mcvalac.defaults.papermc.listener.util;

import io.github.mcvalac.mcbackpack.api.model.BackpackData;
import io.github.mcvalac.defaults.papermc.listener.util.HandleInventoryOpen.BackpackHolder;
import io.github.mcvalac.defaults.papermc.manager.PasswordInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Listens for chat events to handle password input for locked backpacks.
 * <p>
 * When a player attempts to open a locked backpack, they are placed in a pending state.
 * This listener intercepts their next chat message to validate the password.
 * </p>
 */
public class HandleChatInput implements Listener {

    private static final String PBKDF2_PREFIX = "pbkdf2$";
    private static final int PBKDF2_KEY_LENGTH = 256;

    /** The main plugin instance, used for scheduling synchronous tasks. */
    private final Plugin plugin;

    /** The manager tracking players currently waiting to input a password. */
    private final PasswordInputManager passwordManager;

    /**
     * Constructs the chat input listener.
     *
     * @param plugin          The main plugin instance.
     * @param passwordManager The manager state for password inputs.
     */
    public HandleChatInput(Plugin plugin, PasswordInputManager passwordManager) {
        this.plugin = plugin;
        this.passwordManager = passwordManager;
    }

    /**
     * Intercepts chat messages from players in the password pending list.
     * <p>
     * If the player is pending, the event is cancelled (hiding the password from chat),
     * and the input is verified against the stored backpack hash.
     * </p>
     *
     * @param event The chat event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!passwordManager.isPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        BackpackData data = passwordManager.getPending(player.getUniqueId());

        if (input.equalsIgnoreCase("cancel")) {
            passwordManager.removePending(player.getUniqueId());
            Component msg = Component.translatable("mcengine.mcbackpack.msg.cancelled", "Backpack opening cancelled").color(NamedTextColor.YELLOW);
            player.sendMessage(msg);
            return;
        }

        if (verifyPassword(input, data.getPwdHash())) {
            Component msg = Component.translatable("mcengine.mcbackpack.msg.password.correct", "Password correct").color(NamedTextColor.GREEN);
            player.sendMessage(msg);

            passwordManager.removePending(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Component title = Component.translatable("mcengine.mcbackpack.gui.title", "Backpack");

                    Inventory backpackInv;
                    if (data.getContent() == null || data.getContent().isEmpty()) {
                        backpackInv = Bukkit.createInventory(new BackpackHolder(data.getUuid()), data.getSize(), title);
                    } else {
                        backpackInv = fromBase64(data.getContent(), data.getSize(), data.getUuid(), title);
                    }
                    player.openInventory(backpackInv);
                } catch (Exception e) {
                    e.printStackTrace();
                    Component err = Component.translatable("mcengine.mcbackpack.msg.error.item.open", "Could not open backpack").color(NamedTextColor.RED);
                    player.sendMessage(err);
                }
            });

        } else {
            Component incorrect = Component.translatable("mcengine.mcbackpack.msg.password.incorrect", "Incorrect password").color(NamedTextColor.RED);
            Component tryAgain = Component.translatable("mcengine.mcbackpack.msg.try.again", "Please try again").color(NamedTextColor.RED);

            Component builder = incorrect.append(Component.text(" ")).append(tryAgain);
            player.sendMessage(builder);
        }
    }

    /**
     * Verifies if the raw input matches the stored salted hash.
     *
     * @param inputRaw   The raw password input from chat.
     * @param storedHash The stored hash from the database.
     * @return {@code true} if matches, {@code false} otherwise.
     */
    private boolean verifyPassword(String inputRaw, String storedHash) {
        try {
            if (storedHash == null) return false;

            if (storedHash.startsWith(PBKDF2_PREFIX)) {
                String[] parts = storedHash.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);

                KeySpec spec = new PBEKeySpec(inputRaw.toCharArray(), salt, iterations, PBKDF2_KEY_LENGTH);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] actual = skf.generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(actual, expected);
            }

            if (!storedHash.contains("$")) return false;
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) return false;
            String salt = parts[0];
            String expectedHex = parts[1];

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actual = digest.digest((salt + inputRaw).getBytes(StandardCharsets.UTF_8));
            byte[] expected = hexToBytes(expectedHex);
            return expected != null && MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) return null;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) return null;
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    /**
     * Deserializes a Base64 string into a Bukkit Inventory.
     *
     * @param base64 The Base64 encoded inventory data.
     * @param size   The size of the inventory.
     * @param uuid   The UUID of the backpack owner/holder.
     * @param title  The inventory title.
     * @return The reconstructed Inventory object.
     * @throws IOException If IO errors occur.
     */
    private Inventory fromBase64(String base64, int size, String uuid, Component title) throws IOException {
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(uuid), size, title);
        if (base64 == null || base64.isEmpty()) return inventory;

        byte[] data = Base64.getDecoder().decode(base64);
        ItemStack[] items = ItemStack.deserializeItemsFromBytes(data);

        inventory.setContents(items);
        return inventory;
    }
}
