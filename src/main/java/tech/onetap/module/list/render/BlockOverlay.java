package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.util.render.providers.ColorProvider;

@ModuleInformation(moduleName = "BlockOverlay", moduleDesc = "\u041E\u0431\u0432\u043E\u0434\u0438\u0442 \u043D\u0430\u0432\u0435\u0434\u0435\u043D\u043D\u044B\u0439 \u0431\u043B\u043E\u043A", moduleCategory = ModuleCategory.RENDER)
public class BlockOverlay extends Module {
    public static final BlockOverlay INSTANCE = new BlockOverlay();
    private static final String SHADER_MODE_PLASMA = "Plasma";
    private static final String SHADER_MODE_BALATRO = "Balatro";
    private static final String SHADER_MODE_CELLS = "Cells";

    private static final ShaderProgramKey BLOCK_PLASMA_SHADER = new ShaderProgramKey(
            Identifier.of("mre", "core/block_overlay"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );
    private static final ShaderProgramKey BLOCK_BALATRO_SHADER = new ShaderProgramKey(
            Identifier.of("mre", "core/block_overlay_balatro"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );
    private static final ShaderProgramKey BLOCK_CELLS_SHADER = new ShaderProgramKey(
            Identifier.of("mre", "core/block_overlay_cells"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );
    private static final int PLASMA_PRIMARY_TINT = ColorProvider.rgba(136, 121, 176, 255);
    private static final int PLASMA_SECONDARY_TINT = ColorProvider.rgba(128, 165, 218, 255);
    private static final int PLASMA_ACCENT_TINT = ColorProvider.rgba(231, 166, 214, 255);
    private static final int BALATRO_PRIMARY_TINT = ColorProvider.rgba(102, 25, 54, 255);
    private static final int BALATRO_SECONDARY_TINT = ColorProvider.rgba(47, 17, 65, 255);
    private static final int BALATRO_ACCENT_TINT = ColorProvider.rgba(255, 194, 92, 255);
    private static final int CELLS_PRIMARY_TINT = ColorProvider.rgba(24, 119, 108, 255);
    private static final int CELLS_SECONDARY_TINT = ColorProvider.rgba(88, 190, 164, 255);
    private static final int CELLS_ACCENT_TINT = ColorProvider.rgba(224, 255, 206, 255);
    private static final int SHADER_OUTLINE = ColorProvider.rgba(255, 255, 255, 255);
    private final SliderSetting outlineAlpha = new SliderSetting("\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u043E\u0443\u0442\u043B\u0430\u0439\u043D\u0430", 1.0f, 0.0f, 1.0f, 0.05f).setVisible(() -> false);
    private final SliderSetting fillAlpha = new SliderSetting("\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u0437\u0430\u043B\u0438\u0432\u043A\u0438", 0.3f, 0.0f, 1.0f, 0.05f);
    private final SliderSetting lineWidth = new SliderSetting("\u0422\u043E\u043B\u0449\u0438\u043D\u0430 \u043B\u0438\u043D\u0438\u0438", 2.0f, 1.0f, 5.0f, 0.5f).setVisible(() -> false);
    private final BooleanSetting useShader = new BooleanSetting("\u0418\u0441\u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u044C \u0448\u0435\u0439\u0434\u0435\u0440", true);
    private final ModeSetting shaderMode = new ModeSetting("\u0428\u0435\u0439\u0434\u0435\u0440", SHADER_MODE_PLASMA, SHADER_MODE_PLASMA, SHADER_MODE_BALATRO, SHADER_MODE_CELLS).setVisible(useShader::getValue);

    private Box currentBox = null;
    private float currentVisibility = 0.0F;

    @Subscribe
    public void onRender3D(EventWorldRender e) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        HitResult hitResult = mc.crosshairTarget;
        BlockPos blockPos = null;
        boolean hasValidTarget = false;
        float animationFactor = Math.max(0.75f, e.getTickDelta());

        if (hitResult instanceof BlockHitResult bhr) {
            BlockPos candidate = bhr.getBlockPos();
            BlockState state = mc.world.getBlockState(candidate);

            if (!(state.getBlock() instanceof FluidBlock) && !state.isAir()) {
                blockPos = candidate;
                hasValidTarget = true;
            }
        }

        if (hasValidTarget) {
            Box targetBox = new Box(blockPos).expand(0.002D);
            if (currentBox == null) {
                currentBox = targetBox;
            }
            double step = Math.min(1.0, 0.16D * 2.35D * animationFactor);
            currentBox = interpolateBox(currentBox, targetBox, step);
            currentVisibility = approach(currentVisibility, 1.0F, 0.14F * animationFactor);
        } else {
            currentVisibility = 0.0F;
            currentBox = null;
            return;
        }

        renderInternal(e.getMatrixStack(), currentBox, currentVisibility);
    }

    public void renderStyledWorldBox(MatrixStack matrices, Box worldBox) {
        if (mc.player == null || mc.world == null || worldBox == null) return;
        renderInternal(matrices, worldBox.expand(0.002D), 1.0F);
    }

    private void renderInternal(MatrixStack matrices, Box worldBox, float alphaMultiplier) {
        if (alphaMultiplier <= 0.01F) {
            return;
        }

        boolean shaderEnabled = useShader.getValue();
        int themeA = ThemeManager.getInstance().getCurrentTheme().getColorFirst();
        int themeB = resolveSecondaryThemeColor(themeA);

        int fillA = ColorProvider.setAlpha(themeA, Math.round((float) fillAlpha.getValue() * 255.0F * alphaMultiplier));
        int fillB = ColorProvider.setAlpha(themeB, Math.round((float) fillAlpha.getValue() * 255.0F * alphaMultiplier));
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float x1 = (float) (worldBox.minX - camPos.x);
        float y1 = (float) (worldBox.minY - camPos.y);
        float z1 = (float) (worldBox.minZ - camPos.z);
        float x2 = (float) (worldBox.maxX - camPos.x);
        float y2 = (float) (worldBox.maxY - camPos.y);
        float z2 = (float) (worldBox.maxZ - camPos.z);

        drawFill(matrices, x1, y1, z1, x2, y2, z2, fillA, fillB, shaderEnabled);
    }

    private Box interpolateBox(Box from, Box to, double step) {
        return new Box(
                lerp(from.minX, to.minX, step),
                lerp(from.minY, to.minY, step),
                lerp(from.minZ, to.minZ, step),
                lerp(from.maxX, to.maxX, step),
                lerp(from.maxY, to.maxY, step),
                lerp(from.maxZ, to.maxZ, step)
        );
    }

    private void drawFill(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, int colorA, int colorB, boolean shaderEnabled) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        if (shaderEnabled) {
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            ShaderProgram shader = RenderSystem.setShader(getActiveShaderKey());
            if (shader != null) {
                applyShaderUniforms(shader);
            }
        } else {
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Direction face : Direction.values()) {
            putFace(buffer, matrix, face, x1, y1, z1, x2, y2, z2, shaderEnabled, colorA, colorB);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void putFace(BufferBuilder buffer, Matrix4f matrix, Direction face,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         boolean shaderMode, int colorA, int colorB) {
        if (shaderMode) {
            putShaderFace(buffer, matrix, face, x1, y1, z1, x2, y2, z2);
            return;
        }

        int bottom = shaderMode ? 0xFFFFFFFF : colorA;
        int top = shaderMode ? 0xFFFFFFFF : colorB;
        int flat = shaderMode ? 0xFFFFFFFF : ColorProvider.interpolateColor(colorA, colorB, 0.5f);

        switch (face) {
            case UP -> {
                buffer.vertex(matrix, x1, y2, z1).color(flat);
                buffer.vertex(matrix, x1, y2, z2).color(flat);
                buffer.vertex(matrix, x2, y2, z2).color(flat);
                buffer.vertex(matrix, x2, y2, z1).color(flat);
            }
            case DOWN -> {
                buffer.vertex(matrix, x1, y1, z1).color(flat);
                buffer.vertex(matrix, x2, y1, z1).color(flat);
                buffer.vertex(matrix, x2, y1, z2).color(flat);
                buffer.vertex(matrix, x1, y1, z2).color(flat);
            }
            case NORTH -> {
                buffer.vertex(matrix, x1, y1, z1).color(bottom);
                buffer.vertex(matrix, x1, y2, z1).color(top);
                buffer.vertex(matrix, x2, y2, z1).color(top);
                buffer.vertex(matrix, x2, y1, z1).color(bottom);
            }
            case SOUTH -> {
                buffer.vertex(matrix, x1, y1, z2).color(bottom);
                buffer.vertex(matrix, x2, y1, z2).color(bottom);
                buffer.vertex(matrix, x2, y2, z2).color(top);
                buffer.vertex(matrix, x1, y2, z2).color(top);
            }
            case WEST -> {
                buffer.vertex(matrix, x1, y1, z1).color(bottom);
                buffer.vertex(matrix, x1, y1, z2).color(bottom);
                buffer.vertex(matrix, x1, y2, z2).color(top);
                buffer.vertex(matrix, x1, y2, z1).color(top);
            }
            case EAST -> {
                buffer.vertex(matrix, x2, y1, z1).color(bottom);
                buffer.vertex(matrix, x2, y2, z1).color(top);
                buffer.vertex(matrix, x2, y2, z2).color(top);
                buffer.vertex(matrix, x2, y1, z2).color(bottom);
            }
        }
    }

    private void putShaderFace(BufferBuilder buffer, Matrix4f matrix, Direction face,
                               float x1, float y1, float z1, float x2, float y2, float z2) {
        switch (face) {
            case UP -> {
                putShaderVertex(buffer, matrix, x1, y2, z1, 0.0f, 1.0f, false);
                putShaderVertex(buffer, matrix, x1, y2, z2, 0.0f, 0.0f, false);
                putShaderVertex(buffer, matrix, x2, y2, z2, 1.0f, 0.0f, false);
                putShaderVertex(buffer, matrix, x2, y2, z1, 1.0f, 1.0f, false);
            }
            case DOWN -> {
                putShaderVertex(buffer, matrix, x1, y1, z1, 0.0f, 1.0f, false);
                putShaderVertex(buffer, matrix, x2, y1, z1, 1.0f, 1.0f, false);
                putShaderVertex(buffer, matrix, x2, y1, z2, 1.0f, 0.0f, false);
                putShaderVertex(buffer, matrix, x1, y1, z2, 0.0f, 0.0f, false);
            }
            case NORTH -> {
                putShaderVertex(buffer, matrix, x1, y1, z1, 0.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x1, y2, z1, 0.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x2, y2, z1, 1.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x2, y1, z1, 1.0f, 1.0f, true);
            }
            case SOUTH -> {
                putShaderVertex(buffer, matrix, x1, y1, z2, 0.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x2, y1, z2, 1.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x2, y2, z2, 1.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x1, y2, z2, 0.0f, 0.0f, true);
            }
            case WEST -> {
                putShaderVertex(buffer, matrix, x1, y1, z1, 0.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x1, y1, z2, 1.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x1, y2, z2, 1.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x1, y2, z1, 0.0f, 0.0f, true);
            }
            case EAST -> {
                putShaderVertex(buffer, matrix, x2, y1, z1, 0.0f, 1.0f, true);
                putShaderVertex(buffer, matrix, x2, y2, z1, 0.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x2, y2, z2, 1.0f, 0.0f, true);
                putShaderVertex(buffer, matrix, x2, y1, z2, 1.0f, 1.0f, true);
            }
        }
    }

    private void putShaderVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float u, float v, boolean sideFace) {
        int encoded = ColorProvider.rgba(Math.round(u * 255.0f), Math.round(v * 255.0f), sideFace ? 255 : 0, 255);
        buffer.vertex(matrix, x, y, z).color(encoded);
    }

    private void drawOutline(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width, boolean shaderStyle) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (shaderStyle) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(false);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.lineWidth(width);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        if (shaderStyle) {
            drawBoxEdges(buffer, matrix, x1, y1, z1, x2, y2, z2, color);
        } else {
            float dash = 0.2f;
            float gap = 0.05f;
            float pattern = dash + gap;
            float phase = ((System.currentTimeMillis() % 1600L) / 1600.0f) * pattern;
            drawBoxDashedEdges(buffer, matrix, x1, y1, z1, x2, y2, z2, color, dash, gap, phase);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawBoxEdges(BufferBuilder buffer, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              int color) {
        drawLine(buffer, matrix, x1, y1, z1, x2, y1, z1, color);
        drawLine(buffer, matrix, x2, y1, z1, x2, y1, z2, color);
        drawLine(buffer, matrix, x2, y1, z2, x1, y1, z2, color);
        drawLine(buffer, matrix, x1, y1, z2, x1, y1, z1, color);

        drawLine(buffer, matrix, x1, y2, z1, x2, y2, z1, color);
        drawLine(buffer, matrix, x2, y2, z1, x2, y2, z2, color);
        drawLine(buffer, matrix, x2, y2, z2, x1, y2, z2, color);
        drawLine(buffer, matrix, x1, y2, z2, x1, y2, z1, color);

        drawLine(buffer, matrix, x1, y1, z1, x1, y2, z1, color);
        drawLine(buffer, matrix, x2, y1, z1, x2, y2, z1, color);
        drawLine(buffer, matrix, x1, y1, z2, x1, y2, z2, color);
        drawLine(buffer, matrix, x2, y1, z2, x2, y2, z2, color);
    }

    private void drawBoxDashedEdges(BufferBuilder buffer, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int color, float dash, float gap, float phase) {
        drawDashedLine(buffer, matrix, x1, y1, z1, x2, y1, z1, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y1, z1, x2, y1, z2, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y1, z2, x1, y1, z2, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x1, y1, z2, x1, y1, z1, color, dash, gap, phase);

        drawDashedLine(buffer, matrix, x1, y2, z1, x2, y2, z1, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y2, z1, x2, y2, z2, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y2, z2, x1, y2, z2, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x1, y2, z2, x1, y2, z1, color, dash, gap, phase);

        drawDashedLine(buffer, matrix, x1, y1, z1, x1, y2, z1, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y1, z1, x2, y2, z1, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x1, y1, z2, x1, y2, z2, color, dash, gap, phase);
        drawDashedLine(buffer, matrix, x2, y1, z2, x2, y2, z2, color, dash, gap, phase);
    }

    private void drawDashedLine(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                int color, float dash, float gap, float phase) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.0001f) return;

        float ux = dx / length;
        float uy = dy / length;
        float uz = dz / length;

        float pattern = dash + gap;
        float traveled = -phase;

        while (traveled < length) {
            float start = Math.max(traveled, 0f);
            float end = Math.min(traveled + dash, length);

            if (end > start) {
                float sx = x1 + ux * start;
                float sy = y1 + uy * start;
                float sz = z1 + uz * start;

                float ex = x1 + ux * end;
                float ey = y1 + uy * end;
                float ez = z1 + uz * end;

                buffer.vertex(matrix, sx, sy, sz).color(color);
                buffer.vertex(matrix, ex, ey, ez).color(color);
            }

            traveled += pattern;
        }
    }

    private void drawLine(BufferBuilder buffer, Matrix4f matrix,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          int color) {
        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
    }

    private double lerp(double start, double end, double step) {
        return start + (end - start) * Math.min(1.0, Math.max(0.0, step));
    }

    private float approach(float current, float target, float step) {
        float clampedStep = Math.min(1.0F, Math.max(0.0F, step));
        return current + (target - current) * clampedStep;
    }

    public boolean shouldUseShaderStyle() {
        return useShader.getValue();
    }

    public String getSelectedShaderMode() {
        return getActiveShaderMode();
    }

    public int getShaderPrimaryColor() {
        int primaryTint = getPrimaryTint();
        int themeColor = ThemeManager.getInstance().getCurrentTheme().getColorFirst();
        float mixAmount = isBalatroShaderSelected() ? 0.12f : isCellsShaderSelected() ? 0.14f : 0.18f;
        int mixed = ColorProvider.interpolate(primaryTint, ColorProvider.setAlpha(themeColor, 255), mixAmount);
        return ColorProvider.setAlpha(mixed, getShaderAlpha());
    }

    public int getShaderSecondaryColor() {
        int secondaryTint = getSecondaryTint();
        int themeColor = resolveSecondaryThemeColor(ThemeManager.getInstance().getCurrentTheme().getColorFirst());
        float mixAmount = isBalatroShaderSelected() ? 0.10f : isCellsShaderSelected() ? 0.18f : 0.16f;
        int mixed = ColorProvider.interpolate(secondaryTint, ColorProvider.setAlpha(themeColor, 255), mixAmount);
        return ColorProvider.setAlpha(mixed, getShaderAlpha());
    }

    public int getShaderAccentColor() {
        int accentTint = getAccentTint();
        int themeColor = ThemeManager.getInstance().getCurrentTheme().getColorFirst();
        float mixAmount = isBalatroShaderSelected() ? 0.08f : isCellsShaderSelected() ? 0.12f : 0.10f;
        int mixed = ColorProvider.interpolate(accentTint, ColorProvider.setAlpha(themeColor, 255), mixAmount);
        return ColorProvider.setAlpha(mixed, getShaderAlpha());
    }

    public float getShaderFillAlphaValue() {
        return (float) fillAlpha.getValue();
    }

    public void applyShaderUniforms(ShaderProgram shader) {
        if (shader == null) {
            return;
        }

        int primaryColor = getShaderPrimaryColor();
        int secondaryColor = getShaderSecondaryColor();
        int accentColor = getShaderAccentColor();
        float w = (float) mc.getWindow().getScaledWidth();
        float h = (float) mc.getWindow().getScaledHeight();
        float time = (System.currentTimeMillis() % 1000000L) / 1000.0f;
        if (isCellsShaderSelected()) {
            time *= 0.78f;
        }

        if (shader.getUniform("resolution") != null) shader.getUniform("resolution").set(w, h);
        if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
        if (shader.getUniform("oct") != null) shader.getUniform("oct").set(isBalatroShaderSelected() ? 7 : isCellsShaderSelected() ? 5 : 6);
        if (shader.getUniform("factor") != null) shader.getUniform("factor").set(2.0f);
        if (shader.getUniform("quality") != null) shader.getUniform("quality").set(isBalatroShaderSelected() ? 7 : isCellsShaderSelected() ? 6 : 8);
        if (shader.getUniform("alpha") != null) shader.getUniform("alpha").set(getShaderFillAlphaValue());

        if (shader.getUniform("primaryColor") != null) {
            shader.getUniform("primaryColor").set(
                    ColorProvider.red(primaryColor) / 255.0f,
                    ColorProvider.green(primaryColor) / 255.0f,
                    ColorProvider.blue(primaryColor) / 255.0f,
                    ColorProvider.alpha(primaryColor) / 255.0f
            );
        }
        if (shader.getUniform("secondaryColor") != null) {
            shader.getUniform("secondaryColor").set(
                    ColorProvider.red(secondaryColor) / 255.0f,
                    ColorProvider.green(secondaryColor) / 255.0f,
                    ColorProvider.blue(secondaryColor) / 255.0f,
                    ColorProvider.alpha(secondaryColor) / 255.0f
            );
        }
        if (shader.getUniform("color") != null) {
            shader.getUniform("color").set(
                    ColorProvider.red(accentColor) / 255.0f,
                    ColorProvider.green(accentColor) / 255.0f,
                    ColorProvider.blue(accentColor) / 255.0f,
                    ColorProvider.alpha(accentColor) / 255.0f
            );
        }
    }

    private ShaderProgramKey getActiveShaderKey() {
        if (isBalatroShaderSelected()) {
            return BLOCK_BALATRO_SHADER;
        }
        if (isCellsShaderSelected()) {
            return BLOCK_CELLS_SHADER;
        }
        return BLOCK_PLASMA_SHADER;
    }

    private String getActiveShaderMode() {
        if (SHADER_MODE_BALATRO.equalsIgnoreCase(shaderMode.getValue())) {
            return SHADER_MODE_BALATRO;
        }
        if (SHADER_MODE_CELLS.equalsIgnoreCase(shaderMode.getValue())) {
            return SHADER_MODE_CELLS;
        }
        return SHADER_MODE_PLASMA;
    }

    private boolean isBalatroShaderSelected() {
        return SHADER_MODE_BALATRO.equalsIgnoreCase(getActiveShaderMode());
    }

    private boolean isCellsShaderSelected() {
        return SHADER_MODE_CELLS.equalsIgnoreCase(getActiveShaderMode());
    }

    private int getPrimaryTint() {
        if (isBalatroShaderSelected()) {
            return BALATRO_PRIMARY_TINT;
        }
        if (isCellsShaderSelected()) {
            return CELLS_PRIMARY_TINT;
        }
        return PLASMA_PRIMARY_TINT;
    }

    private int getSecondaryTint() {
        if (isBalatroShaderSelected()) {
            return BALATRO_SECONDARY_TINT;
        }
        if (isCellsShaderSelected()) {
            return CELLS_SECONDARY_TINT;
        }
        return PLASMA_SECONDARY_TINT;
    }

    private int getAccentTint() {
        if (isBalatroShaderSelected()) {
            return BALATRO_ACCENT_TINT;
        }
        if (isCellsShaderSelected()) {
            return CELLS_ACCENT_TINT;
        }
        return PLASMA_ACCENT_TINT;
    }

    private int resolveSecondaryThemeColor(int primary) {
        int secondary = ThemeManager.getInstance().getCurrentTheme().getColorSecond();
        int brightness = ColorProvider.red(secondary) + ColorProvider.green(secondary) + ColorProvider.blue(secondary);
        if (brightness <= 24) {
            secondary = ColorProvider.interpolateColor(primary, ColorProvider.rgba(255, 255, 255, ColorProvider.alpha(primary)), 0.35f);
        }
        return secondary;
    }

    private int getShaderAlpha() {
        return (int) (fillAlpha.getValue() * 255);
    }
}
