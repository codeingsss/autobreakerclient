package jevmc.org.example.autobreakerclient.client.gui;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import jevmc.org.example.autobreakerclient.client.module.impl.hud.HudModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Drag-to-position editor for HUD elements. Every HUD module (even disabled
 * ones, so they can be placed in advance) is drawn at its stored position
 * with a box around it; dragging a box moves that module's saved (x, y).
 */
public class HudEditScreen extends Screen {
    private static final int BORDER = 2;
    private static final int COLOR_ENABLED = 0xA06FB8FF;
    private static final int COLOR_ENABLED_HOVER = 0xFF3C8CFF;
    private static final int COLOR_DISABLED = 0x80888888;

    private HudModule dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditScreen() {
        super(Text.literal("Edit HUD"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ModuleManager.saveConfig();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (Module module : ModuleManager.getModules(Category.HUD)) {
            if (!(module instanceof HudModule hudModule)) {
                continue;
            }
            hudModule.renderAt(context);
            boolean hovered = contains(hudModule, mouseX, mouseY);
            int color = hudModule.isEnabled() ? (hovered ? COLOR_ENABLED_HOVER : COLOR_ENABLED) : COLOR_DISABLED;
            drawOutline(context, hudModule.getX() - BORDER, hudModule.getY() - BORDER,
                    hudModule.getWidth() + BORDER * 2, hudModule.getHeight() + BORDER * 2, color);
        }

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Drag elements to position them. Press Escape when done."),
                this.width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) {
            return false;
        }
        for (Module module : ModuleManager.getModules(Category.HUD)) {
            if (module instanceof HudModule hudModule && contains(hudModule, click.x(), click.y())) {
                dragging = hudModule;
                dragOffsetX = (int) click.x() - hudModule.getX();
                dragOffsetY = (int) click.y() - hudModule.getY();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging == null) {
            return false;
        }
        dragging.setPosition((int) click.x() - dragOffsetX, (int) click.y() - dragOffsetY);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging == null) {
            return false;
        }
        dragging = null;
        ModuleManager.saveConfig();
        return true;
    }

    private static boolean contains(HudModule module, double px, double py) {
        int x = module.getX() - BORDER;
        int y = module.getY() - BORDER;
        int w = module.getWidth() + BORDER * 2;
        int h = module.getHeight() + BORDER * 2;
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private static void drawOutline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}
