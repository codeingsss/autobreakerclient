package jevmc.org.example.autobreakerclient.client;

import jevmc.org.example.autobreakerclient.client.config.ConfigShareManager;
import jevmc.org.example.autobreakerclient.client.gui.ClickGuiScreen;
import jevmc.org.example.autobreakerclient.client.gui.HudEditScreen;
import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.HudModule;
import jevmc.org.example.autobreakerclient.client.profile.ServerProfileManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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

        // Deferred: enabling a module from saved config can touch
        // MinecraftClient.getInstance().options, which is still null here.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModuleManager.loadConfig());

        ServerProfileManager.init();
        ConfigShareManager.init();

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
        if (screen instanceof ClickGuiScreen || screen instanceof HudEditScreen) {
            return;
        }
        for (Module module : ModuleManager.getModules(Category.HUD)) {
            if (module.isEnabled() && module instanceof HudModule hudModule) {
                hudModule.renderAt(context);
            }
        }
    }
}
