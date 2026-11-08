package jevmc.org.example.autobreakerclient.client.module.setting;

public class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String name, int defaultValue, int min, int max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public void set(int newValue) {
        int clamped = Math.max(min, Math.min(max, newValue));
        if (clamped != value) {
            value = clamped;
            fireChange();
        }
    }

    public double getFraction() {
        return (value - min) / (double) (max - min);
    }

    public void setFraction(double fraction) {
        set((int) Math.round(min + fraction * (max - min)));
    }
}
