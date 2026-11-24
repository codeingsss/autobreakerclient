package jevmc.org.example.autobreakerclient.client.gui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.StyleSpriteSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.ceil
import kotlin.math.max
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

    // Kept fairly bright on purpose: at GUI scale 1 the font is rasterised
    // around 9px with no hinting, and dim thin strokes drop out entirely -
    // "Quality of Life" came out as "QuLity of ..fe" at the old values.
    const val TEXT = 0xFFF0F1F5.toInt()
    const val TEXT_MUTED = 0xFFADB2BF.toInt()
    const val TEXT_FAINT = 0xFF8990A0.toInt()

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

    /** Keeps the colour but scales whatever alpha it already has. */
    fun fade(color: Int, factor: Double): Int {
        val alpha = ((color ushr 24) and 0xFF) * factor.coerceIn(0.0, 1.0)
        return (color and 0x00FFFFFF) or ((alpha.roundToInt().coerceIn(0, 255)) shl 24)
    }

    /**
     * Poppins, shipped in assets/autobreakerclient/font. Declared as real
     * Minecraft font providers so the game rasterises them and text metrics
     * stay correct, rather than us blitting glyphs ourselves.
     */
    val UI_FONT: Identifier = Identifier.of("autobreakerclient", "ui")
    val UI_FONT_MEDIUM: Identifier = Identifier.of("autobreakerclient", "ui_medium")

    // 1.21.11 takes a StyleSpriteSource rather than a bare Identifier.
    private val UI_STYLE: Style = Style.EMPTY.withFont(StyleSpriteSource.Font(UI_FONT))
    private val UI_MEDIUM_STYLE: Style = Style.EMPTY.withFont(StyleSpriteSource.Font(UI_FONT_MEDIUM))

    fun ui(text: String): Text = Text.literal(text).setStyle(UI_STYLE)

    fun uiMedium(text: String): Text = Text.literal(text).setStyle(UI_MEDIUM_STYLE)
}

/** Width of [text] in the custom UI font. */
fun TextRenderer.uiWidth(text: String): Int = getWidth(Theme.ui(text))

/**
 * Trims [text] to [maxWidth] in the custom UI font, adding an ellipsis when it
 * does not fit. The vanilla [TextRenderer.trimToWidth] measures in the default
 * font, so it would cut in the wrong place for ours.
 */
fun TextRenderer.uiTrim(text: String, maxWidth: Int, medium: Boolean = false): Text {
    val style: (String) -> Text = if (medium) Theme::uiMedium else Theme::ui
    if (getWidth(style(text)) <= maxWidth) return style(text)
    var kept = text
    while (kept.isNotEmpty() && getWidth(style("$kept...")) > maxWidth) {
        kept = kept.dropLast(1)
    }
    return style("$kept...")
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
        // Half-chord of the corner circle at this row, so the inset shrinks as
        // we move away from the edge. Keeping it fractional is what stops the
        // curve looking like stairs: the leftover fraction of a pixel is drawn
        // as a partly transparent sliver instead of being rounded away.
        val dy = r - i - 0.5
        val inset = r - sqrt(r * r - dy * dy)
        val solid = ceil(inset).toInt()
        fill(x + solid, y + i, x + width - solid, y + i + 1, color)
        fill(x + solid, y + height - i - 1, x + width - solid, y + height - i, color)

        val coverage = solid - inset
        if (coverage <= 0.02 || solid < 1) continue
        val edge = Theme.fade(color, coverage)
        val left = x + solid - 1
        val right = x + width - solid
        fill(left, y + i, left + 1, y + i + 1, edge)
        fill(right, y + i, right + 1, y + i + 1, edge)
        fill(left, y + height - i - 1, left + 1, y + height - i, edge)
        fill(right, y + height - i - 1, right + 1, y + height - i, edge)
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
        val half = sqrt(max(0.0, radius * radius - dy * dy))
        val solid = half.toInt()
        if (solid > 0) {
            fill(centerX - solid, centerY + i, centerX + solid, centerY + i + 1, color)
        }
        // Same trick as the rounded corners: the leftover fraction becomes a
        // translucent edge pixel rather than a hard step.
        val coverage = half - solid
        if (coverage <= 0.02) continue
        val edge = Theme.fade(color, coverage)
        fill(centerX - solid - 1, centerY + i, centerX - solid, centerY + i + 1, edge)
        fill(centerX + solid, centerY + i, centerX + solid + 1, centerY + i + 1, edge)
    }
}
