package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;

public class NoCloudsModule extends Module {
    private CloudRenderMode old;

    public NoCloudsModule() {
        super("No Clouds", "Turns off cloud rendering.", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getCloudRenderMode().getValue();
        MinecraftClient.getInstance().options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getCloudRenderMode().setValue(old);
    }
}
