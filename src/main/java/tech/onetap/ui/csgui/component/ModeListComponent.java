package tech.onetap.ui.csgui.component;

import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class ModeListComponent extends Component {
    private final ModeListSetting setting;

    public ModeListComponent(ModeListSetting setting) {
        this.setting = setting;
    }

    public ModeListSetting getSetting() {
        return setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float visibility = updateVisibility(setting.visible.get());
        float renderAlpha = alpha * visibility;
        if (renderAlpha <= 0.01f) {
            return;
        }

        int alphaInt = (int) (255 * renderAlpha);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), setting.getName(), x, y - 1.5f, ColorProvider.rgba(215, 215, 215, alphaInt), 7.6f);

        float boxY = y + 10.0f;
        float boxHeight = height - 9.0f;
        DrawUtil.drawRound(x - 0.5f, boxY - 0.5f, width + 1.0f, boxHeight + 1.0f, 3.5f, ColorProvider.rgba(255, 255, 255, (int) (65 * renderAlpha)));
        DrawUtil.drawRound(x, boxY, width, boxHeight, 3.0f, ColorProvider.rgba(14, 14, 16, (int) (95 * renderAlpha)));

        float currentY = boxY + 3.0f;
        for (int i = 0; i < setting.getSettings().size(); i++) {
            BooleanSetting option = setting.getSettings().get(i);
            option.getAnimation().run(option.getValue());



            int textColor = ColorProvider.interpolateColor(
                    ColorProvider.rgba(190, 190, 190, alphaInt),
                    ColorProvider.rgba(255, 255, 255, alphaInt),
                    option.getAnimation().getValue()
            );
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), option.getName(), x + 6.0f, currentY + 2.1f, textColor, 7.4f);

            float checkSize = 9.0f;
            float checkX = x + width - checkSize - 6.0f;
            float checkY = currentY + 1.5f;
            int boxColor = ColorProvider.interpolateColor(
                    ColorProvider.rgba(95, 105, 105, (int) (75 * renderAlpha)),
                    ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt),
                    option.getAnimation().getValue()
            );

            DrawUtil.drawRound(checkX, checkY, checkSize, checkSize, 2.0f, boxColor);
            Builder.border()
                    .size(new SizeState(checkSize + 1.0f, checkSize + 1.0f))
                    .radius(new QuadRadiusState(2.5f))
                    .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, (int) (70 * renderAlpha))))
                    .thickness(0.2f)
                    .smoothness(1f, 0.5f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), checkX - 0.5f, checkY - 0.5f);

            int iconAlpha = (int) (alphaInt * option.getAnimation().getValue());
            DrawUtil.drawText(Fonts.ICONS2.get(), "S", checkX + 0.8f, checkY - 1.1f, ColorProvider.rgba(255, 255, 255, iconAlpha), 9.5f);

            currentY += 13.0f;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getVisibilityProgress() <= 0.05f || button != 0) {
            return false;
        }

        float currentY = y + 12.0f;
        for (BooleanSetting option : setting.getSettings()) {
            if (HoverUtil.isHovered(mouseX, mouseY, x + 3.0f, currentY, width - 6.0f, 12.0f)) {
                option.toggle();
                return true;
            }
            currentY += 13.0f;
        }

        return false;
    }

    public float getPreferredHeight() {
        return 13.0f + setting.getSettings().size() * 13.0f;
    }
}
