package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

/**
 * A live ping graph instead of a single number: samples latency once a
 * second into a small ring buffer, renders it as a bar sparkline color-coded
 * by severity, and summarizes both the average (how high) and the jitter -
 * standard deviation of recent samples (how unstable) - as a one-line
 * verdict, since a steady 120ms and a connection swinging between 40ms and
 * 300ms look identical as a single rolling number but play very differently.
 * Tags the reading "(Hypixel)" when the current server address is one, since
 * its own ping thresholds for "fine" skew higher than a typical SMP.
 */
public class NetworkDoctorModule extends HudModule {
    private static final int SAMPLE_COUNT = 40;
    private static final int GRAPH_WIDTH = 80;
    private static final int GRAPH_HEIGHT = 20;
    private static final int GRAPH_MAX_MS = 300;

    private final int[] samples = new int[SAMPLE_COUNT];
    private int sampleIndex;
    private int filled;
    private int tickCounter;

    public NetworkDoctorModule() {
        super("Network Doctor", "Live ping graph with a stability verdict, not just a number.");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled()) {
                return;
            }
            if (++tickCounter < 20) {
                return;
            }
            tickCounter = 0;
            Integer ping = currentPing(client);
            if (ping == null) {
                return;
            }
            samples[sampleIndex] = ping;
            sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT;
            if (filled < SAMPLE_COUNT) {
                filled++;
            }
        });
    }

    private static Integer currentPing(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (client.player == null || handler == null) {
            return null;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        return entry == null ? null : entry.getLatency();
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (filled == 0) {
            String text = "Network Doctor: gathering data...";
            context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
            return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
        }

        double sum = 0;
        int max = 0;
        for (int i = 0; i < filled; i++) {
            sum += samples[i];
            max = Math.max(max, samples[i]);
        }
        double average = sum / filled;
        double variance = 0;
        for (int i = 0; i < filled; i++) {
            double diff = samples[i] - average;
            variance += diff * diff;
        }
        double jitter = Math.sqrt(variance / filled);

        boolean hypixel = isHypixel(client);
        int goodCeiling = hypixel ? 110 : 60;
        int okCeiling = hypixel ? 200 : 120;
        String quality = average < goodCeiling ? "Excellent" : average < okCeiling ? "Good" : "Poor";
        String stability = jitter < 15 ? "Stable" : jitter < 40 ? "Jittery" : "Unstable";

        String headline = String.format("Network: %s, %s%s", quality, stability, hypixel ? " (Hypixel)" : "");
        context.drawTextWithShadow(client.textRenderer, headline, x, y, 0xFFFFFFFF);
        int textHeight = client.textRenderer.fontHeight;

        String detail = String.format("%.0fms avg, %.0fms jitter", average, jitter);
        context.drawTextWithShadow(client.textRenderer, detail, x, y + textHeight + 1, 0xFFAAAAAA);

        int graphY = y + (textHeight + 1) * 2 + 2;
        context.fill(x, graphY, x + GRAPH_WIDTH, graphY + GRAPH_HEIGHT, 0x60000000);
        int barWidth = Math.max(1, GRAPH_WIDTH / SAMPLE_COUNT);
        for (int i = 0; i < filled; i++) {
            int sampleAt = (sampleIndex - filled + i + SAMPLE_COUNT * 2) % SAMPLE_COUNT;
            int value = Math.min(samples[sampleAt], GRAPH_MAX_MS);
            int barHeight = Math.max(1, value * GRAPH_HEIGHT / GRAPH_MAX_MS);
            int barX = x + i * barWidth;
            int color = value < goodCeiling ? 0xFF55D65A : value < okCeiling ? 0xFFE6C13C : 0xFFE6543C;
            context.fill(barX, graphY + GRAPH_HEIGHT - barHeight, barX + Math.max(1, barWidth - 1), graphY + GRAPH_HEIGHT, color);
        }

        int totalHeight = (textHeight + 1) * 2 + 2 + GRAPH_HEIGHT;
        return new RenderResult(GRAPH_WIDTH, totalHeight);
    }

    private static boolean isHypixel(MinecraftClient client) {
        if (client.isInSingleplayer()) {
            return false;
        }
        ServerInfo server = client.getCurrentServerEntry();
        return server != null && server.address != null && server.address.toLowerCase().contains("hypixel.net");
    }
}
