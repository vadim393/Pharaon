package tech.onetap.ui.punch.gui;

import tech.onetap.util.render.renderers.DrawUtil;

public final class MsdfFont {
    private final tech.onetap.util.render.msdf.MsdfFont delegate;

    public MsdfFont(tech.onetap.util.render.msdf.MsdfFont delegate) {
        this.delegate = delegate;
    }

    public float measureWidth(String text, float size, float letterSpacing) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float raw = delegate.getWidth(text, size);
        return raw + letterSpacing * (text.length() - 1);
    }

    public float measureWidth(String text, float size) {
        return measureWidth(text, size, 0.0F);
    }

    public float textHeight(float size) {
        return size;
    }

    public float centeredTextY(float centerY, float size) {
        return centerY - size / 2.0F;
    }

    public float ascender(float size) {
        return size * 0.8F;
    }

    public String ellipsize(String text, float size, float letterSpacing, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (measureWidth(text, size, letterSpacing) <= maxWidth) {
            return text;
        }
        String ellipsis = "\u2026";
        String candidate = text;
        while (candidate.length() > 1
                && measureWidth(candidate + ellipsis, size, letterSpacing) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate + ellipsis;
    }

    public Glyph glyph(int codePoint) {
        return null;
    }

    public void draw(String text, float x, float y, int color, float size) {
        DrawUtil.drawText(delegate, text, x, y, color, size);
    }

    public tech.onetap.util.render.msdf.MsdfFont getFont() {
        return delegate;
    }

    public static final class Glyph {
        public Bounds planeBounds() {
            return null;
        }
    }

    public static final class Bounds {
        public float top() {
            return 0.0F;
        }

        public float bottom() {
            return 0.0F;
        }
    }
}