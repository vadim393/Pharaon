package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Список модулей в стиле Hud3: стопка строк без панели, с обводкой в цвет темы,
 * скруглением первой и последней строки и анимацией появления.
 */
public class ModuleListRenderer {
    private static final float ROW_HEIGHT = 12f;
    private static final float PADDING = 4f;
    private static final float PANEL_ANIM = 0.24f;
    private static final float ROW_ANIM = 0.22f;
    private static final float BORDER_THICKNESS = 1.0f;

    private final Interface owner;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, (int) (PANEL_ANIM * 1000));
    private final Map<Module, RowAnim> rowAnims = new HashMap<>();

    public ModuleListRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        Draggable drag = owner.getModuleListDrag();
        float x = drag.getX();
        float y = drag.getY();

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context, x, y, drag);
            return;
        }

        renderClassic(context, x, y, drag);
    }

    private void renderPouchOld(DrawContext context, float x, float y, Draggable drag) {
        List<Module> enabled = collectEnabledModules();
        boolean hasModules = !enabled.isEmpty();
        alpha.run(hasModules ? 1f : 0f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f || enabled.isEmpty()) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        sortedModules(enabled);
        List<Module> modules = new ArrayList<>(enabled);

        float screenWidth = owner.mc.getWindow().getScaledWidth();
        boolean leftSide = x < screenWidth / 2f;

        float maxWidth = 0f;
        for (Module module : modules) {
            maxWidth = Math.max(maxWidth, moduleWidth(module));
        }

        float totalHeight = modules.size() * ROW_HEIGHT;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            RowAnim anim = rowAnims.computeIfAbsent(module, k -> new RowAnim());
            anim.slide.run(0f);
            anim.offset.run(i * ROW_HEIGHT);
        }

        rowAnims.entrySet().removeIf(entry -> !modules.contains(entry.getKey()) && entry.getValue().slide.getValue() <= -14f);

        Scissor.push();
        Scissor.setFromComponentCoordinates((int) x, (int) y, (int) maxWidth, (int) totalHeight);

        int themeColor = ColorProvider.getThemeColor();
        int textColor = ColorProvider.rgba(255, 255, 255, (int) (255 * globalAlpha));
        int bgColor = ColorProvider.rgba(30, 25, 40, (int) (220 * globalAlpha));
        int outlineColor = ColorProvider.rgba(30, 25, 40, (int) (120 * globalAlpha));
        int last = modules.size() - 1;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            RowAnim anim = rowAnims.get(module);
            float slide = (float) anim.slide.getValue();
            float rowY = y + (float) anim.offset.getValue();
            float width = moduleWidth(module);

            float rowX = leftSide ? x + slide : x + maxWidth - width - slide;
            float drawY = rowY - 1f;
            float drawH = ROW_HEIGHT + 2f;

            boolean isFirst = i == 0;
            boolean isLast = i == last;
            float topRounded = isFirst ? 3f : 0f;
            float bottomRounded = isLast ? 3f : 0f;

            Vector4f radius = new Vector4f(topRounded, topRounded, bottomRounded, bottomRounded);
            DrawUtil.drawRound(rowX, drawY, width, drawH, radius, bgColor);

            Builder.border()
                    .size(new SizeState(width + 0.5f, drawH + 0.25f))
                    .radius(new QuadRadiusState(radius.x, radius.y, radius.z, radius.w))
                    .color(new QuadColorState(outlineColor))
                    .thickness(BORDER_THICKNESS)
                    .smoothness(0.5f, 1f)
                    .build()
                    .render(rowX, drawY);

            int perRowText = ColorProvider.interpolateColor(textColor, themeColor, 0.15f);
            DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), module.getName(), rowX + PADDING, rowY + ROW_HEIGHT / 2f - 3f, perRowText, 6f);
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(maxWidth);
        drag.setHeight(totalHeight);
    }

    private void renderClassic(DrawContext context, float x, float y, Draggable drag) {
        List<Module> enabled = collectEnabledModules();
        if (enabled.isEmpty()) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float width = 60f;
        float height = enabled.size() * 10f + 2f;

        for (int i = 0; i < enabled.size(); i++) {
            Module module = enabled.get(i);
            float rowY = y + i * 10f;
            DrawUtil.drawText(Fonts.SFREGULAR.get(), module.getName(), x + 3f, rowY, -1, 6.5f);
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private List<Module> collectEnabledModules() {
        List<Module> result = new ArrayList<>();
        for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
            if (module.isEnabled()) {
                result.add(module);
            }
        }
        return result;
    }

    private void sortedModules(List<Module> modules) {
        modules.sort((a, b) -> Float.compare(moduleWidth(b), moduleWidth(a)));
    }

    private float moduleWidth(Module module) {
        return Fonts.SFSEMIBOLD.get().getWidth(module.getName(), moduleNameSize()) + PADDING * 2f;
    }

    private float moduleNameSize() {
        return 6f;
    }

    private static class RowAnim {
        private final Animation slide = new Animation(Easing.EXPO_OUT, (int) (ROW_ANIM * 1000));
        private final Animation offset = new Animation(Easing.EXPO_OUT, (int) (ROW_ANIM * 1000));

        private RowAnim() {
            slide.setValue(-15f);
        }
    }
}