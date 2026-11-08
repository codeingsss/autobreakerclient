package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

public class NoEntityShadowsModule extends Module {
    private boolean old;

    public NoEntityShadowsModule() {
        super("No Entity Shadows", "Disables the blob shadow drawn under entities.", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getEntityShadows().getValue();
        MinecraftClient.getInstance().options.getEntityShadows().setValue(false);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getEntityShadows().setValue(old);
    }
}
