package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

/**
 * WASD and mouse-button display, the staple of EvergreenHUD-style HUDs.
 *
 * <p>Keys are read from the player's own bindings rather than hardcoded
 * scancodes, so a non-QWERTY or rebound layout still lights up the right
 * box, and the letter shown is whatever that binding is actually bound to.</p>
 */
public class KeystrokesModule extends HudModule {
    private static final int BOX = 16;
    private static final int GAP = 2;
    private static final int PRESSED_BACKGROUND = 0xC0FF8A33;
    private static final int RELEASED_BACKGROUND = 0x80101216;
    private static final int PRESSED_TEXT = 0xFF14161B;
    private static final int RELEASED_TEXT = 0xFFD8DAE2;

    public KeystrokesModule() {
        super("Keystrokes", "Shows WASD and mouse buttons as they are pressed.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) {
            return new RenderResult(0, 0);
        }

        int rowTwo = y + BOX + GAP;
        int rowThree = rowTwo + BOX + GAP;
        int width = BOX * 3 + GAP * 2;

        key(context, client.options.forwardKey, x + BOX + GAP, y);
        key(context, client.options.leftKey, x, rowTwo);
        key(context, client.options.backKey, x + BOX + GAP, rowTwo);
        key(context, client.options.rightKey, x + BOX * 2 + GAP * 2, rowTwo);

        int halfWidth = (width - GAP) / 2;
        button(context, "LMB", client.options.attackKey.isPressed(), x, rowThree, halfWidth);
        button(context, "RMB", client.options.useKey.isPressed(), x + halfWidth + GAP, rowThree,
                width - halfWidth - GAP);

        return new RenderResult(width, rowThree + BOX - y);
    }

    private void key(DrawContext context, KeyBinding binding, int x, int y) {
        String label = binding.getBoundKeyLocalizedText().getString().toUpperCase();
        button(context, label, binding.isPressed(), x, y, BOX);
    }

    private void button(DrawContext context, String label, boolean pressed, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.fill(x, y, x + width, y + BOX, pressed ? PRESSED_BACKGROUND : RELEASED_BACKGROUND);
        String shown = client.textRenderer.trimToWidth(label, width - 2);
        int textX = x + (width - client.textRenderer.getWidth(shown)) / 2;
        int textY = y + (BOX - client.textRenderer.fontHeight) / 2 + 1;
        context.drawText(client.textRenderer, shown, textX, textY, pressed ? PRESSED_TEXT : RELEASED_TEXT, false);
    }
}
