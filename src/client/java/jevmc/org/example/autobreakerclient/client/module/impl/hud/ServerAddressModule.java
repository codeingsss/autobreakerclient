package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;

/**
 * Text-only port of CoralMod's Server Address HUD (CC0-1.0,
 * github.com/jirmjahu/coralmod) — the server favicon render is left out to
 * avoid pulling in dynamic texture registration/cleanup for a cosmetic extra.
 */
public class ServerAddressModule extends HudModule {
    public ServerAddressModule() {
        super("Server Address", "Shows the address of the server you're connected to.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text;
        if (client.isInSingleplayer()) {
            text = "Singleplayer";
        } else {
            ServerInfo server = client.getCurrentServerEntry();
            text = server == null ? "Unknown" : server.address;
        }
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
