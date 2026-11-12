package jevmc.org.example.autobreakerclient.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets a server address pick its own OneConfig profile: run
 * {@code /autobreaker profile set <name>} while connected to a server and
 * that server's address is remembered against the given profile. The next
 * time you join that address, {@link ConfigManager#openProfile(String)}
 * switches to it automatically.
 */
public final class ServerProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static Map<String, String> addressToProfile = new HashMap<>();

    private ServerProfileManager() {
    }

    public static void init() {
        load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String address = currentServerAddress(client);
            if (address == null) {
                return;
            }
            String profile = addressToProfile.get(address);
            if (profile == null) {
                return;
            }
            if (!ConfigManager.profiles().contains(profile)) {
                LOGGER.warn("Server profile mapping for {} points at missing profile '{}'", address, profile);
                return;
            }
            if (!profile.equals(ConfigManager.activeProfile())) {
                ConfigManager.openProfile(profile);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("autobreaker")
                        .then(ClientCommandManager.literal("profile")
                                .then(ClientCommandManager.literal("set")
                                        .then(ClientCommandManager.argument("profile", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String profile = StringArgumentType.getString(ctx, "profile");
                                                    String address = currentServerAddress(MinecraftClient.getInstance());
                                                    if (address == null) {
                                                        ctx.getSource().sendError(Text.literal("Not connected to a server."));
                                                        return 0;
                                                    }
                                                    addressToProfile.put(address, profile);
                                                    save();
                                                    ctx.getSource().sendFeedback(Text.literal(
                                                            address + " will now load OneConfig profile \"" + profile + "\"."));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("clear")
                                        .executes(ctx -> {
                                            String address = currentServerAddress(MinecraftClient.getInstance());
                                            if (address == null) {
                                                ctx.getSource().sendError(Text.literal("Not connected to a server."));
                                                return 0;
                                            }
                                            addressToProfile.remove(address);
                                            save();
                                            ctx.getSource().sendFeedback(Text.literal("Cleared the profile mapping for " + address + "."));
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> {
                                            if (addressToProfile.isEmpty()) {
                                                ctx.getSource().sendFeedback(Text.literal("No server profile mappings set."));
                                                return 1;
                                            }
                                            addressToProfile.forEach((address, profile) ->
                                                    ctx.getSource().sendFeedback(Text.literal(address + " -> " + profile)));
                                            return 1;
                                        })))));
    }

    private static String currentServerAddress(MinecraftClient client) {
        if (client.isInSingleplayer()) {
            return null;
        }
        ServerInfo server = client.getCurrentServerEntry();
        return server == null ? null : server.address;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobreakerclient-server-profiles.json");
    }

    private static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            Map<String, String> loaded = GSON.fromJson(Files.readString(path), MAP_TYPE);
            if (loaded != null) {
                addressToProfile = loaded;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load server profile mappings, starting empty", e);
        }
    }

    private static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(addressToProfile));
        } catch (IOException e) {
            LOGGER.warn("Failed to save server profile mappings", e);
        }
    }
}
