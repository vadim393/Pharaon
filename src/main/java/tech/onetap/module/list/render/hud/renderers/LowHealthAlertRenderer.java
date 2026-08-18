package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class LowHealthAlertRenderer {
    private final Interface owner;
    private final Animation lowHpAlertAnimation = new Animation(Easing.EXPO_OUT, 300);

    public LowHealthAlertRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        float hp = owner.mc.player.getHealth() + owner.mc.player.getAbsorptionAmount();
        float threshold = owner.getLowHpAlertThresholdSetting().getFloatValue();
        boolean shouldShow = hp <= threshold && !owner.mc.player.isDead();

        lowHpAlertAnimation.run(shouldShow ? 1 : 0);
        float anim = (float) lowHpAlertAnimation.getValue();
        if (anim <= 0.01f) return;

        int alphaInt = (int) (255 * anim);
        String text = String.format(java.util.Locale.US, "Критическое здоровье: %.1f HP", hp);
        int iconColor = ColorProvider.rgba(222, 222, 222, alphaInt);

        if (owner.getHudStyleSetting().is("Pharaon")) {
            float textWidth = Fonts.SFBOLD.get().getWidth(text, 8.6f);
            float width = 20f + textWidth + 7f;
            float height = 20f;
            float x = (owner.mc.getWindow().getScaledWidth() - width) / 2f;
            float y = 100f;

            context.getMatrices().push();
            context.getMatrices().translate(x + width / 2f, y + height / 2f, 0);
            context.getMatrices().scale(anim, anim, 1f);
            context.getMatrices().translate(-(x + width / 2f), -(y + height / 2f), 0);

            DrawUtil.drawRound(x, y, width, height, 3f, 2f, ColorProvider.rgba(13, 16, 23, alphaInt));
            DrawUtil.drawRound(x + 4f, y + 4f, 12f, 12f, 2.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt));
            DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue90f", x + 5.75f, y + 5.65f, iconColor, 10.5f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), text, x + 19f, y + 6.1f, ColorProvider.rgba(200, 200, 200, alphaInt), 8.6f);

            context.getMatrices().pop();
            return;
        }

        if (owner.isAxiomStyle()) {
            float textWidth = Fonts.RUBIK.get().getWidth(text, 7.2f);
            float width = 22f + textWidth + 8f;
            float height = 16.5f;
            float x = (owner.mc.getWindow().getScaledWidth() - width) / 2f;
            float y = 100f;

            context.getMatrices().push();
            context.getMatrices().translate(x + width / 2f, y + height / 2f, 0);
            context.getMatrices().scale(anim, anim, 1f);
            context.getMatrices().translate(-(x + width / 2f), -(y + height / 2f), 0);

            owner.drawBackground(x, y, width, height, 5f, alphaInt);
            owner.drawAxiomAccent(x + 4f, y + 3.25f, 13.5f, height - 6.5f, 3.6f, alphaInt);
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "G", x + 6.05f, y + 4.35f, iconColor, 8.5f);
            DrawUtil.drawText(Fonts.RUBIK.get(), text, x + 22f, y + 3.95f, owner.getAxiomPrimaryTextColor(alphaInt), 7.2f);

            context.getMatrices().pop();
            return;
        }

        String iconCode = "G";
        float textWidth = Fonts.SFMEDIUM.get().getWidth(text, 7f);
        float iconWidth = Fonts.ICONS_NURIK.get().getWidth(iconCode, 9f);
        float width = iconWidth + textWidth + 22f;
        float height = 14.5f;
        float x = (owner.mc.getWindow().getScaledWidth() - width) / 2f;
        float y = 100f;

        context.getMatrices().push();
        context.getMatrices().translate(x + width / 2f, y + height / 2f, 0);
        context.getMatrices().scale(anim, anim, 1f);
        context.getMatrices().translate(-(x + width / 2f), -(y + height / 2f), 0);

        owner.drawBackground(x, y, width, height, 4, alphaInt);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), iconCode, x + 5, y + 4, iconColor, 9f);
        DrawUtil.drawRound(x + 18f, y + 2.5f, 0.5f, height - 5f, 0, ColorProvider.rgba(255, 255, 255, (int) (120 * anim)));
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x + 23f, y + 3f, ColorProvider.rgba(255, 255, 255, alphaInt), 7f);

        context.getMatrices().pop();
    }
}
