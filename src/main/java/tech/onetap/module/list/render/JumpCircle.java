package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import moonward.free.utils.render.RenderGlow;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.providers.ColorProvider;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInformation(moduleName = "JumpCircle", moduleDesc = "Рисует круги при прыжке", moduleCategory = ModuleCategory.RENDER)
public class JumpCircle extends Module {
    private static final Identifier CIRCLE_TEXTURE = Identifier.of("mre", "images/circle.png");
    private static final int SEGMENTS = 10;
    private static final float BASE_Y_OFFSET = 0.01f;
    private static final float ALPHA_BOOST = 1.35f;

    private final SliderSetting radius = new SliderSetting("Радиус", 1.0f, 0.5f, 2.0f, 0.1f);
    private final SliderSetting lifetimeMs = new SliderSetting("Длительность", 3200.0f, 400.0f, 6000.0f, 100.0f);
    private final CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();

    private boolean wasOnGround;
    private Vec3d lastGroundPos;
    private int airborneTicks;
    private boolean sawDescending;

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            lastGroundPos = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());
        }

        if (!onGround) {
            airborneTicks++;
            if (mc.player.getVelocity().y < -0.08) {
                sawDescending = true;
            }
        }

        boolean landed = !wasOnGround && onGround;
        if (landed) {
            if (airborneTicks > 1 && sawDescending) {
                Vec3d base = lastGroundPos != null ? lastGroundPos : mc.player.getPos();
                spawnCircle(base);
            }

            airborneTicks = 0;
            sawDescending = false;
        } else if (wasOnGround && !onGround) {
            airborneTicks = 1;
            sawDescending = false;
        }

        wasOnGround = onGround;
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        long lifeMax = (long) lifetimeMs.getValue();
        circles.removeIf(circle -> now - circle.spawnTimeMs >= lifeMax);
        if (circles.isEmpty()) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Matrix4f matrix = event.getMatrixStack().peek().getPositionMatrix();

        setupTextureSampling(CIRCLE_TEXTURE);
        RenderGlow.start(CIRCLE_TEXTURE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasVertices = false;
        for (Circle circle : circles) {
            hasVertices |= appendCircle(buffer, matrix, cameraPos, event.getTickDelta(), circle, now, lifeMax);
        }
        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } else {
            buffer.endNullable();
        }

        RenderGlow.finish();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void spawnCircle(Vec3d basePos) {
        circles.add(new Circle(new Vec3d(basePos.x, basePos.y + BASE_Y_OFFSET, basePos.z), System.currentTimeMillis(), 0.0f));
    }

    private boolean appendCircle(BufferBuilder buffer, Matrix4f matrix, Vec3d cameraPos, float tickDelta, Circle circle, long now, long lifeMax) {
        float ageMs = now - circle.spawnTimeMs;
        float lifeProgress = MathHelper.clamp(ageMs / (float) lifeMax, 0.0f, 1.0f);
        float appearDuration = Math.min(350.0f, lifeMax * 0.15f);
        float appearProgress = MathHelper.clamp(ageMs / Math.max(1.0f, appearDuration), 0.0f, 1.0f);
        float appear = 0.5f - 0.5f * (float) Math.cos(Math.PI * appearProgress);
        float fadeOut = 1.0f - lifeProgress;
        float alpha = Math.min(1.0f, appear * fadeOut * ALPHA_BOOST);
        if (alpha <= 0.001f) {
            return false;
        }

        float worldX = (float) (circle.pos.x - cameraPos.x);
        float worldY = (float) (circle.pos.y - cameraPos.y);
        float worldZ = (float) (circle.pos.z - cameraPos.z);
        float outerRadius = (float) radius.getValue() * (0.30f + 1.45f * lifeProgress) * 0.5f;
        float innerRadius = Math.max(0.001f, outerRadius * 0.02f);
        float spin = circle.rotationSeed;
        float angleStep = 360.0f / SEGMENTS;

        int outerBase = boostColor(ColorProvider.getThemeColor(), alpha, 1.35f, 1.24f, 38);
        int outerDark = darkenColor(boostColor(ColorProvider.getThemeColorTwo(), alpha, 1.20f, 1.02f, 10), 0.78f);
        int inner = boostColor(ColorProvider.getThemeColor(), alpha * 0.58f, 1.15f, 1.30f, 52);

        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = spin + i * angleStep;
            float angle2 = spin + (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float phase1 = mirroredPhase(i / (float) SEGMENTS);
            float phase2 = mirroredPhase((i + 1.0f) / SEGMENTS);
            float eased1 = 0.5f - 0.5f * (float) Math.cos(Math.PI * phase1);
            float eased2 = 0.5f - 0.5f * (float) Math.cos(Math.PI * phase2);

            int color1 = lerpColor(outerBase, outerDark, eased1);
            int color2 = lerpColor(outerBase, outerDark, eased2);

            buffer.vertex(matrix, worldX + cos1 * outerRadius, worldY, worldZ + sin1 * outerRadius)
                    .texture(cos1 * 0.5f + 0.5f, sin1 * 0.5f + 0.5f)
                    .color(color1);
            buffer.vertex(matrix, worldX + cos2 * outerRadius, worldY, worldZ + sin2 * outerRadius)
                    .texture(cos2 * 0.5f + 0.5f, sin2 * 0.5f + 0.5f)
                    .color(color2);
            buffer.vertex(matrix, worldX + cos2 * innerRadius, worldY, worldZ + sin2 * innerRadius)
                    .texture(0.5f, 0.5f)
                    .color(inner);
            buffer.vertex(matrix, worldX + cos1 * innerRadius, worldY, worldZ + sin1 * innerRadius)
                    .texture(0.5f, 0.5f)
                    .color(inner);
        }
        return true;
    }

    private void setupTextureSampling(Identifier texture) {
        int textureId = mc.getTextureManager().getTexture(texture).getGlId();
        RenderSystem.bindTexture(textureId);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
    }

    private float mirroredPhase(float progress) {
        float wrapped = progress % 1.0f;
        if (wrapped < 0.0f) {
            wrapped += 1.0f;
        }
        return wrapped >= 0.5f ? (1.0f - wrapped) * 2.0f : wrapped * 2.0f;
    }

    private int lerpColor(int from, int to, float progress) {
        int red = MathHelper.lerp(MathHelper.clamp(progress, 0.0f, 1.0f), ColorProvider.red(from), ColorProvider.red(to));
        int green = MathHelper.lerp(MathHelper.clamp(progress, 0.0f, 1.0f), ColorProvider.green(from), ColorProvider.green(to));
        int blue = MathHelper.lerp(MathHelper.clamp(progress, 0.0f, 1.0f), ColorProvider.blue(from), ColorProvider.blue(to));
        int alpha = MathHelper.lerp(MathHelper.clamp(progress, 0.0f, 1.0f), ColorProvider.alpha(from), ColorProvider.alpha(to));
        return ColorProvider.pack(red, green, blue, alpha);
    }

    private int darkenColor(int color, float factor) {
        int red = Math.max(0, Math.min(255, Math.round(ColorProvider.red(color) * factor)));
        int green = Math.max(0, Math.min(255, Math.round(ColorProvider.green(color) * factor)));
        int blue = Math.max(0, Math.min(255, Math.round(ColorProvider.blue(color) * factor)));
        return ColorProvider.pack(red, green, blue, ColorProvider.alpha(color));
    }

    private int boostColor(int color, float alphaFactor, float saturationMul, float brightnessMul, int brightnessBoost) {
        int red = ColorProvider.red(color);
        int green = ColorProvider.green(color);
        int blue = ColorProvider.blue(color);
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        float saturation = Math.min(1.0f, hsb[1] * saturationMul);
        float brightness = Math.min(1.0f, hsb[2] * brightnessMul);
        int boosted = Color.HSBtoRGB(hsb[0], saturation, brightness);
        int boostedRed = Math.min(255, ((boosted >> 16) & 0xFF) + brightnessBoost);
        int boostedGreen = Math.min(255, ((boosted >> 8) & 0xFF) + brightnessBoost);
        int boostedBlue = Math.min(255, (boosted & 0xFF) + brightnessBoost);
        int alpha = Math.max(0, Math.min(255, Math.round(alphaFactor * 255.0f)));
        return ColorProvider.pack(boostedRed, boostedGreen, boostedBlue, alpha);
    }

    private void reset() {
        circles.clear();
        wasOnGround = false;
        lastGroundPos = null;
        airborneTicks = 0;
        sawDescending = false;
    }

    private record Circle(
            Vec3d pos,
            long spawnTimeMs,
            float rotationSeed
    ) {
    }
}
