package jevmc.org.example.autobreakerclient.client.module;

public enum Category {
    PERFORMANCE("Performance"),
    HUD("HUD");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
