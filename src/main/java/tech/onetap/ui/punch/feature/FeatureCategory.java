package tech.onetap.ui.punch.feature;

public enum FeatureCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visuals"),
    PLAYER("Player"),
    MISC("Other");

    private final String displayName;

    FeatureCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
