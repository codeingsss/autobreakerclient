package jevmc.org.example.autobreakerclient.client.gui

import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Colours and drawing primitives for AutoBreaker's menu.
 *
 * Vanilla only gives us axis-aligned [DrawContext.fill], so the rounded
 * surfaces this menu is built out of are composed here once and reused, rather
 * than every screen re-inventing them.
 */
object Theme {
    // Dark neutral surfaces so the orange accent is the only saturated thing
    // on screen - a wash of orange over everything was already rejected.
    const val SCRIM = 0xB4000000.toInt()
    const val WINDOW = 0xFF14161B.toInt()
    const val WINDOW_BORDER = 0xFF2A2E38.toInt()
    const val HEADER = 0xFF191C22.toInt()
    const val SIDEBAR = 0xFF171A20.toInt()

    const val CARD = 0xFF1E222A.toInt()
    const val CARD_HOVER = 0xFF262B35.toInt()
    const val FIELD = 0xFF101216.toInt()
    const val TRACK = 0xFF333945.toInt()

    const val ACCENT = 0xFFFF8A33.toInt()
    const val ACCENT_DIM = 0x40FF8A33
    const val ACCENT_TEXT = 0xFFFFA866.toInt()

    const val TEXT = 0xFFE9EAEE.toInt()
    const val TEXT_MUTED = 0xFF8A8F9C.toInt()
    const val TEXT_FAINT = 0xFF5F6472.toInt()

    /** Linear blend between two ARGB colours; [t] is clamped to 0..1. */
    fun mix(from: Int, to: Int, t: Double): Int {
        val f = t.coerceIn(0.0, 1.0)
        var out = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            out = out or (((a + (b - a) * f).roundToInt() and 0xFF) shl shift)
        }
        return out
    }

    fun withAlpha(color: Int, alpha: Double): Int =
        (color and 0x00FFFFFF) or (((alpha.coerceIn(0.0, 1.0) * 255).roundToInt() and 0xFF) shl 24)
}

/**
 * Filled rectangle with rounded corners. Each corner row is inset by the
 * horizontal distance from the circle of the given radius, which reads as a
 * clean curve at the small radii this menu uses.
 */
fun DrawContext.roundRect(x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
    if (width <= 0 || height <= 0) return
    val r = radius.coerceIn(0, minOf(width, height) / 2)
    if (r == 0) {
        fill(x, y, x + width, y + height, color)
        return
    }
    fill(x, y + r, x + width, y + height - r, color)
    for (i in 0 until r) {
        // Half-chord of the corner circle at this row, so the inset shrinks
        // as we move away from the very edge.
        val dy = r - i - 0.5
        val inset = (r - sqrt(r * r - dy * dy)).toInt()
        fill(x + inset, y + i, x + width - inset, y + i + 1, color)
        fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color)
    }
}

/** Rounded outline, drawn as a rounded fill with a smaller one punched out. */
fun DrawContext.roundOutline(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    radius: Int,
    thickness: Int,
    color: Int,
    fillColor: Int,
) {
    roundRect(x, y, width, height, radius, color)
    roundRect(
        x + thickness,
        y + thickness,
        width - thickness * 2,
        height - thickness * 2,
        (radius - thickness).coerceAtLeast(0),
        fillColor,
    )
}

/** A filled circle, used for slider and toggle knobs. */
fun DrawContext.circle(centerX: Int, centerY: Int, radius: Int, color: Int) {
    if (radius <= 0) return
    for (i in -radius until radius) {
        val dy = i + 0.5
        val half = sqrt(radius * radius - dy * dy).toInt()
        if (half <= 0) continue
        fill(centerX - half, centerY + i, centerX + half, centerY + i + 1, color)
    }
}
