package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

public class CoordinatesModule extends HudModule {
    public CoordinatesModule() {
        super("Coordinates", "Shows your current XYZ position.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return new RenderResult(0, 0);
        }
        String text = String.format("%.1f / %.1f / %.1f", player.getX(), player.getY(), player.getZ());
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
