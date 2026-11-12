package jevmc.org.example.autobreakerclient.client.oneconfig

import jevmc.org.example.autobreakerclient.client.module.Category as ModuleCategory
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree

/**
 * Exposes every existing [jevmc.org.example.autobreakerclient.client.module.Module]
 * as an OneConfig [Tree] via [Properties.functional], which wraps a plain
 * getter/setter pair as a config property. Module state, settings and their
 * side effects (onEnable/onDisable) stay exactly as they were - this only
 * adapts them onto OneConfig's config tree, it doesn't replace them.
 */
class AutoBreakerConfig : Config("AutoBreaker", "AutoBreaker", Config.Category.PERFORMANCE) {
    override fun makeTree(): Tree {
        val root = Tree.tree("AutoBreaker")
        for (category in ModuleCategory.values()) {
            val modules = ModuleManager.getModules(category)
            if (modules.isEmpty()) {
                continue
            }
            val categoryTree = Tree.tree(category.displayName)
            for (module in modules) {
                categoryTree.put(
                    Properties.functional(
                        { module.isEnabled },
                        { enabled: Boolean -> module.setEnabled(enabled) },
                        module.name
                    )
                )
                for (setting in module.settings) {
                    if (setting is IntSetting) {
                        categoryTree.put(
                            Properties.functional(
                                { setting.value },
                                { value: Int -> setting.set(value) },
                                setting.name
                            )
                        )
                    }
                }
            }
            root.put(categoryTree)
        }
        return root
    }
}
