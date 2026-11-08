package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Ported from CoralMod (CC0-1.0, github.com/jirmjahu/coralmod). */
public class ClockModule extends HudModule {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClockModule() {
        super("Clock", "Shows the current system time.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = LocalTime.now().format(FORMAT);
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
