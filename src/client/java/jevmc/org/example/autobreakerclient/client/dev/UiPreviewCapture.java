package jevmc.org.example.autobreakerclient.client.dev;

import jevmc.org.example.autobreakerclient.client.gui.ModMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;

/**
 * Opens the mod menu on its own shortly after launch, writes a screenshot of
 * it, and quits - so a menu change can actually be looked at instead of being
 * declared finished because it compiled.
 *
 * Inert unless this is a Loom dev run AND {@code -Dautobreaker.uiPreview=true}
 * is set, so it can never fire for a player. Run it with:
 * {@code ./gradlew runClient -PuiPreview}
 */
public final class UiPreviewCapture {
    private static final String PROPERTY = "autobreaker.uiPreview";
    private static final String FILE_NAME = "autobreaker-menu.png";

    // Client ticks already run behind the Mojang splash, so these count from
    // the title screen actually appearing rather than from launch.
    private static final int OPEN_AFTER_TITLE = 20;
    private static final int SHOOT_AFTER_TITLE = 40;
    private static final int STOP_AFTER_TITLE = 55;

    private static int ticks = Integer.MIN_VALUE;

    private UiPreviewCapture() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment() || !Boolean.getBoolean(PROPERTY)) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(UiPreviewCapture::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticks == Integer.MIN_VALUE) {
            // The splash overlay keeps covering the screen after currentScreen
            // is already the title screen, so wait for it to be gone too -
            // otherwise the framebuffer we capture is still the Mojang splash.
            if (!(client.currentScreen instanceof TitleScreen) || client.getOverlay() != null) {
                return;
            }
            ticks = 0;
        }
        ticks++;
        if (ticks == OPEN_AFTER_TITLE) {
            // Expanded, so the screenshot shows the settings rows too.
            client.setScreen(new ModMenuScreen(true));
        } else if (ticks == SHOOT_AFTER_TITLE) {
            // The framebuffer still holds the last rendered frame, which is the
            // menu we opened 20 ticks ago.
            ScreenshotRecorder.saveScreenshot(
                    client.runDirectory, FILE_NAME, client.getFramebuffer(), 1, message -> {
                    });
        } else if (ticks == STOP_AFTER_TITLE) {
            client.scheduleStop();
        }
    }
}
