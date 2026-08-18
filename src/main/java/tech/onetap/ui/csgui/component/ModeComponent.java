package tech.onetap.ui.csgui.component;

import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class ModeComponent extends Component {
    private final ModeSetting setting;
    private final Animation switchAnimation = new Animation(Easing.CUBIC_OUT, 110);

    public ModeComponent(ModeSetting setting) {
        this.setting = setting;
    }

    public ModeSetting getSetting() {
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
        float boxY = y + 16.0f;
        switchAnimation.run(1f);
        boolean leftHovered = HoverUtil.isHovered(mouseX, mouseY, x, boxY, 16.0f, 14.0f);
        boolean rightHovered = HoverUtil.isHovered(mouseX, mouseY, x + width - 16.0f, boxY, 16.0f, 14.0f);
        boolean centerHovered = HoverUtil.isHovered(mouseX, mouseY, x + 16.0f, boxY, width - 32.0f, 14.0f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), setting.getName(), x, y + 2, ColorProvider.rgba(215, 215, 215, alphaInt), 7.6f);
        DrawUtil.drawRound(x - 0.5f, boxY - 0.5f, width + 1.0f, 15.0f, 3.5f, ColorProvider.rgba(255, 255, 255, (int) (72 * renderAlpha)));
        DrawUtil.drawRound(x, boxY, width, 14.0f, 3.0f, ColorProvider.rgba(18, 18, 21, (int) (108 * renderAlpha)));
        DrawUtil.drawRound(x + 16.0f, boxY + 0.5f, width - 32.0f, 13.0f, 2.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (88 * renderAlpha)));


        if (leftHovered) {
            DrawUtil.drawRound(x + 0.5f, boxY + 0.5f, 15.0f, 13.0f, 2.5f, ColorProvider.rgba(255, 255, 255, (int) (25 * renderAlpha)));
        }
        if (rightHovered) {
            DrawUtil.drawRound(x + width - 15.5f, boxY + 0.5f, 15.0f, 13.0f, 2.5f, ColorProvider.rgba(255, 255, 255, (int) (25 * renderAlpha)));
        }

        String displayMode = trimToWidth(setting.getValue(), width - 46.0f, 7.4f);
        float textX = x + width / 2.0f - Fonts.SFMEDIUM.get().getWidth(displayMode, 7.4f) / 2.0f;
        float textOffset = (1.0f - (float) switchAnimation.getValue()) * 4.0f;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), displayMode, textX, boxY + 2.3f - textOffset, ColorProvider.rgba(255, 255, 255, alphaInt), 7.4f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "<", x + 5.0f, boxY + 2.2f, ColorProvider.rgba(190, 190, 190, alphaInt), 7.6f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), ">", x + width - 10.0f, boxY + 2.2f, ColorProvider.rgba(190, 190, 190, alphaInt), 7.6f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getVisibilityProgress() <= 0.05f) {
            return false;
        }
        if (button != 0 && button != 1) {
            return false;
        }

        float boxY = y + 16.0f;
        boolean leftHovered = HoverUtil.isHovered(mouseX, mouseY, x, boxY, 16.0f, 14.0f);
        boolean rightHovered = HoverUtil.isHovered(mouseX, mouseY, x + width - 16.0f, boxY, 16.0f, 14.0f);
        boolean centerHovered = HoverUtil.isHovered(mouseX, mouseY, x + 16.0f, boxY, width - 32.0f, 14.0f);
        if (!leftHovered && !rightHovered && !centerHovered) {
            return false;
        }

        if (leftHovered) {
            int index = setting.getIndex() - 1;
            if (index < 0) {
                index = setting.getModes().size() - 1;
            }
            setting.setIndex(index);
            switchAnimation.setValue(0f);
            return true;
        }

        if (rightHovered) {
            setting.cycle();
            switchAnimation.setValue(0f);
            return true;
        }

        if (button == 0) {
            setting.cycle();
            switchAnimation.setValue(0f);
            return true;
        }

        int index = setting.getIndex() - 1;
        if (index < 0) {
            index = setting.getModes().size() - 1;
        }
        setting.setIndex(index);
        switchAnimation.setValue(0f);
        return true;

    }

    public float getPreferredHeight() {
        return 30.0f;
    }

    private String trimToWidth(String text, float maxWidth, float fontSize) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Fonts.SFMEDIUM.get().getWidth(text, fontSize) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        String result = text;
        while (!result.isEmpty() && Fonts.SFMEDIUM.get().getWidth(result + ellipsis, fontSize) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }
}
