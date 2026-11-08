package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clicks-per-second counter. Polling the mouse button state once per client
 * tick (20 Hz) misses fast clicks entirely — a single press-release cycle
 * from drag-clicking or butterfly-clicking is often well under the 50ms tick
 * window, so it can fall entirely between two polls. Hooking GLFW's own
 * mouse button callback instead sees every physical click as it happens.
 *
 * <p>GLFW allows only one callback per window per event type, and Minecraft's
 * {@code Mouse} class has already installed its own by the time
 * {@link ClientLifecycleEvents#CLIENT_STARTED} fires, so this captures that
 * existing callback and always re-invokes it after recording a click — vanilla
 * mouse input (combat, UI clicks, block breaking) keeps working unmodified.</p>
 */
public class CpsModule extends HudModule {
    private final List<Long> leftClicks = new CopyOnWriteArrayList<>();
    private final List<Long> rightClicks = new CopyOnWriteArrayList<>();

    public CpsModule() {
        super("CPS", "Shows left/right click clicks-per-second.");
        ClientLifecycleEvents.CLIENT_STARTED.register(this::hookMouse);
    }

    private void hookMouse(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        GLFWMouseButtonCallback previous = GLFW.glfwSetMouseButtonCallback(handle, (window, button, action, mods) -> {
            try {
                if (action == GLFW.GLFW_PRESS) {
                    long now = System.currentTimeMillis();
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        leftClicks.add(now);
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        rightClicks.add(now);
                    }
                }
            } finally {
                if (previous != null) {
                    previous.invoke(window, button, action, mods);
                }
            }
        });
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        long now = System.currentTimeMillis();
        leftClicks.removeIf(time -> now - time > 1000);
        rightClicks.removeIf(time -> now - time > 1000);

        MinecraftClient client = MinecraftClient.getInstance();
        String text = "CPS: " + leftClicks.size() + " | " + rightClicks.size();
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }
}
