package jevmc.org.example.autobreakerclient.client.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Draws filled rectangles with rounded corners using nothing but plain solid fills,
 * so it has zero dependency on custom shaders or textures.
 */
public final class RoundedRect {
    private RoundedRect() {
    }

    public static void fill(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (radius == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // Straight edges: a vertical band through the middle and two side strips.
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Four rounded corners, one horizontal strip per row using a circle equation.
        for (int row = 0; row < radius; row++) {
            int dy = radius - row;
            int maxDx = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            int filledWidth = Math.min(radius, maxDx + 1);

            context.fill(x + radius - filledWidth, y + row, x + radius, y + row + 1, color);
            context.fill(x + width - radius, y + row, x + width - radius + filledWidth, y + row + 1, color);

            int bottomRow = y + height - 1 - row;
            context.fill(x + radius - filledWidth, bottomRow, x + radius, bottomRow + 1, color);
            context.fill(x + width - radius, bottomRow, x + width - radius + filledWidth, bottomRow + 1, color);
        }
    }
}
