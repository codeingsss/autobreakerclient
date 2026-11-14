package jevmc.org.example.autobreakerclient.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import jevmc.org.example.autobreakerclient.client.config.ProfileManager;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets a server address pick its own AutoBreaker profile (see
 * {@link ProfileManager}): run {@code /autobreaker profile set <name>} while
 * connected to a server and that server's address is remembered against the
 * given profile. The next time you join that address, {@link ProfileManager#load}
 * switches to it automatically.
 *
 * <p>Also quietly learns a per-server render distance: if the Performance
 * Governor module is enabled when you disconnect, whatever render distance
 * it settled on for that session is remembered against the address it was
 * learned on. The next time you join that same address, that remembered
 * value is applied immediately and the Governor is turned back on to keep
 * refining it - so a server with heavier tick load converges to a lower
 * distance than a quiet SMP does, automatically, with zero setup beyond
 * enabling the Governor once.</p>
 */
public final class ServerProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PROFILE_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Type TUNING_MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final String PERFORMANCE_GOVERNOR = "Performance Governor";

    private static Map<String, String> addressToProfile = new HashMap<>();
    private static Map<String, Integer> addressToRenderDistance = new HashMap<>();
    private static String connectedAddress;

    private ServerProfileManager() {
    }

    public static void init() {
        load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String address = currentServerAddress(client);
            connectedAddress = address;
            if (address == null) {
                return;
            }

            String profile = addressToProfile.get(address);
            if (profile != null) {
                if (!ProfileManager.exists(profile)) {
                    LOGGER.warn("Server profile mapping for {} points at missing profile '{}'", address, profile);
                } else {
                    ProfileManager.load(profile);
                    ModuleManager.saveConfig();
                }
            }

            Integer renderDistance = addressToRenderDistance.get(address);
            if (renderDistance != null) {
                client.options.getViewDistance().setValue(renderDistance);
                ModuleManager.getByName(PERFORMANCE_GOVERNOR).ifPresent(module -> module.setEnabled(true));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            String address = connectedAddress;
            connectedAddress = null;
            if (address == null) {
                return;
            }
            boolean governorActive = ModuleManager.getByName(PERFORMANCE_GOVERNOR)
                    .map(Module::isEnabled)
                    .orElse(false);
            if (!governorActive) {
                return;
            }
            GameOptions options = client.options;
            if (options == null) {
                return;
            }
            addressToRenderDistance.put(address, options.getViewDistance().getValue());
            save();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> profileSave = ClientCommandManager.literal("save")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                ProfileManager.save(name);
                                ctx.getSource().sendFeedback(Text.literal("Saved the current setup as profile \"" + name + "\"."));
                                return 1;
                            }));

            LiteralArgumentBuilder<FabricClientCommandSource> profileLoad = ClientCommandManager.literal("load")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                if (!ProfileManager.load(name)) {
                                    ctx.getSource().sendError(Text.literal("No profile named \"" + name + "\"."));
                                    return 0;
                                }
                                ModuleManager.saveConfig();
                                ctx.getSource().sendFeedback(Text.literal("Loaded profile \"" + name + "\"."));
                                return 1;
                            }));

            LiteralArgumentBuilder<FabricClientCommandSource> profileDelete = ClientCommandManager.literal("delete")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                if (!ProfileManager.delete(name)) {
                                    ctx.getSource().sendError(Text.literal("No profile named \"" + name + "\"."));
                                    return 0;
                                }
                                ctx.getSource().sendFeedback(Text.literal("Deleted profile \"" + name + "\"."));
                                return 1;
                            }));

            LiteralArgumentBuilder<FabricClientCommandSource> profileSet = ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                String address = currentServerAddress(MinecraftClient.getInstance());
                                if (address == null) {
                                    ctx.getSource().sendError(Text.literal("Not connected to a server."));
                                    return 0;
                                }
                                if (!ProfileManager.exists(name)) {
                                    ctx.getSource().sendError(Text.literal(
                                            "No profile named \"" + name + "\" - save one first with /autobreaker profile save " + name));
                                    return 0;
                                }
                                addressToProfile.put(address, name);
                                save();
                                ctx.getSource().sendFeedback(Text.literal(
                                        address + " will now load profile \"" + name + "\"."));
                                return 1;
                            }));

            LiteralArgumentBuilder<FabricClientCommandSource> profileClear = ClientCommandManager.literal("clear")
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
                    });

            LiteralArgumentBuilder<FabricClientCommandSource> profileList = ClientCommandManager.literal("list")
                    .executes(ctx -> {
                        var saved = ProfileManager.list();
                        if (saved.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No saved profiles yet - /autobreaker profile save <name>."));
                        } else {
                            ctx.getSource().sendFeedback(Text.literal("Saved profiles: " + String.join(", ", saved)));
                        }
                        if (!addressToProfile.isEmpty()) {
                            addressToProfile.forEach((address, profile) ->
                                    ctx.getSource().sendFeedback(Text.literal(address + " -> " + profile)));
                        }
                        return 1;
                    });

            LiteralArgumentBuilder<FabricClientCommandSource> tuneForget = ClientCommandManager.literal("forget")
                    .executes(ctx -> {
                        String address = currentServerAddress(MinecraftClient.getInstance());
                        if (address == null) {
                            ctx.getSource().sendError(Text.literal("Not connected to a server."));
                            return 0;
                        }
                        addressToRenderDistance.remove(address);
                        save();
                        ctx.getSource().sendFeedback(Text.literal(
                                "Forgot the learned render distance for " + address + "."));
                        return 1;
                    });

            LiteralArgumentBuilder<FabricClientCommandSource> tuneList = ClientCommandManager.literal("list")
                    .executes(ctx -> {
                        if (addressToRenderDistance.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal(
                                    "No learned render distances yet - enable Performance Governor and play a bit."));
                            return 1;
                        }
                        addressToRenderDistance.forEach((address, distance) ->
                                ctx.getSource().sendFeedback(Text.literal(address + " -> " + distance + " chunks")));
                        return 1;
                    });

            dispatcher.register(ClientCommandManager.literal("autobreaker")
                    .then(ClientCommandManager.literal("profile")
                            .then(profileSave)
                            .then(profileLoad)
                            .then(profileDelete)
                            .then(profileSet)
                            .then(profileClear)
                            .then(profileList))
                    .then(ClientCommandManager.literal("tune")
                            .then(tuneForget)
                            .then(tuneList)));
        });
    }

    private static String currentServerAddress(MinecraftClient client) {
        if (client.isInSingleplayer()) {
            return null;
        }
        ServerInfo server = client.getCurrentServerEntry();
        return server == null ? null : server.address;
    }

    private static Path profilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobreakerclient-server-profiles.json");
    }

    private static Path tuningPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobreakerclient-server-tuning.json");
    }

    private static void load() {
        Path profilePathValue = profilePath();
        if (Files.exists(profilePathValue)) {
            try {
                Map<String, String> loaded = GSON.fromJson(Files.readString(profilePathValue), PROFILE_MAP_TYPE);
                if (loaded != null) {
                    addressToProfile = loaded;
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load server profile mappings, starting empty", e);
            }
        }

        Path tuningPathValue = tuningPath();
        if (Files.exists(tuningPathValue)) {
            try {
                Map<String, Integer> loaded = GSON.fromJson(Files.readString(tuningPathValue), TUNING_MAP_TYPE);
                if (loaded != null) {
                    addressToRenderDistance = loaded;
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load server render distance mappings, starting empty", e);
            }
        }
    }

    private static void save() {
        Path profilePathValue = profilePath();
        try {
            Files.createDirectories(profilePathValue.getParent());
            Files.writeString(profilePathValue, GSON.toJson(addressToProfile));
        } catch (IOException e) {
            LOGGER.warn("Failed to save server profile mappings", e);
        }

        Path tuningPathValue = tuningPath();
        try {
            Files.createDirectories(tuningPathValue.getParent());
            Files.writeString(tuningPathValue, GSON.toJson(addressToRenderDistance));
        } catch (IOException e) {
            LOGGER.warn("Failed to save server render distance mappings", e);
        }
    }
}
