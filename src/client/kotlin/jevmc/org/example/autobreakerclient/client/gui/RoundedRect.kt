package jevmc.org.example.autobreakerclient.client.gui

import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Draws filled rectangles with rounded corners using nothing but plain solid
 * fills, so it has zero dependency on custom shaders or textures.
 */
object RoundedRect {
    @JvmStatic
    fun fill(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        val r = radius.coerceAtMost(min(width, height) / 2).coerceAtLeast(0)
        if (r == 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }

        // Straight edges: a vertical band through the middle and two side strips.
        context.fill(x + r, y, x + width - r, y + height, color)
        context.fill(x, y + r, x + r, y + height - r, color)
        context.fill(x + width - r, y + r, x + width, y + height - r, color)

        // Four rounded corners, one horizontal strip per row using a circle equation.
        for (row in 0 until r) {
            val dy = r - row
            val maxDx = sqrt(max(0, r * r - dy * dy).toDouble()).toInt()
            val filledWidth = min(r, maxDx + 1)

            context.fill(x + r - filledWidth, y + row, x + r, y + row + 1, color)
            context.fill(x + width - r, y + row, x + width - r + filledWidth, y + row + 1, color)

            val bottomRow = y + height - 1 - row
            context.fill(x + r - filledWidth, bottomRow, x + r, bottomRow + 1, color)
            context.fill(x + width - r, bottomRow, x + width - r + filledWidth, bottomRow + 1, color)
        }
    }
}
