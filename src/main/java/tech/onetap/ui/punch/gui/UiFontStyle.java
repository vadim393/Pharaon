package tech.onetap.ui.punch.gui;

public enum UiFontStyle {
    REGULAR(0.0F, 0),
    REGULAR_TRACKED(0.04F, 0),
    MEDIUM(0.0F, 6),
    SEMIBOLD(0.02F, 7);

    private final float letterSpacingEm;
    private final int weight;

    UiFontStyle(float letterSpacingEm, int weight) {
        this.letterSpacingEm = letterSpacingEm;
        this.weight = weight;
    }

    public float letterSpacingEm() {
        return letterSpacingEm;
    }

    public int weight() {
        return weight;
    }
}