package jevmc.org.example.autobreakerclient.client.gui

import jevmc.org.example.autobreakerclient.client.module.Category
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.impl.hud.HudModule
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

private const val BORDER = 2
private const val COLOR_ENABLED = 0xA0FFB366.toInt()
private const val COLOR_ENABLED_HOVER = 0xFFFF8A33.toInt()
private const val COLOR_DISABLED = 0x80888888.toInt()

/**
 * Drag-to-position editor for HUD elements. Every HUD module (even disabled
 * ones, so they can be placed in advance) is drawn at its stored position
 * with a box around it; dragging a box moves that module's saved (x, y).
 */
class HudEditScreen : Screen(Text.literal("Edit HUD")) {
    private var dragging: HudModule? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun shouldPause() = false

    override fun close() {
        ModuleManager.saveConfig()
        super.close()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        for (module in ModuleManager.getModules(Category.HUD)) {
            val hudModule = module as? HudModule ?: continue
            hudModule.renderAt(context)
            val hovered = contains(hudModule, mouseX.toDouble(), mouseY.toDouble())
            val color = when {
                !hudModule.isEnabled -> COLOR_DISABLED
                hovered -> COLOR_ENABLED_HOVER
                else -> COLOR_ENABLED
            }
            drawOutline(
                context, hudModule.x - BORDER, hudModule.y - BORDER,
                hudModule.width + BORDER * 2, hudModule.height + BORDER * 2, color
            )
        }

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Drag elements to position them. Press Escape when done."),
            width / 2, 8, 0xFFFFFFFF.toInt()
        )
    }

    override fun mouseClicked(click: Click, doubleClick: Boolean): Boolean {
        if (click.button() != 0) {
            return false
        }
        for (module in ModuleManager.getModules(Category.HUD)) {
            val hudModule = module as? HudModule ?: continue
            if (contains(hudModule, click.x(), click.y())) {
                dragging = hudModule
                dragOffsetX = click.x().toInt() - hudModule.x
                dragOffsetY = click.y().toInt() - hudModule.y
                return true
            }
        }
        return false
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        val module = dragging ?: return false
        module.setPosition(click.x().toInt() - dragOffsetX, click.y().toInt() - dragOffsetY)
        return true
    }

    override fun mouseReleased(click: Click): Boolean {
        if (dragging == null) {
            return false
        }
        dragging = null
        ModuleManager.saveConfig()
        return true
    }

    private fun contains(module: HudModule, px: Double, py: Double): Boolean {
        val x = module.x - BORDER
        val y = module.y - BORDER
        val w = module.width + BORDER * 2
        val h = module.height + BORDER * 2
        return px >= x && px < x + w && py >= y && py < y + h
    }

    private fun drawOutline(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        context.fill(x, y, x + w, y + 1, color)
        context.fill(x, y + h - 1, x + w, y + h, color)
        context.fill(x, y, x + 1, y + h, color)
        context.fill(x + w - 1, y, x + w, y + h, color)
    }
}
