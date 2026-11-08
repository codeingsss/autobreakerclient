package jevmc.org.example.autobreakerclient.client.module.impl.performance;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

public class NoViewBobbingModule extends Module {
    private boolean old;

    public NoViewBobbingModule() {
        super("No View Bobbing", "Disables the camera bob animation while walking.", Category.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        old = MinecraftClient.getInstance().options.getBobView().getValue();
        MinecraftClient.getInstance().options.getBobView().setValue(false);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getBobView().setValue(old);
    }
}
