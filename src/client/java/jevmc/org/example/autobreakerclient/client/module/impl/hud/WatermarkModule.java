package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WatermarkModule extends HudModule {
    public WatermarkModule() {
        super("Watermark", "Shows the AutoBreaker name tag in the top-left corner.", true);
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = "AutoBreaker";
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFF8A33);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
