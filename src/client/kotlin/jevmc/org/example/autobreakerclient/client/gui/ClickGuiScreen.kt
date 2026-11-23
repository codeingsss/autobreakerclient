package jevmc.org.example.autobreakerclient.client.gui

import jevmc.org.example.autobreakerclient.client.module.Category
import jevmc.org.example.autobreakerclient.client.module.Module
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.roundToInt

private const val WINDOW_WIDTH = 480
private const val WINDOW_HEIGHT = 404
private const val HEADER_HEIGHT = 34
private const val SIDEBAR_WIDTH = 142
private const val FOOTER_HEIGHT = 26
private const val TAB_HEIGHT = 28
private const val TAB_SPACING = 2
private const val CONTENT_PADDING = 12
private const val ROW_HEIGHT = 24
private const val ROW_SPACING = 4
private const val SLIDER_HEIGHT = 16
private const val SLIDER_INDENT = 14
private const val CLOSE_BUTTON_SIZE = 18
private const val SCROLL_STEP = 18

private const val TOGGLE_WIDTH = 26
private const val TOGGLE_HEIGHT = 13
private const val ACCENT_BAR_WIDTH = 3

// Neutral near-black surfaces; the orange is reserved for state, so it reads as
// an accent instead of tinting the whole window.
private const val COLOR_OUTLINE = 0xFF2C2C35.toInt()
private const val COLOR_HEADER_BG = 0xF21B1B21.toInt()
private const val COLOR_SIDEBAR_BG = 0xF2131317.toInt()
private const val COLOR_CONTENT_BG = 0xF218181D.toInt()
private const val COLOR_FOOTER_BG = 0xF2141419.toInt()

private const val ACCENT = 0xFFFF8A33.toInt()
private const val ACCENT_SOFT = 0xFFFFB366.toInt()
private const val ACCENT_FAINT = 0x26FF8A33

private const val TEXT_PRIMARY = 0xFFF0F0F3.toInt()
private const val TEXT_DIM = 0xFF8C8C98.toInt()
private const val TEXT_FAINT = 0xFF63636E.toInt()

private const val COLOR_TAB_HOVER = 0x14FFFFFF
private const val COLOR_TAB_SELECTED = 0x1FFFFFFF
private const val COLOR_ROW_BG = 0xFF1F1F26.toInt()
private const val COLOR_ROW_HOVER = 0xFF272730.toInt()
private const val COLOR_TOGGLE_OFF = 0xFF3C3C47.toInt()
private const val COLOR_TOGGLE_THUMB = 0xFFF2F2F5.toInt()
private const val COLOR_SLIDER_TRACK = 0xFF15151A.toInt()
private const val COLOR_SLIDER_FILL = 0x59FF8A33
private const val COLOR_CLOSE_HOVER = 0xFFE05A4A.toInt()
private const val COLOR_SCROLLBAR = 0x40FFFFFF

private data class TabHit(val category: Category, val x: Int, val y: Int, val w: Int, val h: Int)
private data class ButtonHit(val x: Int, val y: Int, val w: Int, val h: Int)
private data class RowHit(val module: Module, val x: Int, val y: Int, val w: Int, val h: Int)
private data class SliderHit(val setting: IntSetting, val x: Int, val y: Int, val w: Int, val h: Int)

/**
 * A fixed, centered settings window: category sidebar on the left, scrollable
 * module list on the right, and a footer that explains whatever is hovered.
 * Backdrop blur comes from the vanilla [DrawContext.applyBlur] post effect and
 * every panel is a plain solid fill via [RoundedRect] - no shaders, textures
 * or extra rendering libraries.
 */
class ClickGuiScreen : Screen(Text.literal("AutoBreaker")) {
    private val tabHits = mutableListOf<TabHit>()
    private val moduleRows = mutableListOf<RowHit>()
    private val sliderRows = mutableListOf<SliderHit>()
    private var editHudButton: ButtonHit? = null

    private var panelX = 0
    private var panelY = 0
    private var positioned = false
    private var selectedCategory: Category = Category.values()[0]
    private var scrollOffset = 0
    private var draggingSlider: IntSetting? = null
    private var hoveredDescription: String? = null

    override fun shouldPause() = false

    override fun close() {
        ModuleManager.saveConfig()
        super.close()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!positioned) {
            panelX = (width - WINDOW_WIDTH) / 2
            panelY = (height - WINDOW_HEIGHT) / 2
            positioned = true
        }
        hoveredDescription = null
        tryApplyBlur(context)

        val contentX = panelX + SIDEBAR_WIDTH
        val contentTop = panelY + HEADER_HEIGHT
        val footerTop = panelY + WINDOW_HEIGHT - FOOTER_HEIGHT
        val contentWidth = WINDOW_WIDTH - SIDEBAR_WIDTH
        val bodyHeight = WINDOW_HEIGHT - HEADER_HEIGHT

        // One subtle outline behind everything instead of a thick orange frame.
        RoundedRect.fill(context, panelX - 1, panelY - 1, WINDOW_WIDTH + 2, WINDOW_HEIGHT + 2, 9, COLOR_OUTLINE)
        RoundedRect.fill(context, panelX, panelY, WINDOW_WIDTH, HEADER_HEIGHT, 8, COLOR_HEADER_BG)
        RoundedRect.fill(context, panelX, contentTop, SIDEBAR_WIDTH, bodyHeight, 8, COLOR_SIDEBAR_BG)
        RoundedRect.fill(context, contentX, contentTop, contentWidth, bodyHeight, 8, COLOR_CONTENT_BG)
        RoundedRect.fill(context, contentX, footerTop, contentWidth, FOOTER_HEIGHT, 8, COLOR_FOOTER_BG)
        // Hairline of accent under the header - the only always-on orange.
        context.fill(panelX + 1, contentTop - 1, panelX + WINDOW_WIDTH - 1, contentTop, ACCENT)

        drawHeader(context, mouseX, mouseY)

        layoutTabs()
        for (tab in tabHits) {
            val selected = tab.category == selectedCategory
            val hovered = contains(tab.x, tab.y, tab.w, tab.h, mouseX.toDouble(), mouseY.toDouble())
            if (selected || hovered) {
                RoundedRect.fill(
                    context, tab.x, tab.y, tab.w, tab.h, 5,
                    if (selected) COLOR_TAB_SELECTED else COLOR_TAB_HOVER
                )
            }
            if (selected) {
                RoundedRect.fill(context, tab.x, tab.y + 5, ACCENT_BAR_WIDTH, tab.h - 10, 1, ACCENT)
            }
            context.drawTextWithShadow(
                textRenderer, tab.category.displayName, tab.x + 14,
                tab.y + (tab.h - textRenderer.fontHeight) / 2,
                if (selected) ACCENT_SOFT else TEXT_DIM
            )
        }

        val visibleHeight = bodyHeight - FOOTER_HEIGHT - CONTENT_PADDING
        val naturalHeight = layoutModules(contentX, contentTop, contentWidth, 0)
        val maxScroll = max(0, naturalHeight - visibleHeight)
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)

        moduleRows.clear()
        sliderRows.clear()
        layoutModules(contentX, contentTop, contentWidth, scrollOffset)

        context.enableScissor(contentX, contentTop, contentX + contentWidth, footerTop)
        drawContent(context, mouseX, mouseY, contentTop, footerTop)
        context.disableScissor()

        if (maxScroll > 0) {
            val trackHeight = footerTop - contentTop - CONTENT_PADDING
            val thumbHeight = max(16, trackHeight * visibleHeight / naturalHeight)
            val thumbY = contentTop + CONTENT_PADDING / 2 + (trackHeight - thumbHeight) * scrollOffset / maxScroll
            RoundedRect.fill(context, contentX + contentWidth - 5, thumbY, 3, thumbHeight, 1, COLOR_SCROLLBAR)
        }

        val footerText = hoveredDescription ?: "Right Shift to toggle this menu"
        context.drawTextWithShadow(
            textRenderer, trimToWidth(footerText, contentWidth - CONTENT_PADDING * 2),
            contentX + CONTENT_PADDING, footerTop + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            if (hoveredDescription != null) TEXT_DIM else TEXT_FAINT
        )
    }

    private fun drawHeader(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawTextWithShadow(
            textRenderer, "AutoBreaker", panelX + CONTENT_PADDING + 2,
            panelY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, ACCENT
        )
        val enabled = ModuleManager.getModules().count { it.isEnabled }
        context.drawTextWithShadow(
            textRenderer, "$enabled active",
            panelX + CONTENT_PADDING + 4 + textRenderer.getWidth("AutoBreaker") + 10,
            panelY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, TEXT_FAINT
        )

        val closeX = panelX + WINDOW_WIDTH - CONTENT_PADDING - CLOSE_BUTTON_SIZE
        val closeY = panelY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2
        val closeHovered = contains(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, mouseX.toDouble(), mouseY.toDouble())
        if (closeHovered) {
            RoundedRect.fill(context, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, 4, COLOR_CLOSE_HOVER)
        }
        context.drawCenteredTextWithShadow(
            textRenderer, Text.literal("✕"), closeX + CLOSE_BUTTON_SIZE / 2,
            closeY + (CLOSE_BUTTON_SIZE - textRenderer.fontHeight) / 2,
            if (closeHovered) TEXT_PRIMARY else TEXT_DIM
        )
    }

    private fun drawContent(context: DrawContext, mouseX: Int, mouseY: Int, contentTop: Int, footerTop: Int) {
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        fun inView(y: Double) = y >= contentTop && y <= footerTop

        editHudButton?.let { button ->
            val hovered = contains(button.x, button.y, button.w, button.h, mx, my) && inView(my)
            RoundedRect.fill(context, button.x, button.y, button.w, button.h, 5, if (hovered) COLOR_ROW_HOVER else COLOR_ROW_BG)
            context.drawTextWithShadow(
                textRenderer, "Edit HUD Layout", button.x + 10,
                button.y + (button.h - textRenderer.fontHeight) / 2, ACCENT_SOFT
            )
            context.drawTextWithShadow(
                textRenderer, "›", button.x + button.w - 14,
                button.y + (button.h - textRenderer.fontHeight) / 2, TEXT_DIM
            )
            if (hovered) hoveredDescription = "Drag each HUD element anywhere on screen"
        }

        for (hit in moduleRows) {
            val enabled = hit.module.isEnabled
            val hovered = contains(hit.x, hit.y, hit.w, hit.h, mx, my) && inView(my)
            if (hovered) hoveredDescription = hit.module.description

            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 5, if (hovered) COLOR_ROW_HOVER else COLOR_ROW_BG)
            if (enabled) {
                RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 5, ACCENT_FAINT)
                RoundedRect.fill(context, hit.x, hit.y + 4, ACCENT_BAR_WIDTH, hit.h - 8, 1, ACCENT)
            }
            context.drawTextWithShadow(
                textRenderer, hit.module.name, hit.x + 12,
                hit.y + (hit.h - textRenderer.fontHeight) / 2,
                if (enabled) TEXT_PRIMARY else TEXT_DIM
            )
            drawToggle(context, hit.x + hit.w - TOGGLE_WIDTH - 10, hit.y + (hit.h - TOGGLE_HEIGHT) / 2, enabled)
        }

        for (hit in sliderRows) {
            val hovered = contains(hit.x, hit.y, hit.w, hit.h, mx, my) && inView(my)
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 4, COLOR_SLIDER_TRACK)

            val fillWidth = (hit.w * hit.setting.fraction).roundToInt().coerceAtLeast(0)
            if (fillWidth > 2) {
                RoundedRect.fill(context, hit.x, hit.y, fillWidth, hit.h, 4, COLOR_SLIDER_FILL)
                RoundedRect.fill(context, hit.x + fillWidth - 2, hit.y, 2, hit.h, 1, ACCENT)
            }
            context.drawTextWithShadow(
                textRenderer, hit.setting.name, hit.x + 8,
                hit.y + (hit.h - textRenderer.fontHeight) / 2, if (hovered) TEXT_PRIMARY else TEXT_DIM
            )
            val value = hit.setting.value.toString()
            context.drawTextWithShadow(
                textRenderer, value, hit.x + hit.w - textRenderer.getWidth(value) - 8,
                hit.y + (hit.h - textRenderer.fontHeight) / 2, ACCENT_SOFT
            )
            if (hovered) hoveredDescription = "Drag to change ${hit.setting.name.lowercase()}"
        }
    }

    private fun layoutTabs() {
        tabHits.clear()
        var y = panelY + HEADER_HEIGHT + 8
        val x = panelX + 6
        val w = SIDEBAR_WIDTH - 12
        for (category in Category.values()) {
            if (ModuleManager.getModules(category).isEmpty()) {
                continue
            }
            tabHits += TabHit(category, x, y, w, TAB_HEIGHT)
            y += TAB_HEIGHT + TAB_SPACING
        }
    }

    private fun layoutModules(contentX: Int, contentTop: Int, contentWidth: Int, scrollOffsetPx: Int): Int {
        val rowX = contentX + CONTENT_PADDING
        val rowWidth = contentWidth - CONTENT_PADDING * 2 - 4
        var y = contentTop + CONTENT_PADDING - scrollOffsetPx

        editHudButton = null
        if (selectedCategory == Category.HUD) {
            editHudButton = ButtonHit(rowX, y, rowWidth, ROW_HEIGHT)
            y += ROW_HEIGHT + ROW_SPACING * 2
        }

        for (module in ModuleManager.getModules(selectedCategory)) {
            moduleRows += RowHit(module, rowX, y, rowWidth, ROW_HEIGHT)
            y += ROW_HEIGHT + ROW_SPACING
            if (module.isEnabled) {
                for (setting in module.settings) {
                    if (setting is IntSetting) {
                        // Indented so it reads as belonging to the row above.
                        sliderRows += SliderHit(setting, rowX + SLIDER_INDENT, y, rowWidth - SLIDER_INDENT, SLIDER_HEIGHT)
                        y += SLIDER_HEIGHT + ROW_SPACING
                    }
                }
            }
        }
        return y - contentTop + scrollOffsetPx + CONTENT_PADDING
    }

    override fun mouseClicked(click: Click, doubleClick: Boolean): Boolean {
        if (click.button() != 0) {
            return false
        }
        val mx = click.x()
        val my = click.y()

        val closeX = panelX + WINDOW_WIDTH - CONTENT_PADDING - CLOSE_BUTTON_SIZE
        val closeY = panelY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2
        if (contains(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, mx, my)) {
            close()
            return true
        }

        for (tab in tabHits) {
            if (contains(tab.x, tab.y, tab.w, tab.h, mx, my)) {
                if (selectedCategory != tab.category) {
                    selectedCategory = tab.category
                    scrollOffset = 0
                }
                return true
            }
        }

        val contentTop = panelY + HEADER_HEIGHT
        val footerTop = panelY + WINDOW_HEIGHT - FOOTER_HEIGHT
        if (my < contentTop || my > footerTop) {
            return false
        }

        editHudButton?.let { button ->
            if (contains(button.x, button.y, button.w, button.h, mx, my)) {
                ModuleManager.saveConfig()
                MinecraftClient.getInstance().setScreen(HudEditScreen())
                return true
            }
        }

        for (hit in sliderRows) {
            if (contains(hit.x, hit.y, hit.w, hit.h, mx, my)) {
                draggingSlider = hit.setting
                updateSlider(hit, mx)
                return true
            }
        }

        for (hit in moduleRows) {
            if (contains(hit.x, hit.y, hit.w, hit.h, mx, my)) {
                hit.module.toggle()
                ModuleManager.saveConfig()
                return true
            }
        }

        return false
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        val setting = draggingSlider ?: return false
        val hit = sliderRows.firstOrNull { it.setting == setting } ?: return false
        updateSlider(hit, click.x())
        return true
    }

    override fun mouseReleased(click: Click): Boolean {
        if (draggingSlider == null) {
            return false
        }
        draggingSlider = null
        ModuleManager.saveConfig()
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val contentX = panelX + SIDEBAR_WIDTH
        val contentTop = panelY + HEADER_HEIGHT
        if (!contains(contentX, contentTop, WINDOW_WIDTH - SIDEBAR_WIDTH, WINDOW_HEIGHT - HEADER_HEIGHT, mouseX, mouseY)) {
            return false
        }
        scrollOffset -= (verticalAmount * SCROLL_STEP).toInt()
        return true
    }

    private fun trimToWidth(text: String, maxWidth: Int): String {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text
        }
        var cut = text
        while (cut.isNotEmpty() && textRenderer.getWidth("$cut...") > maxWidth) {
            cut = cut.dropLast(1)
        }
        return "$cut..."
    }

    private fun tryApplyBlur(context: DrawContext) {
        try {
            context.applyBlur()
        } catch (alreadyBlurred: IllegalStateException) {
            // Minecraft itself applies a blur once per frame when a screen opens
            // (Screen#renderBackground, gated by the vanilla "Menu Background
            // Blurriness" option) and can do so before our own render() runs.
            // The effect is already in place either way, so ignore it.
        }
    }

    private fun drawToggle(context: DrawContext, x: Int, y: Int, on: Boolean) {
        RoundedRect.fill(
            context, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2,
            if (on) ACCENT else COLOR_TOGGLE_OFF
        )
        val thumbSize = TOGGLE_HEIGHT - 4
        val thumbX = if (on) x + TOGGLE_WIDTH - thumbSize - 2 else x + 2
        RoundedRect.fill(context, thumbX, y + 2, thumbSize, thumbSize, thumbSize / 2, COLOR_TOGGLE_THUMB)
    }

    private fun updateSlider(hit: SliderHit, mouseX: Double) {
        val fraction = (mouseX - hit.x) / hit.w
        hit.setting.fraction = fraction.coerceIn(0.0, 1.0)
    }

    private fun contains(x: Int, y: Int, w: Int, h: Int, px: Double, py: Double) =
        px >= x && px < x + w && py >= y && py < y + h
}
