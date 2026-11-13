package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.gui.DrawContext;

public abstract class HudModule extends Module {
    private static int nextDefaultSlot = 0;

    private int x;
    private int y;
    private int lastWidth = 40;
    private int lastHeight = 10;

    protected HudModule(String name, String description) {
        this(name, description, false);
    }

    protected HudModule(String name, String description, boolean defaultEnabled) {
        super(name, description, Category.HUD, defaultEnabled);
        this.x = 4;
        this.y = 4 + (nextDefaultSlot++) * 16;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getWidth() {
        return lastWidth;
    }

    public final int getHeight() {
        return lastHeight;
    }

    public final void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Draws this element and remembers the box it occupied, for the HUD editor's drag handles. */
    public final void renderAt(DrawContext context) {
        RenderResult result = renderHud(context, x, y);
        lastWidth = result.width();
        lastHeight = result.height();
    }

    /**
     * Draws this element's content with its top-left corner at (x, y).
     *
     * @return the pixel size this element occupied; return (0, 0) if nothing was drawn.
     */
    protected abstract RenderResult renderHud(DrawContext context, int x, int y);

    public record RenderResult(int width, int height) {
    }
}
