package jevmc.org.example.autobreakerclient.client.module.setting;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private Runnable onChange = () -> {};

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    protected void fireChange() {
        onChange.run();
    }
}
