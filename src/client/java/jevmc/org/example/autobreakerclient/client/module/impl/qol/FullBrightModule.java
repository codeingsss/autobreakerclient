package jevmc.org.example.autobreakerclient.client.module.impl.qol;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Ported from CoralMod (CC0-1.0, github.com/jirmjahu/coralmod): maxes out the
 * brightness option instead of a mixin-based gamma override.
 */
public class FullBrightModule extends Module {
    private double oldGamma;

    public FullBrightModule() {
        super("Full Bright", "Maxes out the brightness setting so dark areas are lit.", Category.QOL, false);
    }

    @Override
    protected void onEnable() {
        oldGamma = MinecraftClient.getInstance().options.getGamma().getValue();
        MinecraftClient.getInstance().options.getGamma().setValue(100.0);
    }

    @Override
    protected void onDisable() {
        MinecraftClient.getInstance().options.getGamma().setValue(oldGamma);
    }
}
