package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.gui.DrawContext;

public abstract class HudModule extends Module {
    protected HudModule(String name, String description) {
        super(name, description, Category.HUD, false);
    }

    /**
     * Draws this element's single line of text at the given top-left position.
     *
     * @return the height in pixels this element occupied, so callers can stack the next line below it.
     */
    public abstract int renderHud(DrawContext context, int x, int y);
}
