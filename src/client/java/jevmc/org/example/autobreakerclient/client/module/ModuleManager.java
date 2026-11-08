package jevmc.org.example.autobreakerclient.client.module;

import jevmc.org.example.autobreakerclient.client.config.ConfigManager;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.CoordinatesModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.FpsCounterModule;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.WatermarkModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.ExtremePerformanceModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.FastGraphicsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.MinimalParticlesModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoCloudsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoEntityShadowsModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoViewBobbingModule;
import jevmc.org.example.autobreakerclient.client.module.impl.performance.NoVsyncModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static boolean initialized = false;

    private ModuleManager() {
    }

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

        MODULES.add(new WatermarkModule());
        MODULES.add(new FpsCounterModule());
        MODULES.add(new CoordinatesModule());

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
