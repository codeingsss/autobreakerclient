package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FpsCounterModule extends HudModule {
    public FpsCounterModule() {
        super("FPS Counter", "Shows the current frames per second.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = client.getCurrentFps() + " fps";
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
