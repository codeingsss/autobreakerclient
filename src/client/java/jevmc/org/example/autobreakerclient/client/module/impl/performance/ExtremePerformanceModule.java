package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

import java.util.List;

/**
 * One-click bundle of vanilla client options that trades visuals for raw FPS.
 * Remembers the previous values so disabling the module restores them exactly.
 */
public class ExtremePerformanceModule extends Module {
    private final IntSetting renderDistance = new IntSetting("Render Distance", 6, 2, 32);

    private int oldMaxFps;
    private boolean oldVsync;
    private ParticlesMode oldParticles;
    private CloudRenderMode oldClouds;
    private GraphicsMode oldGraphics;
    private boolean oldEntityShadows;
    private boolean oldBobView;
    private int oldBiomeBlend;
    private int oldViewDistance;

    public ExtremePerformanceModule() {
        super("Extreme Performance", "Maximises FPS by disabling vsync, particles, clouds and shadows.", Category.PERFORMANCE, false);
        renderDistance.setOnChange(this::applyViewDistance);
    }

    @Override
    protected void onEnable() {
        GameOptions options = MinecraftClient.getInstance().options;

        oldMaxFps = options.getMaxFps().getValue();
        oldVsync = options.getEnableVsync().getValue();
        oldParticles = options.getParticles().getValue();
        oldClouds = options.getCloudRenderMode().getValue();
        oldGraphics = options.getPreset().getValue();
        oldEntityShadows = options.getEntityShadows().getValue();
        oldBobView = options.getBobView().getValue();
        oldBiomeBlend = options.getBiomeBlendRadius().getValue();
        oldViewDistance = options.getViewDistance().getValue();

        options.getMaxFps().setValue(260);
        options.getEnableVsync().setValue(false);
        options.getParticles().setValue(ParticlesMode.MINIMAL);
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        options.applyGraphicsMode(GraphicsMode.FAST);
        options.getEntityShadows().setValue(false);
        options.getBobView().setValue(false);
        options.getBiomeBlendRadius().setValue(0);
        applyViewDistance();
    }

    @Override
    protected void onDisable() {
        GameOptions options = MinecraftClient.getInstance().options;

        options.getMaxFps().setValue(oldMaxFps);
        options.getEnableVsync().setValue(oldVsync);
        options.getParticles().setValue(oldParticles);
        options.getCloudRenderMode().setValue(oldClouds);
        options.applyGraphicsMode(oldGraphics);
        options.getEntityShadows().setValue(oldEntityShadows);
        options.getBobView().setValue(oldBobView);
        options.getBiomeBlendRadius().setValue(oldBiomeBlend);
        options.getViewDistance().setValue(oldViewDistance);
    }

    private void applyViewDistance() {
        if (isEnabled()) {
            MinecraftClient.getInstance().options.getViewDistance().setValue(renderDistance.getValue());
        }
    }

    @Override
    public List<jevmc.org.example.autobreakerclient.client.module.setting.Setting<?>> getSettings() {
        return List.of(renderDistance);
    }
}
