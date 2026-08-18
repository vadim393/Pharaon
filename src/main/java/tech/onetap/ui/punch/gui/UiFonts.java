package tech.onetap.ui.punch.gui;

import tech.onetap.util.render.msdf.Fonts;

public final class UiFonts {
    private UiFonts() {
    }

    private static MsdfFont displayFont;

    public static MsdfFont sfProDisplay() {
        if (displayFont == null) {
            displayFont = new MsdfFont(Fonts.SFREGULAR.get());
        }
        return displayFont;
    }

    public static MsdfFont sfPro(int weight) {
        return switch (weight) {
            case 6 -> new MsdfFont(Fonts.SFMEDIUM.get());
            case 7 -> new MsdfFont(Fonts.SFSEMIBOLD.get());
            default -> new MsdfFont(Fonts.SFREGULAR.get());
        };
    }
}