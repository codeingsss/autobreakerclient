package jevmc.org.example.autobreakerclient.client;

import jevmc.org.example.autobreakerclient.client.gui.ClickGuiScreen;
import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.HudModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutobreakerclientClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobreakerclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        HudElementRegistry.addLast(Identifier.of("autobreakerclient", "hud"), this::renderHud);
    }

    private void onEndTick(MinecraftClient client) {
        while (openGuiKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ClickGuiScreen());
            }
        }
    }

    private void renderHud(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof ClickGuiScreen) {
            return;
        }
        int x = 4;
        int y = 4;
        for (Module module : ModuleManager.getModules(Category.HUD)) {
            if (module.isEnabled() && module instanceof HudModule hudModule) {
                y += hudModule.renderHud(context, x, y);
            }
        }
    }
}
