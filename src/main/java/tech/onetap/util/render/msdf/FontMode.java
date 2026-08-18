package tech.onetap.util.render.msdf;

public enum FontMode {
    DEFAULT("Дефолт"),
    CUSTOM("Кастом");

    private final String displayName;

    FontMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}