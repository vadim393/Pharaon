package tech.onetap.ui.csgui.component;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.Module;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;

import java.util.ArrayList;
import java.util.List;

public class ModuleComponent extends Component {
    private static final float HEADER_HEIGHT = 23.0f;
    private static final float CONTENT_PADDING = 6.0f;
    private static final float CONTENT_SPACING = 4.0f;

    @Getter
    private final Module module;
    private final Animation enabledAnimation = new Animation(Easing.CUBIC_OUT, 200);
    private final Animation expandAnimation = new Animation(Easing.CUBIC_OUT, 220);
    private final Animation dotsAnimation = new Animation(Easing.CUBIC_OUT, 180);
    private final List<Component> components = new ArrayList<>();
    private boolean expanded;

    public ModuleComponent(Module module) {
        this.module = module;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
                components.add(new BooleanComponent(booleanSetting));
            } else if (setting instanceof ModeListSetting modeListSetting) {
                components.add(new ModeListComponent(modeListSetting));
            } else if (setting instanceof ModeSetting modeSetting) {
                components.add(new ModeComponent(modeSetting));
            } else if (setting instanceof SliderSetting sliderSetting) {
                components.add(new SliderComponent(sliderSetting));
            }
        }
    }

    public float getHeight() {
        expandAnimation.run(expanded ? 1f : 0f);
        float expandValue = (float) expandAnimation.getValue();
        return HEADER_HEIGHT + getExpandedContentHeight() * expandValue;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        enabledAnimation.run(module.isEnabled());
        expandAnimation.run(expanded ? 1f : 0f);
        dotsAnimation.run(expanded ? 1f : 0f);
        if (alpha <= 0.01f) {
            return;
        }
        boolean canExpand = !components.isEmpty();
        float headerHeight = HEADER_HEIGHT;
        float expandedHeight = getExpandedContentHeight();
        float expandValue = (float) expandAnimation.getValue();
        float bodyHeight = expandedHeight * expandValue;
        float totalHeight = headerHeight + bodyHeight;

        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, totalHeight);
        if (hovered) {
            CursorManager.requestHand();
        }

        int alphaInt = (int) (255 * alpha);
        float enabledValue = (float) enabledAnimation.getValue();
        int disabledBaseColor = ColorProvider.rgba(15, 15, 15, 42 * alpha);
        int enabledBaseColor = ColorProvider.setAlpha(ColorProvider.brighter(ColorProvider.getThemeColor(), 1.25f), (int) (42 * alpha));
        int baseColor = ColorProvider.interpolateColor(disabledBaseColor, enabledBaseColor, enabledValue);
        int borderColor = ColorProvider.rgba(255, 255, 255, 85 * alpha);
        int bodyColor = ColorProvider.rgba(0, 0, 0, (int) (65 * alpha * expandValue));
        int textColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(135, 135, 135, alphaInt),
                ColorProvider.rgba(255, 255, 255, alphaInt),
                enabledValue
        );
        if (enabledValue > 0.01f) {
            int[] glowColors = ColorProvider.getOrbitalRect(
                    ColorProvider.getThemeColor(),
                    ColorProvider.getThemeColorTwo(),
                    500.0,
                    (int) (52 * alpha * enabledValue)
            );
            float glow = 3.75f;
            Builder.glow()
                    .size(new SizeState(width + glow * 2f - 2f, totalHeight + glow * 2f - 2f))
                    .radius(new QuadRadiusState(6.0f))
                    .color(new QuadColorState(glowColors[0], glowColors[1], glowColors[2], glowColors[3]))
                    .glowRadius(glow)
                    .softness(0f)
                    .intensity(1.15f)
                    .additive(true)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x - glow + 1f, y - glow + 1f, 0f);
        }

        DrawUtil.drawRound(x, y, width, totalHeight, 6.0f, baseColor);

        if (bodyHeight > 0.5f) {
            DrawUtil.drawRound(x + 1.0f, y + headerHeight, width - 2.0f, bodyHeight, 0.0f, bodyColor);
        }

        Builder.border()
                .size(new SizeState(width + 1.0f, totalHeight + 1.0f))
                .radius(new QuadRadiusState(6.5f))
                .color(new QuadColorState(borderColor))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x - 0.5f, y - 0.5f);

        float checkSize = 12.0f;
        float dotsWidth = Fonts.SFMEDIUM.get().getWidth("...", 9.0f);
        float dotsX = x + width - dotsWidth - 9.0f;
        float dotsBoxWidth = dotsWidth + 8.0f;
        float dotsBoxHeight = 12.0f;
        float dotsBoxX = dotsX - 5.0f;
        float dotsBoxY = y + (headerHeight - dotsBoxHeight) / 2.0f;
        float checkX = x + width - checkSize - 9.0f;
        float checkY = y + (headerHeight - checkSize) / 2.0f;
        if (canExpand) {
            dotsX = checkX - 9.0f - dotsWidth;
            dotsBoxX = dotsX - 4.0f;
        }
        int checkBgColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(95, 105, 105, (int) (85 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt),
                enabledValue
        );

        DrawUtil.drawRound(checkX, checkY, checkSize, checkSize, 2.0f, checkBgColor);

        Builder.border()
                .size(new SizeState(checkSize + 1.0f, checkSize + 1.0f))
                .radius(new QuadRadiusState(2.5f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, 75 * alpha)))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), checkX - 0.5f, checkY - 0.5f);

        float dotsValue = (float) dotsAnimation.getValue();
        if (canExpand && dotsValue > 0.01f) {
            Builder.border()
                    .size(new SizeState(dotsBoxWidth + 1.0f, dotsBoxHeight + 1.0f))
                    .radius(new QuadRadiusState(3.5f))
                    .color(new QuadColorState(ColorProvider.setAlpha(ColorProvider.brighter(ColorProvider.getThemeColor(),1.3f), (int) (255 * alpha * dotsValue))))
                    .thickness(0.1f)
                    .smoothness(1f, 0.5f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), dotsBoxX + 0.5f, dotsBoxY - 0.5f);
        }

        int checkIconAlpha = (int) (alphaInt * enabledValue);
        DrawUtil.drawText(Fonts.ICONS2.get(), "S", checkX + 1.5f, checkY - 0.5f, ColorProvider.rgba(255, 255, 255, checkIconAlpha), 11.5f);

        if (canExpand) {
            DrawUtil.drawText(Fonts.CELESTIAL.get(), "...", dotsX, dotsBoxY - 1.8f, ColorProvider.rgba(255, 255, 255, alphaInt), 11.0f);
        }
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), module.getName(), x + 6.0f, y + 7.0f, textColor, 10.0f);

        if (bodyHeight > 0.5f && canExpand) {
            float currentY = y + headerHeight + CONTENT_PADDING - 3.0f;
            float componentWidth = width - CONTENT_PADDING * 2.0f;

            for (Component component : components) {
                float visibleProgress = updateComponentVisibility(component);
                if (visibleProgress <= 0.01f) {
                    continue;
                }
                float componentHeight = getComponentHeight(component);
                component.setBounds(x + CONTENT_PADDING, currentY, componentWidth, componentHeight);
                component.setAlpha(alpha * expandValue * visibleProgress);
                component.render(context, mouseX, mouseY, delta);
                currentY += (componentHeight + CONTENT_SPACING) * visibleProgress;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        getHeight();
        if (button == 1 && !components.isEmpty() && HoverUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            expanded = !expanded;
            return true;
        }
        if ((button == 0 || button == 1) && expandAnimation.getValue() > 0.01f) {
            for (Component component : components) {
                if (component.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            module.toggle();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (Component component : components) {
            component.mouseReleased(mouseX, mouseY, button);
        }
    }

    private float getExpandedContentHeight() {
        float contentHeight = 0.0f;
        for (Component component : components) {
            contentHeight += (getComponentHeight(component) + CONTENT_SPACING) * updateComponentVisibility(component);
        }
        if (contentHeight <= 0.0f) {
            return 0.0f;
        }
        return CONTENT_PADDING * 2.0f + contentHeight - CONTENT_SPACING - 3.0f;
    }

    private float updateComponentVisibility(Component component) {
        if (component instanceof BooleanComponent booleanComponent) {
            return booleanComponent.updateVisibility(booleanComponent.getSetting().visible.get());
        }
        if (component instanceof ModeComponent modeComponent) {
            return modeComponent.updateVisibility(modeComponent.getSetting().visible.get());
        }
        if (component instanceof ModeListComponent modeListComponent) {
            return modeListComponent.updateVisibility(modeListComponent.getSetting().visible.get());
        }
        if (component instanceof SliderComponent sliderComponent) {
            return sliderComponent.updateVisibility(sliderComponent.getSetting().visible.get());
        }
        return 0.0f;
    }

    private float getComponentHeight(Component component) {
        if (component instanceof BooleanComponent) {
            return 12.0f;
        }
        if (component instanceof ModeComponent modeComponent) {
            return modeComponent.getPreferredHeight();
        }
        if (component instanceof ModeListComponent modeListComponent) {
            return modeListComponent.getPreferredHeight();
        }
        if (component instanceof SliderComponent) {
            return 18.0f;
        }
        return 0.0f;
    }
}
