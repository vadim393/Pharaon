package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Soul ESP", moduleDesc = "Выпускает ангела из игрока получившего тотем", moduleCategory = ModuleCategory.RENDER)
public class SoulESP extends Module {
    private static final long ANGEL_DURATION_MS = 3200L;
    private static final float ANGEL_RISE = 3.1f;
    private static final float UNIT = 1.0f / 16.0f;

    private final List<AngelSpirit> angels = new ArrayList<>();

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!(e.getPacket() instanceof EntityStatusS2CPacket packet) || packet.getStatus() != 35) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);
        if (entity instanceof PlayerEntity player) {
            angels.add(new AngelSpirit(
                    player.getPos(),
                    player.getBodyYaw(),
                    player.isSneaking(),
                    player.age,
                    System.currentTimeMillis()
            ));
        }
    }

    @Subscribe
    public void onRender(EventWorldRender e) {
        if (mc.player == null || mc.world == null || angels.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        angels.removeIf(angel -> now - angel.spawnTime >= ANGEL_DURATION_MS);
        if (angels.isEmpty()) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = e.getMatrixStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (AngelSpirit angel : angels) {
            float progress = MathHelper.clamp((now - angel.spawnTime) / (float) ANGEL_DURATION_MS, 0.0f, 1.0f);
            if (progress >= 1.0f) {
                continue;
            }

            float alpha = (1.0f - progress) * 0.85f;
            float rise = ANGEL_RISE * ease(progress);
            float driftX = MathHelper.sin(angel.phase * 0.09f + progress * 5.8f) * 0.08f;
            float driftZ = MathHelper.cos(angel.phase * 0.07f + progress * 4.4f) * 0.05f;
            float wingFlap = MathHelper.sin(angel.phase * 0.13f + progress * 11.0f) * 18.0f;

            matrices.push();
            matrices.translate(
                    angel.pos.x - cameraPos.x + driftX,
                    angel.pos.y - cameraPos.y + rise,
                    angel.pos.z - cameraPos.z + driftZ
            );
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - angel.yaw));
            matrices.scale(-1.0f, -1.0f, 1.0f);
            float scale = 1.0f + progress * 0.18f;
            matrices.scale(scale, scale, scale);
            matrices.translate(0.0f, -1.5f, 0.0f);

            if (angel.sneak) {
                matrices.translate(0.0f, 0.2f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0f));
            }

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            drawAngel(buffer, matrices, wingFlap, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawAngel(BufferBuilder buffer, MatrixStack matrices, float wingFlap, float alpha) {
        Matrix4f baseMatrix = matrices.peek().getPositionMatrix();

        float bodyR = 0.96f;
        float bodyG = 0.98f;
        float bodyB = 1.0f;
        float clothR = 0.92f;
        float clothG = 0.95f;
        float clothB = 1.0f;

        box(buffer, baseMatrix, -4 * UNIT, -8 * UNIT, -4 * UNIT, 8 * UNIT, 8 * UNIT, 8 * UNIT, bodyR, bodyG, bodyB, alpha * 0.72f);
        box(buffer, baseMatrix, -3 * UNIT, 0.0f, -2 * UNIT, 6 * UNIT, 10 * UNIT, 4 * UNIT, bodyR, bodyG, bodyB, alpha * 0.68f);
        box(buffer, baseMatrix, -5 * UNIT, 10 * UNIT, -3 * UNIT, 10 * UNIT, 12 * UNIT, 6 * UNIT, clothR, clothG, clothB, alpha * 0.60f);

        box(buffer, baseMatrix, -7 * UNIT, 1 * UNIT, -1.5f * UNIT, 3 * UNIT, 10 * UNIT, 3 * UNIT, bodyR, bodyG, bodyB, alpha * 0.52f);
        box(buffer, baseMatrix, 4 * UNIT, 1 * UNIT, -1.5f * UNIT, 3 * UNIT, 10 * UNIT, 3 * UNIT, bodyR, bodyG, bodyB, alpha * 0.52f);

        renderWing(buffer, matrices, true, wingFlap, alpha);
        renderWing(buffer, matrices, false, wingFlap, alpha);
        renderHalo(buffer, matrices, alpha);
    }

    private void renderWing(BufferBuilder buffer, MatrixStack matrices, boolean left, float wingFlap, float alpha) {
        float direction = left ? -1.0f : 1.0f;
        float angleZ = direction * (30.0f + wingFlap * 0.78f);
        float angleY = direction * 20.0f;

        matrices.push();
        matrices.translate(direction * 4.3f * UNIT, 0.2f * UNIT, 1.5f * UNIT);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleZ));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angleY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8.0f));
        Matrix4f wingMatrix = matrices.peek().getPositionMatrix();

        float wingR = 0.97f;
        float wingG = 0.99f;
        float wingB = 1.0f;
        float veilZ = 0.55f * UNIT;

        quad(
                buffer,
                wingMatrix,
                direction * 0.3f * UNIT, -2.2f * UNIT, veilZ,
                direction * 5.0f * UNIT, -1.4f * UNIT, veilZ + 0.1f * UNIT,
                direction * 10.4f * UNIT, 5.8f * UNIT, veilZ,
                direction * 1.2f * UNIT, 5.2f * UNIT, veilZ - 0.08f * UNIT,
                wingR,
                wingG,
                wingB,
                alpha * 0.30f
        );
        quad(
                buffer,
                wingMatrix,
                direction * 0.8f * UNIT, -0.4f * UNIT, veilZ + 0.08f * UNIT,
                direction * 6.3f * UNIT, 0.6f * UNIT, veilZ + 0.16f * UNIT,
                direction * 10.1f * UNIT, 6.7f * UNIT, veilZ + 0.04f * UNIT,
                direction * 1.8f * UNIT, 6.1f * UNIT, veilZ - 0.08f * UNIT,
                wingR,
                wingG,
                wingB,
                alpha * 0.24f
        );

        float[] rootX = {0.5f, 0.9f, 1.2f, 1.55f, 1.9f};
        float[] rootY = {-0.8f, 0.5f, 1.9f, 3.4f, 5.0f};
        float[] tipX = {12.0f, 11.3f, 10.2f, 8.9f, 7.4f};
        float[] tipY = {2.2f, 4.4f, 6.6f, 8.4f, 9.8f};
        float[] rootWidth = {2.2f, 2.0f, 1.85f, 1.7f, 1.55f};
        float[] tipWidth = {2.9f, 2.6f, 2.25f, 2.0f, 1.8f};

        for (int i = 0; i < rootX.length; i++) {
            feather(
                    buffer,
                    wingMatrix,
                    direction,
                    rootX[i] * UNIT,
                    rootY[i] * UNIT,
                    tipX[i] * UNIT,
                    tipY[i] * UNIT,
                    rootWidth[i] * UNIT,
                    tipWidth[i] * UNIT,
                    (0.56f - i * 0.05f) * UNIT,
                    wingR,
                    wingG,
                    wingB,
                    alpha * (0.46f - i * 0.05f)
            );
        }
        matrices.pop();
    }

    private void renderHalo(BufferBuilder buffer, MatrixStack matrices, float alpha) {
        float innerRadius = 4.75f * UNIT;
        float outerRadius = 6.1f * UNIT;
        float haloY = -10.85f * UNIT;
        float r = 1.0f;
        float g = 0.93f;
        float b = 0.72f;
        int segments = 32;

        matrices.push();
        matrices.translate(0.0f, haloY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(12.0f));
        Matrix4f haloMatrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++) {
            float start = (float) (Math.PI * 2.0 * i / segments);
            float end = (float) (Math.PI * 2.0 * (i + 1) / segments);
            float startCos = MathHelper.cos(start);
            float startSin = MathHelper.sin(start);
            float endCos = MathHelper.cos(end);
            float endSin = MathHelper.sin(end);

            quad(
                    buffer,
                    haloMatrix,
                    startCos * innerRadius, 0.0f, startSin * innerRadius,
                    startCos * outerRadius, 0.0f, startSin * outerRadius,
                    endCos * outerRadius, 0.0f, endSin * outerRadius,
                    endCos * innerRadius, 0.0f, endSin * innerRadius,
                    r,
                    g,
                    b,
                    alpha * 0.82f
            );
            quad(
                    buffer,
                    haloMatrix,
                    startCos * (innerRadius + 0.32f * UNIT), 0.02f * UNIT, startSin * (innerRadius + 0.32f * UNIT),
                    startCos * (outerRadius - 0.22f * UNIT), 0.02f * UNIT, startSin * (outerRadius - 0.22f * UNIT),
                    endCos * (outerRadius - 0.22f * UNIT), 0.02f * UNIT, endSin * (outerRadius - 0.22f * UNIT),
                    endCos * (innerRadius + 0.32f * UNIT), 0.02f * UNIT, endSin * (innerRadius + 0.32f * UNIT),
                    1.0f,
                    0.98f,
                    0.88f,
                    alpha * 0.95f
            );
        }
        matrices.pop();
    }

    private void feather(BufferBuilder buffer, Matrix4f matrix, float direction, float rootX, float rootY, float tipX, float tipY, float rootWidth, float tipWidth, float z, float r, float g, float b, float a) {
        quad(
                buffer,
                matrix,
                direction * rootX, rootY - rootWidth * 0.5f, z,
                direction * tipX, tipY - tipWidth * 0.5f, z,
                direction * (tipX - 1.9f * UNIT), tipY + tipWidth * 0.5f, z,
                direction * (rootX + 1.0f * UNIT), rootY + rootWidth * 0.5f, z,
                r,
                g,
                b,
                a
        );
    }

    private void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    private void box(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float a) {
        float x2 = x + sx;
        float y2 = y + sy;
        float z2 = z + sz;

        buffer.vertex(matrix, x, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z2).color(r, g, b, a);

        buffer.vertex(matrix, x2, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z).color(r, g, b, a);

        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z).color(r, g, b, a);

        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);

        buffer.vertex(matrix, x, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z).color(r, g, b, a);

        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x, y, z2).color(r, g, b, a);
    }

    private float ease(float value) {
        float clamped = MathHelper.clamp(value, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - clamped, 3.0);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        angels.clear();
    }

    private static class AngelSpirit {
        private final Vec3d pos;
        private final float yaw;
        private final boolean sneak;
        private final float phase;
        private final long spawnTime;

        private AngelSpirit(Vec3d pos, float yaw, boolean sneak, float phase, long spawnTime) {
            this.pos = pos;
            this.yaw = yaw;
            this.sneak = sneak;
            this.phase = phase;
            this.spawnTime = spawnTime;
        }
    }
}
