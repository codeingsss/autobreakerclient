package jevmc.org.example.autobreakerclient.client.oneconfig

import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint

class AutoBreakerConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        context.register(AutoBreakerConfig())
    }
}
