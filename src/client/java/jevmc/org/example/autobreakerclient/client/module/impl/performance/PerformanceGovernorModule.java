package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import jevmc.org.example.autobreakerclient.client.module.setting.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.particle.ParticlesMode;

import java.util.List;

/**
 * Live performance governor: samples {@link MinecraftClient#getCurrentFps()}
 * roughly once a second and nudges render distance (and, if that alone isn't
 * enough, particle density) up or down to track a target FPS, instead of a
 * one-shot preset that stays fixed regardless of what the scene is actually
 * doing. A wide hysteresis band (steps only past 20% under or over target)
 * keeps it from constantly flapping between two values.
 */
public class PerformanceGovernorModule extends Module {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double STEP_DOWN_RATIO = 0.8;
    private static final double STEP_UP_RATIO = 1.2;
    private static final double EMERGENCY_RATIO = 0.6;
    private static final double RECOVER_RATIO = 0.9;

    private final IntSetting targetFps = new IntSetting("Target FPS", 240, 60, 500);
    private final IntSetting minRenderDistance = new IntSetting("Min Render Distance", 4, 2, 32);
    private final IntSetting maxRenderDistance = new IntSetting("Max Render Distance", 16, 2, 32);

    private int tickCounter;
    private int baselineViewDistance;
    private ParticlesMode baselineParticles;
    private boolean particlesReduced;

    public PerformanceGovernorModule() {
        super("Performance Governor",
                "Automatically tracks a target FPS by live-adjusting render distance and particles.",
                Category.PERFORMANCE, false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled() || client.player == null) {
                return;
            }
            if (++tickCounter < CHECK_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            step(client);
        });
    }

    @Override
    protected void onEnable() {
        GameOptions options = MinecraftClient.getInstance().options;
        baselineViewDistance = options.getViewDistance().getValue();
        baselineParticles = options.getParticles().getValue();
        particlesReduced = false;
        tickCounter = 0;
    }

    @Override
    protected void onDisable() {
        GameOptions options = MinecraftClient.getInstance().options;
        options.getViewDistance().setValue(baselineViewDistance);
        options.getParticles().setValue(baselineParticles);
        particlesReduced = false;
    }

    private void step(MinecraftClient client) {
        GameOptions options = client.options;
        int fps = client.getCurrentFps();
        if (fps <= 0) {
            return;
        }
        double ratio = fps / (double) targetFps.getValue();

        int viewDistance = options.getViewDistance().getValue();
        if (ratio < STEP_DOWN_RATIO && viewDistance > minRenderDistance.getValue()) {
            options.getViewDistance().setValue(viewDistance - 1);
        } else if (ratio > STEP_UP_RATIO && viewDistance < maxRenderDistance.getValue()) {
            options.getViewDistance().setValue(viewDistance + 1);
        }

        if (ratio < EMERGENCY_RATIO && !particlesReduced) {
            particlesReduced = true;
            options.getParticles().setValue(ParticlesMode.MINIMAL);
        } else if (ratio > RECOVER_RATIO && particlesReduced) {
            particlesReduced = false;
            options.getParticles().setValue(baselineParticles);
        }
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(targetFps, minRenderDistance, maxRenderDistance);
    }
}
