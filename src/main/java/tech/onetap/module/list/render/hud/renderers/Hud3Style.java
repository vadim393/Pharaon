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
 * Общие элементы HUD-стиля Hud3: тёмные фиолетовые панели с размытым фоном,
 * тонкой обводкой в цвет темы и заголовком; элемент повторяет визуальный язык
 * Hud3 Dliledzhq (bg rgba(30,25,40), border rgba(120,80,160), blur-подложка).
 */
public final class Hud3Style {

    public static final int PANEL_BG = ColorProvider.rgba(30, 25, 40, 255);
    public static final int CONTENT_BG = ColorProvider.rgba(30, 25, 40, 255);
    public static final int BORDER_PURPLE = ColorProvider.rgba(120, 80, 160, 255);
    public static final int GLOW_PURPLE = ColorProvider.rgba(120, 80, 160, 100);
    public static final int TEXT = ColorProvider.rgba(235, 238, 245, 255);
    public static final int TEXT_DIM = ColorProvider.rgba(150, 156, 170, 255);

    public static final float RADIUS = 5f;
    public static final float HEADER_HEIGHT = 15f;
    public static final float ITEM_SPACING = 11f;

    public static final int BG = PANEL_BG;
    public static final int BORDER = BORDER_PURPLE;
    public static final int GLOW = GLOW_PURPLE;

    private static final int BLUR_BG = ColorProvider.rgba(0, 0, 0, 255);

    private Hud3Style() {
    }

    /**
     * Компактный "чип" Hud3 (тотем-каунтер, мини-элементы): blur-подложка,
     * фиолетовая заливка, обводка и inner-glow в цвет темы.
     */
    public static void drawChip(float x, float y, float w, float h, float radius, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);

        DrawUtil.drawRoundBlur(x, y, w, h + 2.3f, radius, ColorProvider.setAlpha(BLUR_BG, (int) (255 * alpha * 0.45f)), 15f);

        DrawUtil.drawRound(x, y, w, h, radius,
                panelTop(alpha), panelBottom(alpha), panelBottom(alpha), panelTop(alpha));

        int theme = ColorProvider.getThemeColor();

        DrawUtil.drawRoundBlur(x, y + HEADER_HEIGHT, w, h - HEADER_HEIGHT + 2.3f,
                radius, ColorProvider.setAlpha(BLUR_BG, (int) (255 * alpha * 0.45f)), 15f);
        DrawUtil.drawRoundBlur(x, y, w, HEADER_HEIGHT, radius,
                ColorProvider.setAlpha(BLUR_BG, (int) (255 * alpha)), 15f);
    }

    /**
     * Панель Hud3: размытая подложка (45% прозрачности чёрного), фиолетовая
     * заливка, обводка и inner-glow в цвет темы. alpha — глобальная 0..1.
     */
    public static void drawPanel(float x, float y, float w, float h, boolean header, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);
        float radius = RADIUS;

        DrawUtil.drawRound(x, y, w, h + 2.3f, radius,
                ColorProvider.rgba(30, 25, 40, (int) (175 * alpha)));
        DrawUtil.drawRoundBlur(x, y, w, h + 2.3f, radius, ColorProvider.setAlpha(BLUR_BG, (int) (255 * alpha * 0.45f)), 15f);

        if (header) {
            DrawUtil.drawRoundBlur(x, y, w, HEADER_HEIGHT, radius,
                    ColorProvider.setAlpha(BLUR_BG, (int) (255 * alpha)), 15f);
        }
    }

    /**
     * Заголовок Hud3: текст в цвете темы слева, иконка в цвете темы справа.
     */
    public static void drawHeader(float x, float y, float w, String title, String icon, float alpha) {
        if (alpha <= 0.01f) return;
        int theme = ColorProvider.getThemeColor();
        int textColor = titleColor(alpha);

        float iconW = icon == null ? 0f : Fonts.ICONS_NURIK.get().getWidth(icon, 10f);
        float titleW = Fonts.SFMEDIUM.get().getWidth(title, 7f);
        float gap = (icon != null && !title.isEmpty()) ? 5f : 0f;
        float blockW = titleW + gap + iconW;
        float blockX = x + Math.max(4.5f, (w - blockW) / 2f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), title, blockX, y + 3.0f,
                ColorProvider.setAlpha(textColor, (int) (255 * alpha)), 7f);

        if (icon != null) {
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), icon, blockX + titleW + gap, y + 2.5f,
                    ColorProvider.setAlpha(theme, (int) (255 * alpha)), 10f);
        }
    }

    /**
     * Кнопка-иконка Hud3: заливка и обводка в цвет темы, иконка внутри.
     */
    public static void drawIconButton(float x, float y, float size, String icon, float alpha) {
        if (alpha <= 0.01f) return;
        int theme = ColorProvider.getThemeColor();

        DrawUtil.drawRound(x, y, size, size, size / 2f,
                ColorProvider.setAlpha(theme, (int) (35 * alpha)));
        Builder.border()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(size / 2f))
                .color(new QuadColorState(ColorProvider.setAlpha(theme, (int) (180 * alpha))))
                .thickness(0.7f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);

        float iconW = Fonts.ICONS2.get().getWidth(icon, 7f);
        DrawUtil.drawText(Fonts.ICONS2.get(), icon, x + (size - iconW) / 2f, y + 2.75f,
                ColorProvider.setAlpha(theme, (int) (255 * alpha)), 7f);
    }

    /**
     * Вертикальный разделитель между элементами в цвете темы.
     */
    public static void drawSeparator(float x, float y, float height, float alpha) {
        int theme = ColorProvider.getThemeColor();
        DrawUtil.drawRound(x, y, 1f, height, 0.5f, ColorProvider.setAlpha(theme, (int) (70 * alpha)));
    }

    /**
     * Текст Hud3: основной цвет с возможной подсветкой в цвет темы.
     */
    public static void drawText(MsdfFont font, String text, float x, float y, float size, float alpha) {
        DrawUtil.drawText(font, text, x, y, ColorProvider.setAlpha(TEXT, (int) (255 * alpha)), size);
    }

    public static void drawText(MsdfFont font, String text, float x, float y, float size, int color, float alpha) {
        DrawUtil.drawText(font, text, x, y, ColorProvider.setAlpha(color, (int) (255 * alpha)), size);
    }

    private static int panelTop(float alpha) {
        return ColorProvider.setAlpha(ColorProvider.rgba(36, 31, 48, 255), (int) (185 * alpha));
    }

    private static int panelBottom(float alpha) {
        return ColorProvider.setAlpha(ColorProvider.rgba(24, 20, 32, 255), (int) (155 * alpha));
    }

    private static int titleColor(float alpha) {
        int theme = ColorProvider.getThemeColor();
        return ColorProvider.interpolateColor(theme, 0xFFFFFFFF, 0.35f);
    }

    public static int applyAlpha(int color, float alpha) {
        if (alpha >= 1f) return color;
        int a = (int) (ColorProvider.alpha(color) * alpha);
        return ColorProvider.setAlpha(color, a);
    }
}
