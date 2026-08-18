package tech.onetap.ui.csgui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private final SliderSetting setting;
    private final Animation sliderAnimation = new Animation(Easing.CUBIC_OUT, 180);
    private boolean dragging;

    public SliderComponent(SliderSetting setting) {
        this.setting = setting;
    }

    public SliderSetting getSetting() {
        return setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float visibility = updateVisibility(setting.visible.get());
        float renderAlpha = alpha * visibility;
        if (renderAlpha <= 0.01f) {
            return;
        }

        float valueRange = (float) (setting.getMax() - setting.getMin());
        float valueProgress = valueRange <= 0.0f ? 0.0f : (float) ((setting.getValue() - setting.getMin()) / valueRange);
        valueProgress = MathHelper.clamp(valueProgress, 0.0f, 1.0f);
        float trackX = x;
        float trackY = y + height - 5.0f;
        float trackWidth = width;
        float fillWidth = trackWidth * valueProgress;

        sliderAnimation.run(fillWidth);

        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, trackX, trackY - 3.0f, trackWidth, 8.0f);
        if (hovered || dragging) {
            CursorManager.requestHand();
        }

        if (dragging) {
            double mouseProgress = MathHelper.clamp((mouseX - trackX) / trackWidth, 0.0, 1.0);
            double value = setting.getMin() + (setting.getMax() - setting.getMin()) * mouseProgress;
            setting.setValue(roundToStep(value, setting.getStep()));
        }

        int alphaInt = (int) (255 * renderAlpha);
        String valueText = formatNumber(setting.getValue());

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), setting.getName() + ": " + valueText, x, y, ColorProvider.rgba(255, 255, 255, alphaInt), 7.8f);
        DrawUtil.drawRound(trackX, trackY, trackWidth, 4f, 0.5f, ColorProvider.rgba(95, 105, 105, (int) (75 * renderAlpha)));
        DrawUtil.drawRound(trackX, trackY, MathHelper.clamp(sliderAnimation.getValue(), 0.0f, trackWidth), 4f, 0.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt));

        float knobX = trackX + MathHelper.clamp(sliderAnimation.getValue(), 0.0f, trackWidth);
        DrawUtil.drawRound(knobX - 3.0f, trackY - 1.25f, 6.0f, 6.5f, 2f, ColorProvider.rgba(225, 225, 225, alphaInt));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getVisibilityProgress() <= 0.05f) {
            return false;
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, x, y + height - 8.0f, width, 12.0f)) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    private double roundToStep(double value, double step) {
        if (step <= 0.0) {
            return MathHelper.clamp(value, setting.getMin(), setting.getMax());
        }

        double rounded = Math.round(value / step) * step;
        int scale = 0;
        if (step < 1.0) {
            scale = (int) Math.ceil(Math.abs(Math.log10(step)));
        }
        double clamped = MathHelper.clamp(rounded, setting.getMin(), setting.getMax());
        return BigDecimal.valueOf(clamped).setScale(Math.max(scale, 2), RoundingMode.HALF_UP).doubleValue();
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
