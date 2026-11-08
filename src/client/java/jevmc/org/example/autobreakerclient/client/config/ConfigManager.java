package jevmc.org.example.autobreakerclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.HudModule;
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import jevmc.org.example.autobreakerclient.client.module.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobreakerclient.json");
    }

    public static void load(List<Module> modules) {
        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject modulesJson = root.getAsJsonObject("modules");
            if (modulesJson == null) {
                return;
            }
            for (Module module : modules) {
                if (!modulesJson.has(module.getName())) {
                    continue;
                }
                JsonObject entry = modulesJson.getAsJsonObject(module.getName());
                JsonObject settings = entry.getAsJsonObject("settings");
                if (settings != null) {
                    for (Setting<?> setting : module.getSettings()) {
                        if (setting instanceof IntSetting intSetting && settings.has(setting.getName())) {
                            intSetting.set(settings.get(setting.getName()).getAsInt());
                        }
                    }
                }
                if (module instanceof HudModule hudModule && entry.has("x") && entry.has("y")) {
                    hudModule.setPosition(entry.get("x").getAsInt(), entry.get("y").getAsInt());
                }
                if (entry.has("enabled") && entry.get("enabled").getAsBoolean()) {
                    module.setEnabled(true);
                }
            }
        } catch (IOException | IllegalStateException e) {
            LOGGER.warn("Failed to load AutoBreaker config, using defaults", e);
        }
    }

    public static void save(List<Module> modules) {
        JsonObject root = new JsonObject();
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.isEnabled());
            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof IntSetting intSetting) {
                    settings.addProperty(setting.getName(), intSetting.getValue());
                }
            }
            entry.add("settings", settings);
            if (module instanceof HudModule hudModule) {
                entry.addProperty("x", hudModule.getX());
                entry.addProperty("y", hudModule.getY());
            }
            modulesJson.add(module.getName(), entry);
        }
        root.add("modules", modulesJson);

        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.warn("Failed to save AutoBreaker config", e);
        }
    }
}
