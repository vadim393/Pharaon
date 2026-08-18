package tech.onetap.module.list.render.hud.renderers;

import org.joml.Matrix4f;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

/**
 * Общие элементы HUD-стиля Funtime: тёмная панель с многослойной тенью,
 * тонкой белой подсветкой сверху и акцентными иконками со свечением.
 */
public final class FuntimeHud {

    public static final int BG = ColorProvider.rgba(30, 30, 35, 255);
    public static final int TEXT = ColorProvider.rgba(210, 210, 215, 255);
    public static final int SEP = ColorProvider.rgba(90, 90, 100, 225);
    public static final int SHADOW = ColorProvider.rgba(0, 0, 0, 90);
    public static final float RADIUS = 8f;

    private FuntimeHud() {
    }

    /**
     * Тень (x-i, y-i, w+2i, h+2i, radius+i, black alpha 25*i), заливка панели
     * и тонкая белая подсветка (alpha 6) сверху — как в ватермарке Funtime.
     */
    public static void drawPanel(float x, float y, float w, float h, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);
        for (int i = 3; i >= 1; i--) {
            DrawUtil.drawRound(x - i, y - i, w + i * 2f, h + i * 2f, RADIUS + i,
                    ColorProvider.rgba(0, 0, 0, 25 * i));
        }
        DrawUtil.drawRound(x, y, w, h, RADIUS, ColorProvider.setAlpha(BG, aInt));
        DrawUtil.drawRound(x, y, w, h, RADIUS, ColorProvider.rgba(255, 255, 255, 6));
    }

    /**
     * Тень + заливка + белая подсветка вокруг иконки-акцента (как в ватермарке).
     */
    public static void drawIconCell(float x, float y, float size, int accentColor, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha);
        DrawUtil.drawRound(x, y, size, size, size / 2f, ColorProvider.setAlpha(accentColor, aInt));
    }

    public static void drawText(MsdfFont font, String text, float x, float y, int color, float size, float alpha) {
        DrawUtil.drawText(font, text, x, y, applyAlpha(color, alpha), size);
    }

    public static void drawGlowAccent(float x, float y, float size, int color, float alpha) {
        if (alpha <= 0.01f) return;
        int aInt = (int) (255 * alpha * 0.5f);
        Builder.glow()
                .size(new SizeState(size + 4, size + 4))
                .radius(new QuadRadiusState(size * 0.75f))
                .color(new QuadColorState(ColorProvider.setAlpha(color, aInt)))
                .glowRadius(6f)
                .softness(0f)
                .intensity(1.4f)
                .additive(false)
                .build()
                .render(x - 2, y - 2, 0);
    }

    public static int applyAlpha(int color, float alpha) {
        if (alpha >= 1f) return color;
        int a = (int) (ColorProvider.alpha(color) * alpha);
        return ColorProvider.setAlpha(color, a);
    }
}
