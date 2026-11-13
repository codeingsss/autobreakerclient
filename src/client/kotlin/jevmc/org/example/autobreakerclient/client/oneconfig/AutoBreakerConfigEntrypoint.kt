package jevmc.org.example.autobreakerclient.client.oneconfig

import jevmc.org.example.autobreakerclient.client.module.Category as ModuleCategory
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("AutoBreaker")

private val ONECONFIG_CATEGORY = mapOf(
    ModuleCategory.PERFORMANCE to Config.Category.PERFORMANCE,
    ModuleCategory.QOL to Config.Category.QOL,
    ModuleCategory.VISUAL to Config.Category.VISUALS,
    ModuleCategory.HUD to Config.Category.HUD
)

class AutoBreakerConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        for (category in ModuleCategory.values()) {
            if (ModuleManager.getModules(category).isEmpty()) {
                continue
            }
            val oneConfigCategory = ONECONFIG_CATEGORY[category] ?: Config.Category.OTHER
            context.register(AutoBreakerCategoryConfig(category, oneConfigCategory))
        }
        try {
            activateAutoBreakerOneConfigTheme()
        } catch (e: Throwable) {
            // Reaches into OneConfig's internal.ui.themes package (see
            // AutoBreakerTheme.kt) since there's no supported public way to
            // set a custom accent color - if that internal shape changes in
            // a future OneConfig release, fail quietly rather than taking
            // the rest of this entrypoint (our actual config trees) down
            // with it.
            LOGGER.warn("Could not apply the AutoBreaker OneConfig theme", e)
        }
    }
}
