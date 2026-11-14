@file:OptIn(org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi::class)

package jevmc.org.example.autobreakerclient.client.oneconfig

import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UIBranding
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("AutoBreaker")
private val ACCENT = Color(0xFFFF8A33)
private val ACCENT_DIM = Color(0xFFCC6A26)
private const val LOGO_PATH = "assets/autobreakerclient/brand/autobreaker.svg"

private val theme: UITheme by lazy {
    (OneConfigTheme.POLY_GLASS_DARK as UITheme).copy(
        name = "AutoBreaker",
        accentTextColor = ACCENT,
        controlThumbColor = ACCENT,
        favoriteColor = ACCENT_DIM,
        branding = UIBranding(LOGO_PATH)
    )
}
private var registered = false

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
 * picker uses - confirmed by disassembling ChipsKt, which reads
 * `UITheme.getAccentTextColor` directly for its selected-chip fill, the same
 * color this overrides.
 *
 * Called more than once (see [jevmc.org.example.autobreakerclient.client.AutobreakerclientClient]'s
 * tick loop) because something in OneConfig's own lazily-loaded theme restore
 * (triggered whenever its UI first actually composes, not during the earlier
 * config-entrypoint phase this originally only ran from once) can still win
 * a race and revert the active theme back to its own default after this
 * already ran - reasserting periodically converges back to this theme
 * within a few seconds regardless of exactly when that happens, without
 * needing to pin down OneConfig's own undocumented load order precisely.
 */
fun activateAutoBreakerOneConfigTheme() {
    try {
        if (!registered) {
            ThemeRegistry.register(theme)
            registered = true
        }
        ThemeRegistry.activate(theme)
        // The sidebar's "ONECONFIG" wordmark image is a separate static
        // override on top of whatever the active theme's own logoPath says -
        // setting both covers it regardless of which one the Branding
        // composable actually reads from.
        OneConfigTheme.overrideLogo(LOGO_PATH)
    } catch (e: Throwable) {
        LOGGER.warn("Could not apply the AutoBreaker OneConfig theme", e)
    }
}
