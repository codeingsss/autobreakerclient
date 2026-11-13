@file:OptIn(org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi::class)

package jevmc.org.example.autobreakerclient.client.oneconfig

import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme

private val ACCENT = Color(0xFFFF8A33)
private val ACCENT_DIM = Color(0xFFCC6A26)

/**
 * The public [OneConfigTheme.activate] only accepts OneConfig's own
 * pre-registered theme instances (it does an `instanceof
 * org.polyfrost.oneconfig.internal.ui.themes.UITheme` check - confirmed by
 * disassembling OneConfigUIComponentsBridgeImpl.activateTheme - which a
 * plain third-party [org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme]
 * subclass can never satisfy, since `UITheme` is `final`). [UITheme] itself
 * is a public Kotlin data class though, just living under an `internal`-named
 * package rather than Kotlin's actual `internal` visibility keyword, so it's
 * really a normal public class: copying one of OneConfig's own built-in
 * instances with [UITheme.copy] and registering/activating it directly
 * through [ThemeRegistry] (bypassing the public wrapper's type check
 * entirely) works within the same constraints OneConfig's own built-in theme
 * picker uses.
 */
fun activateAutoBreakerOneConfigTheme() {
    val base = OneConfigTheme.POLY_GLASS_DARK as UITheme
    val theme = base.copy(
        name = "AutoBreaker",
        accentTextColor = ACCENT,
        controlThumbColor = ACCENT,
        favoriteColor = ACCENT_DIM
    )
    ThemeRegistry.register(theme)
    ThemeRegistry.activate(theme)
}
