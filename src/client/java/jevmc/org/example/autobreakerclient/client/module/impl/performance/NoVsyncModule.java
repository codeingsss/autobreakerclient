package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

public class NoVsyncModule extends Module {
    private boolean old;

    public NoVsyncModule() {
        super("No Vsync", "Disables vertical sync so the framerate is not capped to your monitor.", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getEnableVsync().getValue();
        MinecraftClient.getInstance().options.getEnableVsync().setValue(false);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getEnableVsync().setValue(old);
    }
}
