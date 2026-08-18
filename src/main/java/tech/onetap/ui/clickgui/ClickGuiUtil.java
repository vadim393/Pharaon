package tech.onetap.ui.clickgui;

import net.minecraft.util.math.MathHelper;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClickGuiUtil {
    public static final float NC18 = 12.0f;
    public static final float NC16 = 11.0f;
    public static final float NC14 = 9.5f;
    public static final float NC13 = 9.0f;
    public static final float NC12 = 8.5f;
    public static final float NC11 = 7.5f;
    public static final float BOLD13 = 9.0f;
    public static final float SEMI12 = 8.5f;
    public static final float SEMI15 = 10.5f;
    public static final float ICON10 = 7.0f;
    public static final float ICON14 = 9.5f;

    private static final Map<String, String> STYLE_COLORS = new LinkedHashMap<>();

    static {
        STYLE_COLORS.put("1", "#2850a8");
        STYLE_COLORS.put("2", "#15c9de");
        STYLE_COLORS.put("3", "#6a83dd");
        STYLE_COLORS.put("4", "#7444cd");
        STYLE_COLORS.put("5", "#FFC8C8");
        STYLE_COLORS.put("6", "#FFE8C1");
        STYLE_COLORS.put("7", "#c9c1f8");
        STYLE_COLORS.put("8", "#4ee4b4");
        STYLE_COLORS.put("9", "#cf125e");
        STYLE_COLORS.put("10", "#ffffff");
        STYLE_COLORS.put("11", "#bf5d5d");
        STYLE_COLORS.put("12", "#434566");
        STYLE_COLORS.put("13", "astolfo");
        STYLE_COLORS.put("14", "rgba(255, 255, 255, 255)");
    }

    public static String theme = "1";

    public static Map<String, String> getStyleColors() {
        return STYLE_COLORS;
    }

    public static int firstColor() {
        return colorFor(theme);
    }

    public static int secondColor() {
        return ColorProvider.interpolateColor(firstColor(), 0xFFFFFFFF, 0.14f);
    }

    public static int colorFor(String id) {
        if (id == null) return 0xFF000000;
        if (id.equals("13")) return astolfo(10, 0, 1.0f, 1.0f);
        if (id.equals("14")) return ColorProvider.rgba(255, 255, 255, 255);
        return hexToColor(STYLE_COLORS.get(id));
    }

    public static int hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFF000000;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return (int) (Long.parseLong(clean, 16) & 0xFFFFFFL) | 0xFF000000;
        } catch (NumberFormatException e) {
            return 0xFF000000;
        }
    }

    public static int astolfo(int speed, int index, float sat, float bri) {
        long hue = ((System.currentTimeMillis() / Math.max(1, speed)) + index) % 360;
        return Color.HSBtoRGB(hue / 360.0f, sat, bri);
    }

    public static int applyOpacity(int color, float alpha) {
        return (color & 0x00FFFFFF) | ((int) (MathHelper.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24);
    }

    public static float fast(float current, float target, float speed) {
        return current + (target - current) * (1.0f - (float) Math.pow(2.0, -speed * 0.06));
    }

    public static final float RADIUS_PANEL = 2.0f;
    public static final float RADIUS_ROW = 1.0f;
    public static final float RADIUS_HEADER = 1.0f;
    public static final float ROW_HEIGHT = 19.0f;
    public static final float HEADER_HEIGHT = 24.0f;

    public static int background() {
        return ColorProvider.rgba(16, 16, 18, 240);
    }

    public static int backgroundSoft() {
        return ColorProvider.rgba(24, 24, 26, 220);
    }

    public static int glow() {
        return ColorProvider.rgba(0, 0, 0, 45);
    }

    public static int hoverBackground() {
        return ColorProvider.rgba(255, 255, 255, 16);
    }

    public static int separator() {
        return ColorProvider.rgba(255, 255, 255, 32);
    }

    public static int textColor() {
        return 0xFFFFFFFF;
    }

    public static int textSecondary() {
        return ColorProvider.rgba(205, 205, 205, 255);
    }

    public static int textDisabled() {
        return ColorProvider.rgba(140, 140, 140, 255);
    }

    public static int textMuted() {
        return ColorProvider.rgba(105, 105, 105, 255);
    }

    public static int track() {
        return ColorProvider.rgba(255, 255, 255, 30);
    }

    public static int accent() {
        return 0xFFFFFFFF;
    }

    public static int accentSoft() {
        return ColorProvider.setAlpha(0xFFFFFFFF, 60);
    }

    public static int accentOutline() {
        return ColorProvider.setAlpha(0xFFFFFFFF, 90);
    }

    public static void drawRoundOutline(float x, float y, float w, float h, float r, int fill, int tl, int tr, int br, int bl) {
        DrawUtil.drawRound(x, y, w, h, r, tl, tr, br, bl);
        if ((fill & 0xFF000000) != 0) {
            DrawUtil.drawRound(x + 1, y + 1, w - 2, h - 2, Math.max(0.0f, r - 1.0f), fill);
        }
    }

    public static void drawString(MsdfFont font, String text, float x, float y, int color, float size) {
        DrawUtil.drawText(font, text, x, y, color, size);
    }

    public static void drawCenteredString(MsdfFont font, String text, float cx, float y, int color, float size) {
        DrawUtil.drawText(font, text, cx - font.getWidth(text, size) / 2.0f, y, color, size);
    }
}
