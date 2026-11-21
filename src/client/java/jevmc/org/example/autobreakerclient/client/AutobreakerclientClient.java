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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutobreakerclientClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    /**
     * Set by {@code /autobreaker gui} and consumed on the next client tick -
     * the command body runs while the chat screen is still being torn down,
     * so opening a screen there would just get replaced when chat closes.
     * The tick handler below already opens the GUI correctly, so reuse it.
     */
    private static volatile boolean guiOpenRequested;

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

        // Right Shift can be taken by another mod (or just rebound), which
        // leaves no way into the menu at all, so register a command that
        // opens exactly the same screen.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("autobreaker")
                        .then(ClientCommandManager.literal("gui")
                                .executes(ctx -> {
                                    guiOpenRequested = true;
                                    return 1;
                                }))));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player == null) {
                return;
            }
            client.player.sendMessage(Text.literal(
                    "AutoBreaker ready - press Right Shift or run /autobreaker gui to open the menu."), false);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        HudElementRegistry.addLast(Identifier.of("autobreakerclient", "hud"), this::renderHud);
    }

    private void onEndTick(MinecraftClient client) {
        boolean openByKey = false;
        while (openGuiKey.wasPressed()) {
            openByKey = true;
        }
        boolean openByCommand = guiOpenRequested;
        guiOpenRequested = false;

        if ((openByKey || openByCommand) && client.currentScreen == null) {
            client.setScreen(new ClickGuiScreen());
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
