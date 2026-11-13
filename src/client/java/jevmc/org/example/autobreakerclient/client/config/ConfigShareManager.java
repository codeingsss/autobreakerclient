package jevmc.org.example.autobreakerclient.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Exports every module's enabled state, int settings and HUD positions as a
 * single compact, shareable code - no backend, no account, just a string you
 * paste to someone else and they get your exact setup back. Reuses the same
 * JSON shape {@link ConfigManager} persists to disk, just deflate-compressed
 * and base64url-encoded instead of written to a file.
 */
public final class ConfigShareManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Gson GSON = new Gson();
    private static final String PREFIX = "AB1:";

    private ConfigShareManager() {
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> exportCommand = ClientCommandManager.literal("export")
                    .executes(ctx -> {
                        String code = export(ModuleManager.getModules());
                        ctx.getSource().sendFeedback(Text.literal(code));
                        return 1;
                    });

            LiteralArgumentBuilder<FabricClientCommandSource> importCommand = ClientCommandManager.literal("import")
                    .then(ClientCommandManager.argument("code", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String code = StringArgumentType.getString(ctx, "code");
                                try {
                                    importCode(code, ModuleManager.getModules());
                                    ModuleManager.saveConfig();
                                    ctx.getSource().sendFeedback(Text.literal("Imported AutoBreaker config."));
                                    return 1;
                                } catch (RuntimeException e) {
                                    LOGGER.warn("Failed to import AutoBreaker config code", e);
                                    ctx.getSource().sendError(Text.literal("That doesn't look like a valid AutoBreaker config code."));
                                    return 0;
                                }
                            }));

            dispatcher.register(ClientCommandManager.literal("autobreaker")
                    .then(ClientCommandManager.literal("config")
                            .then(exportCommand)
                            .then(importCommand)));
        });
    }

    public static String export(List<Module> modules) {
        byte[] raw = GSON.toJson(ConfigManager.toJson(modules)).getBytes(StandardCharsets.UTF_8);

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            out.write(buffer, 0, count);
        }
        deflater.end();

        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }

    public static void importCode(String code, List<Module> modules) {
        String trimmed = code.trim();
        if (!trimmed.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Missing/unknown AutoBreaker config code prefix");
        }
        byte[] compressed = Base64.getUrlDecoder().decode(trimmed.substring(PREFIX.length()));

        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 3);
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                out.write(buffer, 0, count);
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Corrupt AutoBreaker config code", e);
        } finally {
            inflater.end();
        }

        JsonObject json = JsonParser.parseString(out.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        ConfigManager.fromJson(modules, json);
    }
}
