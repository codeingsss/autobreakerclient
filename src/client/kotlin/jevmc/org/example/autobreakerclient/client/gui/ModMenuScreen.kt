package jevmc.org.example.autobreakerclient.client.gui

import jevmc.org.example.autobreakerclient.client.module.Category
import jevmc.org.example.autobreakerclient.client.module.Module
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * AutoBreaker's mod menu, laid out the way Vape Lite's is: one compact window,
 * a category rail down the left, and the category's modules as a plain list of
 * rows with a switch on the right - not cards.
 *
 * Descriptions live in the footer and belong to whichever row is hovered, so a
 * long description can never collide with the row's own controls.
 *
 * Everything is drawn from [Theme]'s primitives, since vanilla has no row,
 * switch or rail widget to borrow. Toggling and dragging write straight through
 * to the live [Module]; [close] is the only place that persists them.
 */
class ModMenuScreen(expandAllOnOpen: Boolean = false) : Screen(Text.literal("AutoBreaker")) {

    private companion object {
        const val HEADER_HEIGHT = 30
        const val FOOTER_HEIGHT = 20
        const val RAIL_WIDTH = 116
        const val PAD = 8
        const val ROW_HEIGHT = 20
        const val SETTING_ROW_HEIGHT = 20
        const val SCROLLBAR_WIDTH = 3
    }

    private var selected: Category = Category.entries.first()
    private var search = ""
    private var searchFocused = false
    private var scroll = 0.0
    private var maxScroll = 0.0
    private val expanded = mutableSetOf<String>()

    init {
        // Only the dev screenshot preview passes true, so a run of it shows the
        // settings rows rather than just the collapsed list.
        if (expandAllOnOpen) {
            ModuleManager.getModules().filter { sliders(it).isNotEmpty() }.forEach { expanded.add(it.name) }
        }
    }
    private var dragging: IntSetting? = null
    private var draggingTrack: IntRange = IntRange.EMPTY

    /** Module whose description the footer is showing, set while rendering. */
    private var hoveredModule: Module? = null

    /** Per-module animation progress, so switches and hovers move rather than snap. */
    private val toggleAnim = mutableMapOf<String, Double>()
    private val hoverAnim = mutableMapOf<String, Double>()
    private var lastFrameMs = System.currentTimeMillis()

    // Window geometry, recomputed every frame so a resize needs no bookkeeping.
    private var winX = 0
    private var winY = 0
    private var winW = 0
    private var winH = 0

    private val font get() = MinecraftClient.getInstance().textRenderer

    private fun layout() {
        winW = (width - 60).coerceIn(320, 560)
        winH = (height - 60).coerceIn(200, 340)
        winX = (width - winW) / 2
        winY = (height - winH) / 2
    }

    private fun contentX() = winX + RAIL_WIDTH
    private fun contentY() = winY + HEADER_HEIGHT
    private fun contentW() = winW - RAIL_WIDTH
    private fun contentH() = winH - HEADER_HEIGHT - FOOTER_HEIGHT

    private fun rowX() = contentX() + PAD
    private fun rowW() = contentW() - PAD * 2 - SCROLLBAR_WIDTH - 2

    private fun visibleModules(): List<Module> {
        val query = search.trim().lowercase()
        return ModuleManager.getModules(selected).filter {
            query.isEmpty() ||
                it.name.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
        }
    }

    private fun sliders(module: Module): List<IntSetting> =
        module.settings.filterIsInstance<IntSetting>()

    private fun isExpanded(module: Module) = expanded.contains(module.name)

    /** Total height of a row including its settings when open. */
    private fun rowHeight(module: Module): Int {
        if (!isExpanded(module)) return ROW_HEIGHT
        return ROW_HEIGHT + sliders(module).size * SETTING_ROW_HEIGHT + 4
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        layout()
        val now = System.currentTimeMillis()
        // Seconds since the previous frame, capped so a stall doesn't make
        // every animation jump straight to its end state.
        val dt = ((now - lastFrameMs) / 1000.0).coerceIn(0.0, 0.1)
        lastFrameMs = now
        hoveredModule = null

        context.fill(0, 0, width, height, Theme.SCRIM)
        context.roundOutline(winX - 1, winY - 1, winW + 2, winH + 2, 8, 1, Theme.WINDOW_BORDER, Theme.WINDOW)

        renderRail(context, mouseX, mouseY)
        renderRows(context, mouseX, mouseY, dt)
        renderHeader(context, mouseX, mouseY)
        renderFooter(context)
    }

    private fun renderHeader(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.roundRect(winX, winY, winW, HEADER_HEIGHT, 8, Theme.HEADER)
        context.fill(winX, winY + HEADER_HEIGHT - 8, winX + winW, winY + HEADER_HEIGHT, Theme.HEADER)
        context.fill(winX, winY + HEADER_HEIGHT - 1, winX + winW, winY + HEADER_HEIGHT, Theme.WINDOW_BORDER)

        context.roundRect(winX + PAD + 2, winY + 10, 3, 10, 1, Theme.ACCENT)
        context.drawText(font, "AutoBreaker", winX + PAD + 10, winY + 11, Theme.TEXT, false)

        val box = searchBounds()
        val hovered = box.contains(mouseX, mouseY)
        val border = if (searchFocused) Theme.ACCENT else if (hovered) Theme.TRACK else Theme.WINDOW_BORDER
        context.roundOutline(box.x, box.y, box.w, box.h, 7, 1, border, Theme.FIELD)
        val placeholder = search.isEmpty() && !searchFocused
        val shown = font.trimToWidth(if (placeholder) "Search" else search, box.w - 14)
        context.drawText(
            font,
            shown,
            box.x + 7,
            box.y + (box.h - 7) / 2,
            if (placeholder) Theme.TEXT_FAINT else Theme.TEXT,
            false,
        )
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0L) {
            val caret = box.x + 7 + font.getWidth(shown)
            context.fill(caret, box.y + 4, caret + 1, box.y + box.h - 4, Theme.ACCENT)
        }

        val close = closeBounds()
        context.drawText(
            font,
            "x",
            close.x + 6,
            close.y + 5,
            if (close.contains(mouseX, mouseY)) Theme.TEXT else Theme.TEXT_MUTED,
            false,
        )
    }

    private fun renderRail(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.roundRect(winX, winY, RAIL_WIDTH, winH, 8, Theme.SIDEBAR)
        context.fill(winX + RAIL_WIDTH - 1, winY + HEADER_HEIGHT, winX + RAIL_WIDTH, winY + winH, Theme.WINDOW_BORDER)

        Category.entries.forEachIndexed { index, category ->
            val bounds = railBounds(index)
            val active = category == selected
            val hovered = bounds.contains(mouseX, mouseY)
            if (active) {
                context.roundRect(bounds.x + 5, bounds.y, bounds.w - 10, bounds.h, 5, Theme.ACCENT_DIM)
                context.roundRect(bounds.x + 5, bounds.y + 3, 2, bounds.h - 6, 1, Theme.ACCENT)
            } else if (hovered) {
                context.roundRect(bounds.x + 5, bounds.y, bounds.w - 10, bounds.h, 5, Theme.CARD)
            }
            val enabledCount = ModuleManager.getModules(category).count { it.isEnabled }
            context.drawText(
                font,
                font.trimToWidth(category.displayName, bounds.w - 32),
                bounds.x + 13,
                bounds.y + 5,
                if (active) Theme.ACCENT_TEXT else if (hovered) Theme.TEXT else Theme.TEXT_MUTED,
                false,
            )
            if (enabledCount > 0) {
                val badge = enabledCount.toString()
                context.drawText(
                    font,
                    badge,
                    bounds.x + bounds.w - 12 - font.getWidth(badge),
                    bounds.y + 5,
                    if (active) Theme.ACCENT else Theme.TEXT_FAINT,
                    false,
                )
            }
        }

        val hud = railBounds(HUD_EDITOR_ROW)
        val hudHovered = hud.contains(mouseX, mouseY)
        if (hudHovered) context.roundRect(hud.x + 5, hud.y, hud.w - 10, hud.h, 5, Theme.CARD)
        context.fill(hud.x + 11, hud.y - 5, hud.x + hud.w - 11, hud.y - 4, Theme.WINDOW_BORDER)
        context.drawText(
            font,
            "Edit HUD",
            hud.x + 13,
            hud.y + 5,
            if (hudHovered) Theme.TEXT else Theme.TEXT_MUTED,
            false,
        )
    }

    private fun renderRows(context: DrawContext, mouseX: Int, mouseY: Int, dt: Double) {
        val modules = visibleModules()
        context.enableScissor(contentX(), contentY(), contentX() + contentW(), contentY() + contentH())

        if (modules.isEmpty()) {
            val message = if (search.isBlank()) "Nothing here yet." else "No match for \"$search\""
            context.drawText(font, message, rowX() + 4, contentY() + PAD + 4, Theme.TEXT_FAINT, false)
        }

        var y = contentY() + PAD - scroll.roundToInt()
        for (module in modules) {
            renderRow(context, module, y, mouseX, mouseY, dt)
            y += rowHeight(module)
        }
        context.disableScissor()

        val contentHeight = (y + scroll.roundToInt()) - (contentY() + PAD) + PAD
        maxScroll = max(0.0, (contentHeight - contentH()).toDouble())
        scroll = scroll.coerceIn(0.0, maxScroll)
        if (maxScroll > 0) {
            val trackTop = contentY() + PAD
            val trackHeight = contentH() - PAD * 2
            val thumbHeight = max(20, (trackHeight * (contentH().toDouble() / contentHeight)).roundToInt())
            val thumbY = trackTop + ((trackHeight - thumbHeight) * (scroll / maxScroll)).roundToInt()
            val trackX = contentX() + contentW() - PAD - SCROLLBAR_WIDTH + 2
            context.roundRect(trackX, trackTop, SCROLLBAR_WIDTH, trackHeight, 1, Theme.FIELD)
            context.roundRect(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight, 1, Theme.TRACK)
        }
    }

    private fun renderRow(context: DrawContext, module: Module, y: Int, mouseX: Int, mouseY: Int, dt: Double) {
        val x = rowX()
        val w = rowW()
        val overRow = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT &&
            mouseY >= contentY() && mouseY < contentY() + contentH()
        if (overRow) hoveredModule = module

        val hover = approach(hoverAnim, module.name, if (overRow) 1.0 else 0.0, dt, 9.0)
        val on = approach(toggleAnim, module.name, if (module.isEnabled) 1.0 else 0.0, dt, 11.0)

        if (hover > 0.01) {
            context.roundRect(x, y, w, ROW_HEIGHT, 5, Theme.withAlpha(Theme.CARD_HOVER, hover * 0.85))
        }
        if (on > 0.01) {
            context.roundRect(x, y + 4, 2, ROW_HEIGHT - 8, 1, Theme.withAlpha(Theme.ACCENT, on))
        }

        val settings = sliders(module)
        val switch = switchBounds(y)
        var labelWidth = w - 16 - switch.w - 10
        if (settings.isNotEmpty()) labelWidth -= 10

        context.drawText(
            font,
            font.trimToWidth(module.name, labelWidth),
            x + 8,
            y + 6,
            Theme.mix(Theme.TEXT_MUTED, Theme.TEXT, maxOf(on, hover)),
            false,
        )

        if (settings.isNotEmpty()) {
            context.drawText(
                font,
                if (isExpanded(module)) "-" else "+",
                switch.x - 11,
                y + 6,
                if (isExpanded(module)) Theme.ACCENT_TEXT else Theme.TEXT_FAINT,
                false,
            )
        }

        context.roundRect(
            switch.x,
            switch.y,
            switch.w,
            switch.h,
            switch.h / 2,
            Theme.mix(Theme.TRACK, Theme.ACCENT, on),
        )
        val knobX = switch.x + 6 + ((switch.w - 12) * on).roundToInt()
        context.circle(knobX, switch.y + switch.h / 2, 4, 0xFFFFFFFF.toInt())

        if (!isExpanded(module)) return
        settings.forEachIndexed { i, setting ->
            val rowY = y + ROW_HEIGHT + i * SETTING_ROW_HEIGHT
            context.drawText(font, setting.name, x + 18, rowY + 1, Theme.TEXT_MUTED, false)
            val valueText = setting.value.toString()
            context.drawText(
                font,
                valueText,
                x + w - 10 - font.getWidth(valueText),
                rowY + 1,
                Theme.ACCENT_TEXT,
                false,
            )
            val track = trackBounds()
            val trackY = rowY + 13
            context.roundRect(track.first, trackY, track.last - track.first, 3, 1, Theme.TRACK)
            val filled = ((track.last - track.first) * setting.fraction).roundToInt()
            context.roundRect(track.first, trackY, filled, 3, 1, Theme.ACCENT)
            context.circle(track.first + filled, trackY + 1, 4, 0xFFFFFFFF.toInt())
        }
    }

    private fun renderFooter(context: DrawContext) {
        val footerY = winY + winH - FOOTER_HEIGHT
        context.fill(winX + RAIL_WIDTH, footerY, winX + winW, footerY + 1, Theme.WINDOW_BORDER)
        val module = hoveredModule
        val text = module?.description ?: "Right Shift opens this menu"
        val color = if (module != null) Theme.TEXT_MUTED else Theme.TEXT_FAINT
        context.drawText(
            font,
            font.trimToWidth(text, winW - RAIL_WIDTH - PAD * 2),
            winX + RAIL_WIDTH + PAD,
            footerY + 7,
            color,
            false,
        )
    }

    // ---- hit testing -------------------------------------------------------

    private class Bounds(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun contains(mouseX: Int, mouseY: Int) = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h
        fun contains(mouseX: Double, mouseY: Double) = contains(mouseX.toInt(), mouseY.toInt())
    }

    private fun searchBounds(): Bounds {
        val w = (contentW() - PAD * 2 - 16).coerceIn(70, 180)
        return Bounds(winX + winW - PAD - 14 - w, winY + 7, w, 16)
    }

    private fun closeBounds() = Bounds(winX + winW - PAD - 10, winY + 7, 16, 16)

    private fun railBounds(index: Int): Bounds {
        if (index == HUD_EDITOR_ROW) {
            return Bounds(winX, winY + winH - 24, RAIL_WIDTH, 18)
        }
        return Bounds(winX, contentY() + PAD - 3 + index * 20, RAIL_WIDTH, 18)
    }

    private fun switchBounds(y: Int) = Bounds(rowX() + rowW() - 8 - 22, y + 5, 22, 11)

    private fun trackBounds(): IntRange = (rowX() + 18)..(rowX() + rowW() - 10)

    /** Runs [action] with the module whose row block contains the mouse. */
    private inline fun forRowAt(mouseX: Double, mouseY: Double, action: (Module, Int) -> Unit) {
        if (mouseX < contentX() || mouseY < contentY() || mouseY >= contentY() + contentH()) return
        var y = contentY() + PAD - scroll.roundToInt()
        for (module in visibleModules()) {
            val h = rowHeight(module)
            if (mouseY >= y && mouseY < y + h) {
                action(module, y)
                return
            }
            y += h
        }
    }

    // ---- input -------------------------------------------------------------

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mouseX = click.x
        val mouseY = click.y
        searchFocused = searchBounds().contains(mouseX, mouseY)
        if (searchFocused) return true

        if (closeBounds().contains(mouseX, mouseY)) {
            close()
            return true
        }

        if (railBounds(HUD_EDITOR_ROW).contains(mouseX, mouseY)) {
            ModuleManager.saveConfig()
            MinecraftClient.getInstance().setScreen(HudEditScreen())
            return true
        }
        Category.entries.forEachIndexed { index, category ->
            if (railBounds(index).contains(mouseX, mouseY)) {
                if (selected != category) {
                    selected = category
                    scroll = 0.0
                }
                return true
            }
        }

        var handled = false
        forRowAt(mouseX, mouseY) { module, rowY ->
            handled = true
            if (mouseY < rowY + ROW_HEIGHT) {
                // The switch toggles; the rest of the row opens settings, and
                // toggles too when there are none to open.
                if (switchBounds(rowY).contains(mouseX, mouseY) || sliders(module).isEmpty()) {
                    module.toggle()
                } else if (!expanded.remove(module.name)) {
                    expanded.add(module.name)
                }
                return@forRowAt
            }
            val track = trackBounds()
            sliders(module).forEachIndexed { i, setting ->
                val settingY = rowY + ROW_HEIGHT + i * SETTING_ROW_HEIGHT
                // Generous vertical band: a 3px track is far too thin to hit.
                if (mouseY >= settingY + 6 && mouseY < settingY + SETTING_ROW_HEIGHT) {
                    dragging = setting
                    draggingTrack = track
                    setting.fraction = fractionFor(track, mouseX)
                    return@forRowAt
                }
            }
        }
        return handled || super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        val setting = dragging ?: return super.mouseDragged(click, deltaX, deltaY)
        setting.fraction = fractionFor(draggingTrack, click.x)
        return true
    }

    override fun mouseReleased(click: Click): Boolean {
        if (dragging != null) {
            dragging = null
            return true
        }
        return super.mouseReleased(click)
    }

    private fun fractionFor(track: IntRange, mouseX: Double): Double {
        val span = (track.last - track.first).toDouble()
        if (span <= 0) return 0.0
        return ((mouseX - track.first) / span).coerceIn(0.0, 1.0)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        if (maxScroll <= 0) return false
        scroll = (scroll - vertical * 16).coerceIn(0.0, maxScroll)
        return true
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!searchFocused || !input.isValidChar) return super.charTyped(input)
        search += input.asString()
        scroll = 0.0
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (searchFocused) {
            when (input.key) {
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (search.isNotEmpty()) search = search.dropLast(1)
                    return true
                }
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    searchFocused = false
                    return true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    if (search.isNotEmpty()) search = "" else searchFocused = false
                    return true
                }
            }
        }
        return super.keyPressed(input)
    }

    override fun close() {
        ModuleManager.saveConfig()
        super.close()
    }

    /**
     * Eases the stored value for [key] towards [target]; the rate is per second
     * so animations run at the same speed whatever the frame rate.
     */
    private fun approach(
        store: MutableMap<String, Double>,
        key: String,
        target: Double,
        dt: Double,
        rate: Double,
    ): Double {
        val current = store[key] ?: target
        val next = current + (target - current) * (dt * rate).coerceAtMost(1.0)
        store[key] = next
        return next
    }
}

private const val HUD_EDITOR_ROW = -1
