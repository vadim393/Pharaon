package tech.onetap.module.list.render.hud.renderers;

import org.joml.Vector4f;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

/**
 * Общие элементы HUD-стиля DLC: тёмная "стеклянная" панель (градиентная заливка,
 * мягкая тень, тонкая светлая обводка и highlight сверху), header-полоса с
 * разделителем в цвет темы и заголовок с градиентом от темы к белому.
 */
public final class PouchHud {

    public static final int BG = ColorProvider.rgba(18, 20, 26, 255);
    public static final int TEXT = ColorProvider.rgba(235, 238, 245, 255);
    public static final int BORDER = ColorProvider.rgba(255, 255, 255, 255);
    public static final int GLOW = ColorProvider.rgba(110, 120, 150, 255);

    public static final float RADIUS = 10f;
    public static final float HEADER_HEIGHT = 15f;

    private static final int BG_TOP = ColorProvider.rgba(34, 38, 48, 255);
    private static final int BG_BOTTOM = ColorProvider.rgba(13, 15, 20, 255);
    private static final int TEXT_DIM = ColorProvider.rgba(150, 156, 170, 255);

    private PouchHud() {
    }

    /**
     * Стеклянная панель DLC: мягкая многослойная тень, вертикальный градиент
     * (сверху светлее), тонкий светлый highlight сверху, header-полоса с
     * разделителем в цвет темы и аккуратная обводка. alpha — глобальная 0..1.
     */
    public static void drawPanel(float x, float y, float w, float h, boolean header, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);

        for (int i = 3; i >= 1; i--) {
            DrawUtil.drawRound(x - i, y - i, w + i * 2f, h + i * 2f, RADIUS + i,
                    ColorProvider.rgba(0, 0, 0, 18 * i));
        }

        DrawUtil.drawRound(x, y, w, h, RADIUS,
                ColorProvider.setAlpha(BG_TOP, aInt),
                ColorProvider.setAlpha(BG_BOTTOM, aInt),
                ColorProvider.setAlpha(BG_BOTTOM, aInt),
                ColorProvider.setAlpha(BG_TOP, aInt));

        DrawUtil.drawRound(x, y, w, h, RADIUS, ColorProvider.rgba(255, 255, 255, (int) (10 * alpha)));

        if (header) {
            DrawUtil.drawRound(x, y, w, HEADER_HEIGHT, new Vector4f(RADIUS, 0f, 0f, RADIUS),
                    ColorProvider.rgba(255, 255, 255, (int) (7 * alpha)));

            int theme = ColorProvider.getThemeColor();
            DrawUtil.drawRound(x + 5, y + HEADER_HEIGHT - 0.5f, w - 10, 0.5f, 0,
                    ColorProvider.setAlpha(theme, (int) (55 * alpha)));
        }

        Builder.border()
                .size(new SizeState(w + 0.5f, h + 0.25f))
                .radius(new QuadRadiusState(RADIUS))
                .color(new QuadColorState(ColorProvider.setAlpha(BORDER, aInt)))
                .thickness(0.7f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
    }

    /**
     * Компактный стеклянный "чип" (уведомления, ватермарка): мягкая тень,
     * градиентная заливка, highlight и тонкая обводка.
     */
    public static void drawChip(float x, float y, float w, float h, float radius, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);

        DrawUtil.drawRound(x - 1, y - 1, w + 2, h + 2, radius + 1, ColorProvider.rgba(0, 0, 0, 24));
        DrawUtil.drawRound(x - 2, y - 2, w + 4, h + 4, radius + 2, ColorProvider.rgba(0, 0, 0, 12));

        DrawUtil.drawRound(x, y, w, h, radius,
                ColorProvider.setAlpha(BG_TOP, aInt),
                ColorProvider.setAlpha(BG_BOTTOM, aInt),
                ColorProvider.setAlpha(BG_BOTTOM, aInt),
                ColorProvider.setAlpha(BG_TOP, aInt));

        DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.rgba(255, 255, 255, (int) (8 * alpha)));

        Builder.border()
                .size(new SizeState(w + 0.5f, h + 0.25f))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(ColorProvider.setAlpha(BORDER, aInt)))
                .thickness(0.6f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
    }

    /**
     * Заголовок DLC: справа иконка в цвете темы, текст-градиент от тонированного
     * темы белого к чистому белому.
     */
    public static void drawHeader(float x, float y, float w, String title, String icon, float alpha) {
        if (alpha <= 0.01f) return;
        int theme = ColorProvider.getThemeColor();
        int light = interpolate(theme, 0xFFFFFFFF, 0.78f);

        float iconW = icon == null ? 0f : Fonts.ICONS2.get().getWidth(icon, 8f);
        float textW = Fonts.SFMEDIUM.get().getWidth(title, 8f);
        float titleX = x + 4.5f + Math.max(0f, ((w - iconW) - textW) / 2f - 4.5f);

        drawGradientText(Fonts.SFMEDIUM.get(), title, titleX, y + 3.75f, 8f, 0xFFFFFFFF, light, alpha);

        if (icon != null) {
            float iconX = x + w - iconW - 3.5f;
            DrawUtil.drawText(Fonts.ICONS2.get(), icon, iconX, y + 4.0f, applyAlpha(theme, alpha), 8f);
        }
    }

    /**
     * Посимвольная интерполяция от color1 к color2 (приближение градиента).
     */
    public static void drawGradientText(MsdfFont font, String text, float x, float y, float size,
                                        int color1, int color2, float alpha) {
        if (text == null || text.isEmpty()) return;
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = text.length() == 1 ? 0.5f : (float) i / (text.length() - 1);
            int c = ColorProvider.interpolateColor(color1, color2, t);
            DrawUtil.drawText(font, ch, cx, y, applyAlpha(c, alpha), size);
            cx += font.getWidth(ch, size);
        }
    }

    public static int interpolate(int c1, int c2, float t) {
        return ColorProvider.interpolateColor(c1, c2, t);
    }

    public static int darker(int color, float factor) {
        int r = (int) (ColorProvider.red(color) * (1f - factor));
        int g = (int) (ColorProvider.green(color) * (1f - factor));
        int b = (int) (ColorProvider.blue(color) * (1f - factor));
        return ColorProvider.rgba(r, g, b, ColorProvider.alpha(color));
    }

    public static int applyAlpha(int color, float alpha) {
        if (alpha >= 1f) return color;
        int a = (int) (ColorProvider.alpha(color) * alpha);
        return ColorProvider.setAlpha(color, a);
    }
}