package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@ModuleInformation(
        moduleName = "Crosshair",
        moduleDesc = "\u041A\u0430\u0441\u0442\u043E\u043C\u043D\u044B\u0439 \u043A\u0440\u0443\u0433\u043B\u044B\u0439 \u043F\u0440\u0438\u0446\u0435\u043B \u0441 \u0438\u043D\u0434\u0438\u043A\u0430\u0446\u0438\u0435\u0439 \u043A\u0434",
        moduleCategory = ModuleCategory.RENDER
)
public class Crosshair extends Module {

    private static final String CIRCLE_VIEW_FIRST = "\u041F\u0435\u0440\u0432\u044B\u0439";
    private static final String CIRCLE_VIEW_SECOND = "\u0412\u0442\u043E\u0440\u043E\u0439";

    private final ModeSetting mode = new ModeSetting("\u0420\u0435\u0436\u0438\u043C", "Default", "Default", "Circle");

    private final SliderSetting circleRadius = new SliderSetting("\u0420\u0430\u0434\u0438\u0443\u0441 \u043A\u0440\u0443\u0433\u0430", 4.0f, 0.0f, 10.0f, 0.5f)
            .setVisible(() -> mode.is("Circle"));
    private final SliderSetting circleThickness = new SliderSetting("\u0422\u043E\u043B\u0449\u0438\u043D\u0430 \u043A\u0440\u0443\u0433\u0430", 1.5f, 0.5f, 5.0f, 0.5f)
            .setVisible(() -> mode.is("Circle"));
    private final ModeSetting circleView = new ModeSetting("\u0412\u0438\u0434 \u043A\u0440\u0443\u0433\u0430", CIRCLE_VIEW_FIRST, CIRCLE_VIEW_FIRST, CIRCLE_VIEW_SECOND)
            .setVisible(() -> mode.is("Circle"));
    private final SliderSetting smoothens = new SliderSetting("Smoothens", 0.15f, 0.0f, 1.0f, 0.05f)
            .setVisible(() -> mode.is("Circle") && circleView.is(CIRCLE_VIEW_FIRST));

    private final SliderSetting radius = new SliderSetting("\u0420\u0430\u0434\u0438\u0443\u0441", 4.0f, 2.0f, 10.0f, 0.5f)
            .setVisible(() -> mode.is("Default"));
    private final SliderSetting length = new SliderSetting("\u0414\u043B\u0438\u043D\u0430", 3.0f, 1.0f, 10.0f, 0.5f)
            .setVisible(() -> mode.is("Default"));
    private final SliderSetting thickness = new SliderSetting("\u0422\u043E\u043B\u0449\u0438\u043D\u0430", 1.0f, 0.5f, 5.0f, 0.5f)
            .setVisible(() -> mode.is("Default"));

    private final BooleanSetting outline = new BooleanSetting("\u041E\u0431\u0432\u043E\u0434\u043A\u0430", false)
            .setVisible(() -> mode.is("Default"));
    private final BooleanSetting highlightEntities = new BooleanSetting("\u041E\u043A\u0440\u0430\u0448\u0438\u0432\u0430\u0442\u044C \u043D\u0430\u0432\u043E\u0434\u043A\u0443", true);
    private final BooleanSetting dot = new BooleanSetting("\u0422\u043E\u0447\u043A\u0430 \u0432 \u0446\u0435\u043D\u0442\u0440\u0435", true)
            .setVisible(() -> mode.is("Default"));
    private final BooleanSetting tShape = new BooleanSetting("T-\u041E\u0431\u0440\u0430\u0437\u043D\u044B\u0439", false)
            .setVisible(() -> mode.is("Default"));
    private final BooleanSetting cooldownIndicator = new BooleanSetting("\u0410\u043D\u0438\u043C\u0430\u0446\u0438\u044F \u043A\u0443\u043B\u0434\u0430\u0443\u043D\u0430", false);
    private final SliderSetting animationStrength = new SliderSetting("\u0421\u0438\u043B\u0430 \u0430\u043D\u0438\u043C\u0430\u0446\u0438\u0438", 1.0f, 0.1f, 2.0f, 0.1f)
            .setVisible(cooldownIndicator::getValue);

    private final BooleanSetting hideVanilla = new BooleanSetting("\u0421\u043A\u0440\u044B\u0442\u044C \u0432\u0430\u043D\u0438\u043B\u0443", true);

    private float animatedOffset;
    private float circleAnimation;

    @Override
    public void onEnable() {
        animatedOffset = 0f;
        circleAnimation = 0f;
        super.onEnable();
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (!isEnabled()) return;
        if (!canRenderCustomCrosshair()) return;

        float centerX = mc.getWindow().getScaledWidth() * 0.5f;
        float centerY = mc.getWindow().getScaledHeight() * 0.5f;

        boolean onEntity = mc.crosshairTarget instanceof EntityHitResult hitResult
                && hitResult.getEntity() != null
                && !hitResult.getEntity().isRemoved()
                && hitResult.getEntity() != mc.player;
        int color = ColorProvider.rgba(255, 255, 255, 255);
        int highlightColor = ColorProvider.rgba(255, 60, 60, 255);
        int currentColor = (highlightEntities.getValue() && onEntity) ? highlightColor : color;

        float baseRadius = mode.is("Circle") ? circleRadius.getFloatValue() : radius.getFloatValue();
        float cooldownProgress = 1.0F - MathHelper.clamp(mc.player.getAttackCooldownProgress(0.0f), 0.0f, 1.0f);
        float easedProgress = (float) (-(Math.cos(Math.PI * cooldownProgress) - 1) / 2.0);
        float cooldownOffset = cooldownIndicator.getValue()
                ? easedProgress * 10.0f * animationStrength.getFloatValue()
                : 0.0f;

        float targetOffset = baseRadius + cooldownOffset;

        if (animatedOffset == 0f) animatedOffset = targetOffset;
        animatedOffset += (targetOffset - animatedOffset) * 0.35f;

        MatrixStack matrixStack = e.getDrawContext().getMatrices();

        if (mode.is("Default")) {
            float t = thickness.getFloatValue();
            float l = length.getFloatValue();

            if (dot.getValue()) {
                drawRect(matrixStack, centerX - t / 2f, centerY - t / 2f, t, t, currentColor);
            }

            drawRect(matrixStack, centerX - animatedOffset - l, centerY - t / 2f, l, t, currentColor);
            drawRect(matrixStack, centerX + animatedOffset, centerY - t / 2f, l, t, currentColor);
            drawRect(matrixStack, centerX - t / 2f, centerY + animatedOffset, t, l, currentColor);

            if (!tShape.getValue()) {
                drawRect(matrixStack, centerX - t / 2f, centerY - animatedOffset - l, t, l, currentColor);
            }
        } else if (mode.is("Circle")) {
            if (circleView.is(CIRCLE_VIEW_FIRST)) {
                renderCircleFirst(matrixStack, centerX, centerY);
            } else {
                renderCircleSecond(matrixStack, centerX, centerY, currentColor);
            }
        }
    }

    private void renderCircleFirst(MatrixStack matrices, float centerX, float centerY) {
        circleAnimation = fast(circleAnimation, (1.0f - mc.player.getAttackCooldownProgress(1.0f)) * 260.0f, 10.0f);

        int baseColor = ColorProvider.rgba(23, 21, 21, 180);
        int themeColor = ColorProvider.setAlpha(ColorProvider.getThemeColor(), 180);
        float radiusValue = circleRadius.getFloatValue();
        float thicknessValue = circleThickness.getFloatValue();

        drawArc(matrices, centerX, centerY, radiusValue, thicknessValue, 0.0f, 360.0f, baseColor);
        drawArc(matrices, centerX, centerY, radiusValue, thicknessValue, circleAnimation, 360.0f, themeColor);
    }

    private void renderCircleSecond(MatrixStack matrices, float centerX, float centerY, int color) {
        float diameter = animatedOffset * 2.0f;
        Builder.border()
                .size(new SizeState(diameter, diameter))
                .radius(new QuadRadiusState(animatedOffset))
                .color(new QuadColorState(color))
                .thickness(circleThickness.getFloatValue())
                .smoothness(1.0f, 0.7f)
                .build()
                .render(matrices.peek().getPositionMatrix(), centerX - animatedOffset, centerY - animatedOffset);
    }

    private float fast(float current, float target, float speed) {
        if (speed <= 1.0f) return target;
        return current + (target - current) / speed;
    }

    private void drawArc(
            MatrixStack matrices,
            float centerX,
            float centerY,
            float radius,
            float thickness,
            float startAngle,
            float endAngle,
            int color
    ) {
        float clampedStart = MathHelper.clamp(startAngle, 0.0f, 360.0f);
        float clampedEnd = MathHelper.clamp(endAngle, 0.0f, 360.0f);
        if (clampedEnd <= clampedStart) {
            return;
        }

        float smoothness = MathHelper.clamp(smoothens.getFloatValue(), 0.0f, 1.0f);
        float halfThickness = Math.max(0.01f, thickness * 0.5f);
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;
        float angleRange = clampedEnd - clampedStart;
        int segments = Math.max(120, (int) (angleRange / 360.0f * (160.0f + smoothness * 260.0f)));

        if (ColorProvider.alpha(color) <= 0) {
            return;
        }

        float aaSize = 0.10f + smoothness * 0.42f;
        float coreInner = Math.min(outerRadius, innerRadius + aaSize);
        float coreOuter = Math.max(innerRadius, outerRadius - aaSize);
        if (coreOuter < coreInner) {
            float mid = (outerRadius + innerRadius) * 0.5f;
            coreInner = mid;
            coreOuter = mid;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int transparent = ColorProvider.setAlpha(color, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        drawArcStrip(matrix, centerX, centerY, clampedStart, clampedEnd, segments, outerRadius, coreOuter, color, transparent);
        drawArcStrip(matrix, centerX, centerY, clampedStart, clampedEnd, segments, coreOuter, coreInner, color, color);
        if (coreInner > innerRadius) {
            drawArcStrip(matrix, centerX, centerY, clampedStart, clampedEnd, segments, coreInner, innerRadius, color, transparent);
        }

        RenderSystem.disableBlend();
    }

    private void drawArcStrip(
            Matrix4f matrix,
            float centerX,
            float centerY,
            float startAngle,
            float endAngle,
            int segments,
            float outerRadius,
            float innerRadius,
            int outerColor,
            int innerColor
    ) {
        if (outerRadius <= innerRadius) {
            return;
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float progress = i / (float) segments;
            float angle = MathHelper.lerp(progress, startAngle, endAngle) - 90.0f;
            float radians = angle * ((float) Math.PI / 180.0f);
            float cos = MathHelper.cos(radians);
            float sin = MathHelper.sin(radians);

            buffer.vertex(matrix, centerX + cos * outerRadius, centerY + sin * outerRadius, 0.0f).color(outerColor);
            buffer.vertex(matrix, centerX + cos * innerRadius, centerY + sin * innerRadius, 0.0f).color(innerColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawRect(MatrixStack matrices, float x, float y, float w, float h, int color) {
        if (outline.getValue()) {
            float outlineThicknessValue = 0.5f;
            DrawUtil.drawRect(
                    matrices,
                    x - outlineThicknessValue,
                    y - outlineThicknessValue,
                    w + outlineThicknessValue * 2,
                    h + outlineThicknessValue * 2,
                    ColorProvider.rgba(0, 0, 0, 255)
            );
        }
        DrawUtil.drawRect(matrices, x, y, w, h, color);
    }

    public boolean shouldHideVanillaCrosshair() {
        return isEnabled() && hideVanilla.getValue() && canRenderCustomCrosshair();
    }

    private boolean canRenderCustomCrosshair() {
        if (mc.player == null || mc.world == null) return false;
        if (mc.options.hudHidden) return false;
        if (!mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) return false;
        return mc.currentScreen == null || mc.currentScreen instanceof ChatScreen;
    }
}
