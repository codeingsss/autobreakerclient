package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Clicks-per-second counter. CoralMod (CC0-1.0, github.com/jirmjahu/coralmod)
 * detects clicks through a custom mixin-based mouse event; this polls GLFW's
 * button state once per client tick instead, so no mixin is needed.
 */
public class CpsModule extends HudModule {
    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();
    private boolean leftWasDown;
    private boolean rightWasDown;

    public CpsModule() {
        super("CPS", "Shows left/right click clicks-per-second.");
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();

        if (leftDown && !leftWasDown) {
            leftClicks.add(now);
        }
        if (rightDown && !rightWasDown) {
            rightClicks.add(now);
        }
        leftWasDown = leftDown;
        rightWasDown = rightDown;

        leftClicks.removeIf(time -> now - time > 1000);
        rightClicks.removeIf(time -> now - time > 1000);
    }

    @Override
    public int renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = "CPS: " + leftClicks.size() + " | " + rightClicks.size();
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFAAAAAA);
        return client.textRenderer.fontHeight + 2;
    }
}
