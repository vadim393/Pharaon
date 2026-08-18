package tech.onetap.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.Random;

public class LightningBackground {
    private static final int BRANCH_COUNT = 34;
    private static final int SEGMENTS = 10;

    private final Random random = new Random();
    private final Branch[] branches = new Branch[BRANCH_COUNT];

    public LightningBackground() {
        for (int i = 0; i < BRANCH_COUNT; i++) {
            Branch branch = new Branch(random);
            branch.pickLocation(random);
            branch.nextFlash = 0L;
            branches[i] = branch;
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, int width, int height, float alpha) {
        if (alpha <= 0.01f || width <= 0 || height <= 0) {
            return;
        }

        long time = System.currentTimeMillis();

        for (Branch branch : branches) {
            branch.update(time, random);
            if (branch.flash <= 0.01f) {
                continue;
            }
            renderBranch(matrices, branch, width, height, alpha);
        }
    }

    private void renderBranch(MatrixStack matrices, Branch branch, int width, int height, float alpha) {
        float sx = branch.sx * width;
        float sy = branch.sy * height;
        float ex = branch.ex * width;
        float ey = branch.ey * height;

        float dx = ex - sx;
        float dy = ey - sy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 6f) {
            return;
        }

        float nx = -dy / len;
        float ny = dx / len;

        float[][] points = new float[SEGMENTS + 1][2];
        long time = System.currentTimeMillis();
        for (int i = 0; i <= SEGMENTS; i++) {
            float u = i / (float) SEGMENTS;
            float falloff = 1f - u * u;
            float wiggle = (float) Math.sin(time * 0.011 + branch.seed + i * 1.7) * 0.35f;
            float off = (branch.offsets[i] + wiggle * 0.16f) * falloff * branch.amp * 0.16f * len;
            points[i][0] = sx + dx * u + nx * off;
            points[i][1] = sy + dy * u + ny * off;
        }

        int theme = ColorProvider.getThemeColor();
        for (int i = 0; i < SEGMENTS; i++) {
            float flicker = 0.65f + 0.35f * (0.5f + 0.5f * (float) Math.sin(time * 0.023 + branch.seed + i * 2.1));
            int glowAlpha = (int) (26 * branch.flash * flicker * alpha);
            int coreAlpha = (int) (190 * branch.flash * (0.45f + 0.55f * flicker) * alpha);

            drawSegment(matrices, points[i][0], points[i][1], points[i + 1][0], points[i + 1][1], 6f,
                    ColorProvider.setAlpha(theme, glowAlpha));
            drawSegment(matrices, points[i][0], points[i][1], points[i + 1][0], points[i + 1][1], 1.3f,
                    ColorProvider.rgba(255, 255, 255, coreAlpha));
        }
    }

    private static void drawSegment(MatrixStack matrices, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) {
            return;
        }
        float nx = -dy / len * thickness * 0.5f;
        float ny = dx / len * thickness * 0.5f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        DrawUtil.drawSetup();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x1 + nx, y1 + ny, 0f).color(color);
        buffer.vertex(matrix, x2 + nx, y2 + ny, 0f).color(color);
        buffer.vertex(matrix, x2 - nx, y2 - ny, 0f).color(color);
        buffer.vertex(matrix, x1 - nx, y1 - ny, 0f).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        DrawUtil.drawEnd();
    }

    private float rand(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static class Branch {
        final float seed;
        final float amp;
        final float[] offsets = new float[SEGMENTS + 1];

        float sx;
        float sy;
        float ex;
        float ey;
        float flash;
        long nextFlash;
        long flashStart;

        Branch(Random random) {
            seed = random.nextFloat() * 1000f;
            amp = 0.75f + random.nextFloat() * 0.5f;
            reseedOffsets(random);
        }

        void reseedOffsets(Random random) {
            for (int i = 0; i <= SEGMENTS; i++) {
                offsets[i] = random.nextFloat() * 2f - 1f;
            }
        }

        void pickLocation(Random random) {
            sx = random.nextFloat();
            sy = random.nextFloat();
            ex = random.nextFloat();
            ey = random.nextFloat();
        }

        void update(long time, Random random) {
            if (time < nextFlash) {
                long since = time - flashStart;
                if (flashStart > 0 && since >= 0 && since < 240L) {
                    float base = 1f - since / 240.0f;
                    if (random.nextInt(9) == 0) {
                        base *= 0.25f;
                    }
                    flash = Math.min(1f, base * 3.2f);
                } else {
                    flash = 0f;
                }
                return;
            }

            pickLocation(random);
            reseedOffsets(random);
            flashStart = time;
            flash = 1f;
            nextFlash = time + 260L + random.nextInt(1100);
        }
    }
}