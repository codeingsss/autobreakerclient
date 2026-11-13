package jevmc.org.example.autobreakerclient.client.oneconfig

import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("AutoBreaker")

class AutoBreakerConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        context.register(AutoBreakerConfig())
        try {
            activateAutoBreakerOneConfigTheme()
        } catch (e: Throwable) {
            // Reaches into OneConfig's internal.ui.themes package (see
            // AutoBreakerTheme.kt) since there's no supported public way to
            // set a custom accent color - if that internal shape changes in
            // a future OneConfig release, fail quietly rather than taking
            // the rest of this entrypoint (our actual config tree) down
            // with it.
            LOGGER.warn("Could not apply the AutoBreaker OneConfig theme", e)
        }
    }
}
