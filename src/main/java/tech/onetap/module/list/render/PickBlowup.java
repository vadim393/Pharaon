package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.util.render.providers.ColorProvider;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInformation(moduleName = "PickBlowup", moduleDesc = "чебуреки", moduleCategory = ModuleCategory.RENDER)
public class PickBlowup extends Module {
    private static final long DEFAULT_LIFETIME_MS = 2200L;
    private static final int MAX_EFFECTS = 64;
    private static final int RING_SEGMENTS = 28;
    private static final float EFFECT_SCALE = 1.6f;
    private static final float RING_DRAW_SCALE = 0.38f;
    private static final float VERTICAL_LINE_SCALE = 1.45f;

    private final Random random = new Random(123947126L);
    private final ModeSetting colorMode = new ModeSetting("ColorMode", "Client", "Random", "Client", "Picker", "DoublePicker");
    private final ColorSetting pickColor1 = new ColorSetting("PickColor1", ColorProvider.rgba(100, 255, 100, 255))
            .setVisible(() -> colorMode.is("Picker") || colorMode.is("DoublePicker"));
    private final ColorSetting pickColor2 = new ColorSetting("PickColor2", ColorProvider.rgba(60, 60, 255, 255))
            .setVisible(() -> colorMode.is("DoublePicker"));

    private final List<BlowupElement> blowupElements = new CopyOnWriteArrayList<>();

    @Override
    public void onDisable() {
        blowupElements.clear();
        super.onDisable();
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE || mc.world == null || mc.player == null) {
            return;
        }

        if (!(event.getPacket() instanceof ItemPickupAnimationS2CPacket packet)) {
            return;
        }

        Entity collectorEntity = mc.world.getEntityById(packet.getCollectorEntityId());
        if (collectorEntity != mc.player) {
            return;
        }

        Entity itemEntity = mc.world.getEntityById(packet.getEntityId());
        if (!(itemEntity instanceof ItemEntity item)) {
            return;
        }

        ItemStack stack = item.getStack();
        if (stack.isEmpty()) {
            return;
        }

        Vec3d effectPos = new Vec3d(item.getX(), Math.floor(item.getY()) + 0.01, item.getZ());
        blowupElements.add(new BlowupElement(effectPos, 0.05f * EFFECT_SCALE, 0.25f * EFFECT_SCALE, DEFAULT_LIFETIME_MS, stateColor()));
        trimEffects();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.world == null) {
            blowupElements.clear();
            return;
        }

        for (BlowupElement element : blowupElements) {
            element.update();
        }
        blowupElements.removeIf(BlowupElement::wantToRemove);
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        if (blowupElements.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Matrix4f matrix = event.getMatrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        event.getMatrixStack().push();
        event.getMatrixStack().translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f translatedMatrix = event.getMatrixStack().peek().getPositionMatrix();

        for (BlowupElement element : blowupElements) {
            element.draw(translatedMatrix);
        }

        event.getMatrixStack().pop();

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void trimEffects() {
        if (blowupElements.size() <= MAX_EFFECTS) {
            return;
        }

        int removeCount = blowupElements.size() - MAX_EFFECTS;
        blowupElements.subList(0, removeCount).clear();
    }

    private int stateColor() {
        return switch (colorMode.getValue()) {
            case "Random" -> Color.HSBtoRGB(random.nextFloat(), 1.0f, 1.0f);
            case "Picker" -> pickColor1.getValue();
            case "DoublePicker" -> ColorProvider.interpolate(pickColor1.getValue(), pickColor2.getValue(), random.nextFloat());
            default -> ThemeManager.getInstance().getCurrentTheme().getColorTheme(random.nextInt(360));
        };
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static float clamp01(float value) {
        return MathHelper.clamp(value, 0.0f, 1.0f);
    }

    private static float easeInOutQuad(float value) {
        float t = clamp01(value);
        return t < 0.5f ? 2.0f * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 2.0) * 0.5f;
    }

    private final class BlowupElement {
        private final long startTime = System.currentTimeMillis();
        private final long maxTime;
        private final Vec3d pos;
        private final List<NanoSpark> sparks = new ArrayList<>();
        private final float startRadius;
        private final float endRadius;
        private final int color;

        private BlowupElement(Vec3d pos, float startRadius, float endRadius, long maxTime, int color) {
            this.pos = pos;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.maxTime = maxTime;
            this.color = color;
        }

        private void update() {
            int spawnCount = 14;
            for (int i = 0; i < spawnCount; i++) {
                double yaw = Math.toRadians(random.nextInt(360));
                double radius = lerp(startRadius, endRadius, random.nextDouble());
                float radiusAlpha = easeInOutQuad((float) (radius / Math.max(endRadius, 0.0001f)));
                int sparkColor = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * radiusAlpha));
                Vec3d target = pos.add(
                        Math.sin(yaw) * radius,
                        random.nextDouble(0.02 * EFFECT_SCALE, 0.12 * EFFECT_SCALE),
                        Math.cos(yaw) * radius
                );
                sparks.add(new NanoSpark(pos, target, maxTime / 2.2f, sparkColor, (float) random.nextDouble(0.03 * EFFECT_SCALE, 0.12 * EFFECT_SCALE)));
            }

            sparks.removeIf(NanoSpark::wantToRemove);
        }

        private void draw(Matrix4f matrix) {
            float alpha = getAlphaPC();
            if (alpha <= 0.0f) {
                return;
            }

            if (!sparks.isEmpty()) {
                drawSparks(matrix, alpha);
            }

            if (getTimePC() >= 0.1f) {
                drawVerticalLine(matrix, alpha);
            }

            drawCircle(matrix, alpha);
        }

        private void drawSparks(Matrix4f matrix, float alpha) {
            RenderSystem.lineWidth(1.55f * EFFECT_SCALE);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            boolean hasVertices = false;

            for (NanoSpark spark : sparks) {
                float sparkAlpha = spark.getAlphaPC() * alpha;
                if (sparkAlpha <= 0.01f) {
                    continue;
                }

                Vec3d from = spark.getPreviousPos();
                Vec3d to = spark.getCurrentPos();
                int color1 = ColorProvider.setAlpha(spark.color(), (int) (ColorProvider.alpha(spark.color()) * sparkAlpha * 0.35f));
                int color2 = ColorProvider.setAlpha(spark.color(), (int) (ColorProvider.alpha(spark.color()) * sparkAlpha));

                buffer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(color1);
                buffer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(color2);
                hasVertices = true;
            }

            if (hasVertices) {
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } else {
                buffer.endNullable();
            }
            RenderSystem.lineWidth(1.0f);
        }

        private void drawVerticalLine(Matrix4f matrix, float alpha) {
            float lineAlpha = Math.min(easeInOutQuad(1.0f - getTimePC()) * 1.25f, 1.0f) * alpha;
            if (lineAlpha <= 0.01f) {
                return;
            }

            RenderSystem.lineWidth(4.5f * EFFECT_SCALE);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

            int base = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * lineAlpha * 0.12f));
            int mid = ColorProvider.setAlpha(color, 0);
            int head = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * lineAlpha));
            int top = ColorProvider.setAlpha(color, 0);

            buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(base);
            buffer.vertex(matrix, (float) pos.x, (float) (pos.y + 0.18f * VERTICAL_LINE_SCALE), (float) pos.z).color(mid);
            buffer.vertex(matrix, (float) pos.x, (float) (pos.y + 0.52f * VERTICAL_LINE_SCALE), (float) pos.z).color(head);
            buffer.vertex(matrix, (float) pos.x, (float) (pos.y + 1.02f * VERTICAL_LINE_SCALE), (float) pos.z).color(top);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.lineWidth(1.0f);
        }

        private void drawCircle(Matrix4f matrix, float alpha) {
            float radius = getRadius(startRadius, endRadius, 1.75f);
            float band = Math.max(0.008f, (endRadius - startRadius) * radius);
            float inner = Math.max(0.0f, radius - band * 0.5f);
            float outer = radius + band * 0.5f;

            drawRingStrip(matrix, inner * RING_DRAW_SCALE, radius * RING_DRAW_SCALE, 0, color, alpha);
            drawRingStrip(matrix, radius * RING_DRAW_SCALE, outer * RING_DRAW_SCALE, color, 0, alpha);
        }

        private void drawRingStrip(Matrix4f matrix, float innerRadius, float outerRadius, int innerColor, int outerColor, float alpha) {
            if (outerRadius <= innerRadius) {
                return;
            }

            int c1 = innerColor == 0 ? 0 : ColorProvider.setAlpha(innerColor, (int) (ColorProvider.alpha(innerColor) * alpha));
            int c2 = outerColor == 0 ? 0 : ColorProvider.setAlpha(outerColor, (int) (ColorProvider.alpha(outerColor) * alpha));

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                double angle = Math.toRadians((i / (double) RING_SEGMENTS) * 360.0);
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);

                buffer.vertex(matrix, (float) (pos.x + sin * innerRadius), (float) pos.y, (float) (pos.z + cos * innerRadius)).color(c1);
                buffer.vertex(matrix, (float) (pos.x + sin * outerRadius), (float) pos.y, (float) (pos.z + cos * outerRadius)).color(c2);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        private float getTimePC() {
            return clamp01((float) (System.currentTimeMillis() - startTime) / (float) maxTime);
        }

        private float getRadius(float minRadius, float maxRadius, float speed) {
            return (float) lerp(minRadius, maxRadius, clamp01(getTimePC() * speed));
        }

        private float getAlphaPC() {
            return easeInOutQuad(1.0f - getTimePC());
        }

        private boolean wantToRemove() {
            return getTimePC() >= 1.0f;
        }
    }

    private record NanoSpark(Vec3d origin, Vec3d target, float maxTime, int color, float lift, long startTime) {
        private NanoSpark(Vec3d origin, Vec3d target, float maxTime, int color, float lift) {
            this(origin, target, maxTime, color, lift, System.currentTimeMillis());
        }

        private float getAlphaPC() {
            return 1.0f - clamp01((float) (System.currentTimeMillis() - startTime) / maxTime);
        }

        private float getProgress() {
            return clamp01((float) (System.currentTimeMillis() - startTime) / maxTime);
        }

        private Vec3d getCurrentPos() {
            return interpolate(getProgress());
        }

        private Vec3d getPreviousPos() {
            return interpolate(Math.max(0.0f, getProgress() - 0.18f));
        }

        private Vec3d interpolate(float progress) {
            double wave = Math.sin(progress * Math.PI) * lift;
            return new Vec3d(
                    lerp(origin.x, target.x, progress),
                    lerp(origin.y, target.y, progress) + wave,
                    lerp(origin.z, target.z, progress)
            );
        }

        private boolean wantToRemove() {
            return getAlphaPC() <= 0.0f;
        }
    }
}
