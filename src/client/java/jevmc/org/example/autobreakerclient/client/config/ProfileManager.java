package jevmc.org.example.autobreakerclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Named snapshots of {@link ModuleManager}'s state (reusing the same JSON
 * shape {@link ConfigManager} already builds for the default config and for
 * config-sharing codes), saved one-per-file under
 * {@code config/autobreakerclient-profiles/<name>.json}. Lets
 * {@link jevmc.org.example.autobreakerclient.client.profile.ServerProfileManager}
 * swap your whole setup per server address without depending on any
 * third-party config library.
 */
public final class ProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProfileManager() {
    }

    public static List<String> list() {
        Path dir = profilesDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("Failed to list profiles", e);
            return List.of();
        }
    }

    public static boolean exists(String name) {
        return Files.exists(profilePath(name));
    }

    public static void save(String name) {
        JsonObject json = ConfigManager.toJson(ModuleManager.getModules());
        Path path = profilePath(name);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(json));
        } catch (IOException e) {
            LOGGER.warn("Failed to save profile '{}'", name, e);
        }
    }

    public static boolean load(String name) {
        Path path = profilePath(name);
        if (!Files.exists(path)) {
            return false;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            ConfigManager.fromJson(ModuleManager.getModules(), json);
            return true;
        } catch (IOException | IllegalStateException e) {
            LOGGER.warn("Failed to load profile '{}'", name, e);
            return false;
        }
    }

    public static boolean delete(String name) {
        try {
            return Files.deleteIfExists(profilePath(name));
        } catch (IOException e) {
            LOGGER.warn("Failed to delete profile '{}'", name, e);
            return false;
        }
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobreakerclient-profiles");
    }

    private static Path profilePath(String name) {
        return profilesDir().resolve(name + ".json");
    }
}
