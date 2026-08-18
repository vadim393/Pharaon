package tech.onetap.ui.csgui.component;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.settings.BooleanSetting;
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

public class BooleanComponent extends Component {
    @Getter
    private final BooleanSetting setting;
    private final Animation enabledAnimation = new Animation(Easing.CUBIC_OUT, 200);

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float visibility = updateVisibility(setting.visible.get());
        enabledAnimation.run(setting.getValue());
        float renderAlpha = alpha * visibility;
        if (renderAlpha <= 0.01f) {
            return;
        }

        int alphaInt = (int) (255 * renderAlpha);
        float enabledValue = (float) enabledAnimation.getValue();
        int textColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(165, 165, 165, alphaInt),
                ColorProvider.rgba(255, 255, 255, alphaInt),
                enabledValue
        );

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), setting.getName(), x, y + 2.0f, textColor, 7.8f);

        float boxSize = 11.0f;
        float boxX = x + width - boxSize;
        float boxY = y + (height - boxSize) / 2.0f;
        int boxColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(95, 105, 105, (int) (80 * renderAlpha)),
                ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt),
                enabledValue
        );

        DrawUtil.drawRound(boxX, boxY, boxSize, boxSize, 2.0f, boxColor);
        Builder.border()
                .size(new SizeState(boxSize + 1.0f, boxSize + 1.0f))
                .radius(new QuadRadiusState(2.5f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, (int) (70 * renderAlpha))))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), boxX - 0.5f, boxY - 0.5f);

        int iconAlpha = (int) (alphaInt * enabledValue);
        DrawUtil.drawText(Fonts.ICONS2.get(), "S", boxX + 1.15f, boxY - 1.0f, ColorProvider.rgba(255, 255, 255, iconAlpha), 11.0f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getVisibilityProgress() <= 0.05f) {
            return false;
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            setting.toggle();
            return true;
        }
        return false;
    }
}
