package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/** Ported from CoralMod (CC0-1.0, github.com/jirmjahu/coralmod). */
public class PingModule extends HudModule {
    public PingModule() {
        super("Ping", "Shows your connection latency in milliseconds.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (client.player == null || handler == null) {
            return new RenderResult(0, 0);
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        if (entry == null) {
            return new RenderResult(0, 0);
        }
        String text = entry.getLatency() + " ms";
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
