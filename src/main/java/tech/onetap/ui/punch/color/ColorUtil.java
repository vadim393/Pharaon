package tech.onetap.ui.punch.color;

import tech.onetap.util.render.providers.ColorProvider;

public final class ColorUtil {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int TRANSPARENT = 0x00000000;

    private ColorUtil() {
    }

    public static int rgb(int r, int g, int b) {
        return ColorProvider.rgb(r, g, b);
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int multiplyAlpha(int color, float alpha) {
        return ColorProvider.setAlpha(color, Math.round(alpha(color) * alpha));
    }

    public static int withAlpha(int color, int alpha) {
        return ColorProvider.setAlpha(color, alpha);
    }

    public static float[] hsv(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0.0F) {
            h = 0.0F;
        } else if (max == r) {
            h = ((g - b) / delta) % 6.0F;
        } else if (max == g) {
            h = ((b - r) / delta) + 2.0F;
        } else {
            h = ((r - g) / delta) + 4.0F;
        }
        h /= 6.0F;
        if (h < 0.0F) {
            h += 1.0F;
        }
        float s = max == 0.0F ? 0.0F : delta / max;
        return new float[]{h, s, max};
    }

    public static int fromHsv(float hue, float saturation, float value, int alpha) {
        float h = (hue % 1.0F + 1.0F) % 1.0F;
        int i = (int) Math.floor(h * 6.0F);
        float f = h * 6.0F - i;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - f * saturation);
        float t = value * (1.0F - (1.0F - f) * saturation);
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return rgba(Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F), alpha);
    }

    public static int lerp(int from, int to, float t) {
        return ColorProvider.interpolateColor(from, to, t);
    }

    public static int alpha(int color) {
        return ColorProvider.alpha(color);
    }

    public static int red(int color) {
        return ColorProvider.red(color);
    }

    public static int green(int color) {
        return ColorProvider.green(color);
    }

    public static int blue(int color) {
        return ColorProvider.blue(color);
    }
}
