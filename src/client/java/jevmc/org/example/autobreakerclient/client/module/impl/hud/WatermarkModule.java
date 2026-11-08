package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WatermarkModule extends HudModule {
    public WatermarkModule() {
        super("Watermark", "Shows the AutoBreaker name tag in the top-left corner.");
    }

    @Override
    public int renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = "AutoBreaker";
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFF55C1FF);
        return client.textRenderer.fontHeight + 2;
    }
}
