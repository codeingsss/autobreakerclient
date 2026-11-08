package jevmc.org.example.autobreakerclient.client.module;

import jevmc.org.example.autobreakerclient.client.module.setting.Setting;

import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    protected Module(String name, String description, Category category, boolean defaultEnabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        if (defaultEnabled) {
            onEnable();
        }
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Setting<?>> getSettings() {
        return List.of();
    }
}
