package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;

public class FastGraphicsModule extends Module {
    private GraphicsMode old;

    public FastGraphicsModule() {
        super("Fast Graphics", "Forces the Fast graphics preset (no ambient occlusion, cutout leaves).", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getPreset().getValue();
        MinecraftClient.getInstance().options.applyGraphicsMode(GraphicsMode.FAST);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.applyGraphicsMode(old);
    }
}
