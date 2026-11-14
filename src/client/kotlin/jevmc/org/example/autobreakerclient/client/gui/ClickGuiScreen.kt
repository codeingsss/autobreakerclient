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
import kotlin.math.min
import kotlin.math.roundToInt

private const val WINDOW_WIDTH = 460
private const val WINDOW_HEIGHT = 380
private const val HEADER_HEIGHT = 32
private const val SIDEBAR_WIDTH = 130
private const val TAB_HEIGHT = 26
private const val TAB_SPACING = 4
private const val CONTENT_PADDING = 10
private const val ROW_HEIGHT = 20
private const val ROW_SPACING = 4
private const val SLIDER_HEIGHT = 14
private const val CLOSE_BUTTON_SIZE = 16
private const val SCROLL_STEP = 16

private const val TOGGLE_WIDTH = 26
private const val TOGGLE_HEIGHT = 13
private const val ACCENT_BAR_WIDTH = 3

private const val COLOR_HEADER_BG = 0xE614141A.toInt()
private const val COLOR_SIDEBAR_BG = 0xE60D0D12.toInt()
private const val COLOR_CONTENT_BG = 0xE6101016.toInt()
private const val COLOR_TAB_IDLE = 0x00000000
private const val COLOR_TAB_HOVER = 0x30FFFFFF
private const val COLOR_TAB_SELECTED = 0xFFFF8A33.toInt()
private const val COLOR_ROW_IDLE = 0x30000000
private const val COLOR_ROW_HOVER = 0x40FFFFFF
private const val COLOR_ROW_ENABLED_BG = 0x20FF8A33
private const val COLOR_ROW_ACCENT_BAR = 0xFFFF8A33.toInt()
private const val COLOR_TOGGLE_TRACK_OFF = 0x60FFFFFF
private const val COLOR_TOGGLE_TRACK_ON = 0xFFFF8A33.toInt()
private const val COLOR_TOGGLE_THUMB = 0xFFFFFFFF.toInt()
private const val COLOR_SLIDER_TRACK = 0x30000000
private const val COLOR_SLIDER_FILL = 0xFFFF8A33.toInt()
private const val COLOR_CLOSE_IDLE = 0x20FFFFFF
private const val COLOR_CLOSE_HOVER = 0xFFE64C4C.toInt()
private const val COLOR_SCROLLBAR = 0x80FFFFFF.toInt()
private const val COLOR_ACCENT_TEXT = 0xFFFFB366.toInt()
private const val COLOR_BORDER = 0xFFFF8A33.toInt()
private const val BORDER_THICKNESS = 2
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

private data class TabHit(val category: Category, val x: Int, val y: Int, val w: Int, val h: Int)
private data class ButtonHit(val x: Int, val y: Int, val w: Int, val h: Int)
private data class RowHit(val module: Module, val x: Int, val y: Int, val w: Int, val h: Int)
private data class SliderHit(val setting: IntSetting, val x: Int, val y: Int, val w: Int, val h: Int)

/**
 * A fixed, centered settings window: a rectangular panel with a left-hand
 * category sidebar and a scrollable content list on the right, instead of a
 * single draggable floating box. The background is blurred with the vanilla
 * [DrawContext.applyBlur] post effect and every panel uses rounded corners
 * via [RoundedRect] - no shaders, textures or mixins required.
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

        tryApplyBlur(context)

        val contentX = panelX + SIDEBAR_WIDTH
        val contentTop = panelY + HEADER_HEIGHT
        val contentBottom = panelY + WINDOW_HEIGHT
        val contentWidth = WINDOW_WIDTH - SIDEBAR_WIDTH

        // A single orange backdrop drawn first, slightly larger than the three panels drawn
        // on top of it, leaves a persistent border around the whole window - including a
        // thin ring at the seams between header/sidebar/content - so the brand reads even
        // when every module is toggled off.
        RoundedRect.fill(
            context, panelX - BORDER_THICKNESS, panelY - BORDER_THICKNESS,
            WINDOW_WIDTH + BORDER_THICKNESS * 2, WINDOW_HEIGHT + BORDER_THICKNESS * 2, 12, COLOR_BORDER
        )

        RoundedRect.fill(context, panelX, panelY, WINDOW_WIDTH, HEADER_HEIGHT, 10, COLOR_HEADER_BG)
        RoundedRect.fill(context, panelX, contentTop, SIDEBAR_WIDTH, WINDOW_HEIGHT - HEADER_HEIGHT, 10, COLOR_SIDEBAR_BG)
        RoundedRect.fill(context, contentX, contentTop, contentWidth, WINDOW_HEIGHT - HEADER_HEIGHT, 10, COLOR_CONTENT_BG)

        context.drawCenteredTextWithShadow(
            textRenderer, title, panelX + WINDOW_WIDTH / 2,
            panelY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, COLOR_WHITE
        )

        val closeX = panelX + WINDOW_WIDTH - CONTENT_PADDING - CLOSE_BUTTON_SIZE
        val closeY = panelY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2
        val closeHovered = contains(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, mouseX.toDouble(), mouseY.toDouble())
        RoundedRect.fill(context, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, 4, if (closeHovered) COLOR_CLOSE_HOVER else COLOR_CLOSE_IDLE)
        context.drawCenteredTextWithShadow(
            textRenderer, Text.literal("x"), closeX + CLOSE_BUTTON_SIZE / 2,
            closeY + (CLOSE_BUTTON_SIZE - textRenderer.fontHeight) / 2, COLOR_WHITE
        )

        layoutTabs()
        for (tab in tabHits) {
            val selected = tab.category == selectedCategory
            val hovered = contains(tab.x, tab.y, tab.w, tab.h, mouseX.toDouble(), mouseY.toDouble())
            val bg = if (selected) COLOR_TAB_SELECTED else if (hovered) COLOR_TAB_HOVER else COLOR_TAB_IDLE
            if (bg != COLOR_TAB_IDLE) {
                RoundedRect.fill(context, tab.x, tab.y, tab.w, tab.h, 6, bg)
            }
            context.drawTextWithShadow(
                textRenderer, tab.category.displayName, tab.x + 10,
                tab.y + (tab.h - textRenderer.fontHeight) / 2, COLOR_WHITE
            )
        }

        val naturalHeight = layoutModules(contentX, contentTop, contentWidth, 0)
        val visibleHeight = WINDOW_HEIGHT - HEADER_HEIGHT - CONTENT_PADDING * 2
        val maxScroll = max(0, naturalHeight - visibleHeight)
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)

        moduleRows.clear()
        sliderRows.clear()
        layoutModules(contentX, contentTop, contentWidth, scrollOffset)

        context.enableScissor(contentX, contentTop, contentX + contentWidth, contentBottom)

        editHudButton?.let { button ->
            val hovered = contains(button.x, button.y, button.w, button.h, mouseX.toDouble(), mouseY.toDouble()) &&
                withinContent(mouseY.toDouble(), contentTop, contentBottom)
            RoundedRect.fill(context, button.x, button.y, button.w, button.h, 6, if (hovered) COLOR_TAB_HOVER else COLOR_ROW_IDLE)
            context.drawTextWithShadow(
                textRenderer, "Edit HUD Layout...", button.x + 8,
                button.y + (button.h - textRenderer.fontHeight) / 2, COLOR_ACCENT_TEXT
            )
        }

        for (hit in moduleRows) {
            val enabled = hit.module.isEnabled
            val hovered = contains(hit.x, hit.y, hit.w, hit.h, mouseX.toDouble(), mouseY.toDouble()) &&
                withinContent(mouseY.toDouble(), contentTop, contentBottom)
            val bg = if (enabled) COLOR_ROW_ENABLED_BG else if (hovered) COLOR_ROW_HOVER else COLOR_ROW_IDLE
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 6, bg)
            if (enabled) {
                RoundedRect.fill(context, hit.x, hit.y, ACCENT_BAR_WIDTH, hit.h, 2, COLOR_ROW_ACCENT_BAR)
            }
            val textX = hit.x + 8 + if (enabled) ACCENT_BAR_WIDTH + 2 else 0
            context.drawTextWithShadow(
                textRenderer, hit.module.name, textX,
                hit.y + (hit.h - textRenderer.fontHeight) / 2, if (enabled) COLOR_ACCENT_TEXT else COLOR_WHITE
            )
            drawToggle(context, hit.x + hit.w - TOGGLE_WIDTH - 8, hit.y + (hit.h - TOGGLE_HEIGHT) / 2, enabled)
        }

        for (hit in sliderRows) {
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 4, COLOR_SLIDER_TRACK)
            val fillWidth = (hit.w * hit.setting.fraction).roundToInt()
            if (fillWidth > 0) {
                RoundedRect.fill(context, hit.x, hit.y, fillWidth, hit.h, 4, COLOR_SLIDER_FILL)
            }
            val label = "${hit.setting.name}: ${hit.setting.value}"
            context.drawTextWithShadow(textRenderer, label, hit.x + 4, hit.y + (hit.h - textRenderer.fontHeight) / 2, COLOR_WHITE)
        }

        context.disableScissor()

        if (maxScroll > 0) {
            val thumbHeight = max(12, visibleHeight * visibleHeight / naturalHeight)
            val thumbY = contentTop + (visibleHeight - thumbHeight) * scrollOffset / maxScroll
            context.fill(contentX + contentWidth - 4, thumbY, contentX + contentWidth - 1, thumbY + thumbHeight, COLOR_SCROLLBAR)
        }
    }

    private fun layoutTabs() {
        tabHits.clear()
        var y = panelY + HEADER_HEIGHT + TAB_SPACING
        val x = panelX + TAB_SPACING
        val w = SIDEBAR_WIDTH - TAB_SPACING * 2
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
        val rowWidth = contentWidth - CONTENT_PADDING * 2
        var y = contentTop + CONTENT_PADDING - scrollOffsetPx

        editHudButton = null
        if (selectedCategory == Category.HUD) {
            editHudButton = ButtonHit(rowX, y, rowWidth, ROW_HEIGHT)
            y += ROW_HEIGHT + ROW_SPACING
        }

        for (module in ModuleManager.getModules(selectedCategory)) {
            moduleRows += RowHit(module, rowX, y, rowWidth, ROW_HEIGHT)
            y += ROW_HEIGHT + ROW_SPACING
            if (module.isEnabled) {
                for (setting in module.settings) {
                    if (setting is IntSetting) {
                        sliderRows += SliderHit(setting, rowX, y, rowWidth, SLIDER_HEIGHT)
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
        val contentBottom = panelY + WINDOW_HEIGHT
        if (!withinContent(my, contentTop, contentBottom)) {
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

    private fun withinContent(y: Double, contentTop: Int, contentBottom: Int) = y in contentTop.toDouble()..contentBottom.toDouble()

    private fun tryApplyBlur(context: DrawContext) {
        try {
            context.applyBlur()
        } catch (alreadyBlurred: IllegalStateException) {
            // Minecraft itself applies a blur once per frame when a screen is opened
            // (Screen#renderBackground, gated by the vanilla "Menu Background
            // Blurriness" option) and can do so immediately on open, before our own
            // render() call. The effect is already in place either way, so ignore it.
        }
    }

    private fun drawToggle(context: DrawContext, x: Int, y: Int, on: Boolean) {
        RoundedRect.fill(
            context, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2,
            if (on) COLOR_TOGGLE_TRACK_ON else COLOR_TOGGLE_TRACK_OFF
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
