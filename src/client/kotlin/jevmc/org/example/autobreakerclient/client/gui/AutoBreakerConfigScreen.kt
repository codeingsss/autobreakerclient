package jevmc.org.example.autobreakerclient.client.gui

import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import jevmc.org.example.autobreakerclient.client.module.Category
import jevmc.org.example.autobreakerclient.client.module.Module
import jevmc.org.example.autobreakerclient.client.module.ModuleManager
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

/**
 * Builds AutoBreaker's settings screen with YACL instead of drawing one by
 * hand, so the look is a maintained, consistent config UI rather than our own
 * solid fills. The module model is unchanged - this only projects
 * [ModuleManager]'s modules and their [IntSetting]s onto YACL's builders, and
 * writes straight through to the live module on every change.
 */
object AutoBreakerConfigScreen {

    fun create(parent: Screen?): Screen {
        val builder = YetAnotherConfigLib.createBuilder()
            .title(Text.literal("AutoBreaker"))

        for (category in Category.values()) {
            val modules = ModuleManager.getModules(category)
            if (modules.isEmpty()) {
                continue
            }
            builder.category(buildCategory(category, modules))
        }

        return builder
            .save { ModuleManager.saveConfig() }
            .build()
            .generateScreen(parent)
    }

    private fun buildCategory(category: Category, modules: List<Module>): ConfigCategory {
        val builder = ConfigCategory.createBuilder().name(Text.literal(category.displayName))

        if (category == Category.HUD) {
            builder.option(
                ButtonOption.createBuilder()
                    .name(Text.literal("Edit HUD Layout"))
                    .description(
                        OptionDescription.of(
                            Text.literal(
                                "Drag every HUD element - enabled or not - anywhere on screen. Escape saves and closes."
                            )
                        )
                    )
                    .action { _ ->
                        ModuleManager.saveConfig()
                        MinecraftClient.getInstance().setScreen(HudEditScreen())
                    }
                    .build()
            )
        }

        for (module in modules) {
            val sliders = module.settings.filterIsInstance<IntSetting>()
            if (sliders.isEmpty()) {
                builder.option(toggleOption(module))
            } else {
                // Modules that carry settings get their own group, so the
                // sliders visibly belong to the toggle above them.
                val group = OptionGroup.createBuilder()
                    .name(Text.literal(module.name))
                    .description(OptionDescription.of(Text.literal(module.description)))
                    .option(toggleOption(module))
                for (slider in sliders) {
                    group.option(sliderOption(slider))
                }
                builder.group(group.build())
            }
        }
        return builder.build()
    }

    private fun toggleOption(module: Module): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(Text.literal(module.name))
            .description(OptionDescription.of(Text.literal(module.description)))
            .binding(false, { module.isEnabled }, { enabled -> module.setEnabled(enabled) })
            .controller(TickBoxControllerBuilder::create)
            .build()

    private fun sliderOption(setting: IntSetting): Option<Int> =
        Option.createBuilder<Int>()
            .name(Text.literal(setting.name))
            .binding(setting.value, { setting.value }, { value -> setting.set(value) })
            .controller { option ->
                IntegerSliderControllerBuilder.create(option)
                    .range(setting.min, setting.max)
                    .step(1)
            }
            .build()
}
