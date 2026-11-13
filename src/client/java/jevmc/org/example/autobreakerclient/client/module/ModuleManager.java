package jevmc.org.example.autobreakerclient.client.module;

import jevmc.org.example.autobreakerclient.client.config.ConfigManager;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.ArmorHudModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.ClockModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.CoordinatesModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.CpsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.FpsCounterModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.NetworkDoctorModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.PingModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.ServerAddressModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.WatermarkModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.ExtremePerformanceModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.FastGraphicsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.MinimalParticlesModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoCloudsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoEntityShadowsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoViewBobbingModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoVsyncModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.PerformanceGovernorModule;
import jevmc.org.example.autobreakerclient.client.module.impl.qol.AutoGgModule;
import jevmc.org.example.autobreakerclient.client.module.impl.qol.FullBrightModule;
import jevmc.org.example.autobreakerclient.client.module.impl.qol.ZoomModule;
import jevmc.org.example.autobreakerclient.client.module.impl.visual.MotionBlurModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static boolean initialized = false;

    private ModuleManager() {
    }

    /**
     * Constructs every module (registering keybindings, event listeners, etc.
     * along the way) without touching saved config yet. Must run during
     * {@code onInitializeClient()} so keybindings register before
     * {@code GameOptions} finalizes its key list.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        MODULES.add(new ExtremePerformanceModule());
        MODULES.add(new FastGraphicsModule());
        MODULES.add(new NoVsyncModule());
        MODULES.add(new NoCloudsModule());
        MODULES.add(new MinimalParticlesModule());
        MODULES.add(new NoEntityShadowsModule());
        MODULES.add(new NoViewBobbingModule());
        MODULES.add(new PerformanceGovernorModule());

        MODULES.add(new ZoomModule());
        MODULES.add(new FullBrightModule());
        MODULES.add(new AutoGgModule());

        MODULES.add(new MotionBlurModule());

        MODULES.add(new WatermarkModule());
        MODULES.add(new FpsCounterModule());
        MODULES.add(new CoordinatesModule());
        MODULES.add(new PingModule());
        MODULES.add(new CpsModule());
        MODULES.add(new ClockModule());
        MODULES.add(new ArmorHudModule());
        MODULES.add(new ServerAddressModule());
        MODULES.add(new NetworkDoctorModule());
    }

    /**
     * Applies saved config (enabling modules, restoring slider/HUD-position
     * values). Must run only after the client is fully up — enabling a
     * module can touch {@code MinecraftClient.getInstance().options}, which
     * is still null during {@code onInitializeClient()}.
     */
    public static void loadConfig() {
        ConfigManager.load(MODULES);
    }

    public static List<Module> getModules() {
        return MODULES;
    }

    public static List<Module> getModules(Category category) {
        return MODULES.stream().filter(module -> module.getCategory() == category).toList();
    }

    public static Optional<Module> getByName(String name) {
        return MODULES.stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static void saveConfig() {
        ConfigManager.save(MODULES);
    }
}
