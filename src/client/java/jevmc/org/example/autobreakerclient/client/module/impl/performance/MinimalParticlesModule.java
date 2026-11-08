package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticlesMode;

public class MinimalParticlesModule extends Module {
    private ParticlesMode old;

    public MinimalParticlesModule() {
        super("Minimal Particles", "Reduces particle density to the lowest vanilla setting.", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getParticles().getValue();
        MinecraftClient.getInstance().options.getParticles().setValue(ParticlesMode.MINIMAL);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getParticles().setValue(old);
    }
}
