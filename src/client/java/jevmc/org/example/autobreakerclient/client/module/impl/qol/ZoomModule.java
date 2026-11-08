package jevmc.org.example.autobreakerclient.client.module.impl.qol;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Hold-to-zoom, ported from CoralMod (CC0-1.0, github.com/jirmjahu/coralmod)
 * onto this project's Yarn mappings: GameOptions#getFov()/getMouseSensitivity()
 * in place of CoralMod's Mojmap Options#fov()/sensitivity().
 */
public class ZoomModule extends Module {
    private final KeyBinding zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autobreakerclient.zoom",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KeyBinding.Category.MISC
    ));

    private boolean zooming;
    private int oldFov;
    private double oldSensitivity;

    public ZoomModule() {
        super("Zoom", "Hold the zoom key to zoom in.", Category.QOL, false);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        if (zooming) {
            stopZooming();
        }
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled()) {
            return;
        }
        boolean held = zoomKey.isPressed();
        if (held && !zooming) {
            startZooming();
        } else if (!held && zooming) {
            stopZooming();
        }
    }

    private void startZooming() {
        GameOptions options = MinecraftClient.getInstance().options;
        oldFov = options.getFov().getValue();
        oldSensitivity = options.getMouseSensitivity().getValue();

        options.getFov().setValue(30);
        options.getMouseSensitivity().setValue(Math.min(oldSensitivity, 0.2));
        zooming = true;
    }

    private void stopZooming() {
        GameOptions options = MinecraftClient.getInstance().options;
        options.getFov().setValue(oldFov);
        options.getMouseSensitivity().setValue(oldSensitivity);
        zooming = false;
    }
}
