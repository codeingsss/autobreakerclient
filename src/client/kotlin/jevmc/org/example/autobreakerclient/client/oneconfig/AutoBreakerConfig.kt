package jevmc.org.example.autobreakerclient.client.oneconfig

import jevmc.org.example.autobreakerclient.client.module.Category as ModuleCategory
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree

/**
 * One of these is registered per [ModuleCategory] (see
 * [AutoBreakerConfigEntrypoint]), instead of a single "AutoBreaker" config
 * holding every category as a sub-tree - OneConfig's own Mods page shows one
 * card per registered [Config], so this is what makes Performance/Quality of
 * Life/Visual/HUD show up as their own separate entries there instead of
 * being nested invisibly inside one card.
 *
 * Exposes every existing [jevmc.org.example.autobreakerclient.client.module.Module]
 * in this category as an OneConfig [Tree] via [Properties.functional], which
 * wraps a plain getter/setter pair as a config property. Module state,
 * settings and their side effects (onEnable/onDisable) stay exactly as they
 * were - this only adapts them onto OneConfig's config tree, it doesn't
 * replace them.
 */
class AutoBreakerCategoryConfig(
    private val moduleCategory: ModuleCategory,
    oneConfigCategory: Config.Category
) : Config("autobreaker_${moduleCategory.name.lowercase()}", "AutoBreaker: ${moduleCategory.displayName}", oneConfigCategory) {
    override fun makeTree(): Tree {
        val root = Tree.tree(moduleCategory.displayName)
        for (module in ModuleManager.getModules(moduleCategory)) {
            root.put(
                Properties.functional(
                    { module.isEnabled },
                    { enabled: Boolean -> module.setEnabled(enabled) },
                    module.name
                )
            )
            for (setting in module.settings) {
                if (setting is IntSetting) {
                    root.put(
                        Properties.functional(
                            { setting.value },
                            { value: Int -> setting.set(value) },
                            setting.name
                        )
                    )
                }
            }
        }
        return root
    }
}
