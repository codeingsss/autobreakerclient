package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Heap usage readout. Useful next to the FPS counter when diagnosing the
 * stutter that comes from garbage collection rather than from render load.
 */
public class MemoryModule extends HudModule {
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final int HEALTHY = 0xFFAAAAAA;
    private static final int STRAINED = 0xFFE0A33A;
    private static final int CRITICAL = 0xFFE0736B;

    public MemoryModule() {
        super("Memory", "Shows how much of the allocated heap is in use.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / BYTES_PER_MEGABYTE;
        long used = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE;
        int percent = max <= 0 ? 0 : (int) (used * 100 / max);

        int color = HEALTHY;
        if (percent >= 90) {
            color = CRITICAL;
        } else if (percent >= 75) {
            color = STRAINED;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String text = "Mem: " + used + "/" + max + "MB (" + percent + "%)";
        context.drawTextWithShadow(client.textRenderer, text, x, y, color);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
