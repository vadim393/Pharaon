package tech.onetap.module.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@ModuleInformation(moduleName = "Target ESP", moduleDesc = "Визуальный эффект на текущей цели", moduleCategory = ModuleCategory.RENDER)
public class TargetESP extends Module {

    private static final Identifier GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");
    private static final Identifier SKULL_STATE_0 = Identifier.of("mre", "images/skull_state_0.png");
    private static final Identifier SKULL_STATE_1 = Identifier.of("mre", "images/skull_state_1.png");
    private static final Identifier SKULL_STATE_2 = Identifier.of("mre", "images/skull_state_2.png");
    private static final Identifier GHOST_TEXTURE = Identifier.of("mre", "images/glow.png");
    private static final int GHOST_PARTICLES = 3;
    private static final int GHOST_RING_PARTICLES = 2;
    private static final float GHOST_BASE_SIZE = 0.6f;
    private static final float GHOST_BASE_MUL = 0.05f;
    private static final float GHOST_TRAIL_LENGTH = 50.0f;

    private final ModeSetting mode = new ModeSetting("Режим", "Призраки", "Призраки", "Круг", "Ромб", "Кристаллы", "Череп");
    private final ModeSetting ghostMode = new ModeSetting("Подрежим призраков", "Призраки 1", "Призраки 1", "Призраки 2", "Призраки 3", "Призраки 4").setVisible(() -> mode.is("Призраки"));
    private final BooleanSetting ghostGlow = new BooleanSetting("Свечение", true).setVisible(() -> mode.is("Призраки") && ghostMode.is("Призраки 1"));
    private final BooleanSetting redOnAuraHit = new BooleanSetting("Покраснение при ударе", true);
    private final BooleanSetting twoColorsTheme = new BooleanSetting("Два цвета", true);
    private final ModeSetting rhombusType = new ModeSetting("Тип ромба", "Тип 1", "Тип 1", "Тип 2", "Тип 3", "Тип 4").setVisible(() -> mode.is("Ромб"));
    private final SliderSetting rhombusSize = new SliderSetting("Размер ромба", 85f, 60f, 100f, 5f).setVisible(() -> mode.is("Ромб"));
    private final SliderSetting crystalSize = new SliderSetting("Размер кристаллов", 0.12f, 0.01f, 0.5f, 0.01f).setVisible(() -> mode.is("Кристаллы"));
    private final SliderSetting crystalSpeed = new SliderSetting("Скорость вращения", 2.5f, 0.0f, 10.0f, 0.1f).setVisible(() -> mode.is("Кристаллы"));
    private final SliderSetting crystalCount = new SliderSetting("Кол-во кристаллов", 6f, 1f, 12f, 1f).setVisible(() -> mode.is("Кристаллы"));

    private final Animation animation = new Animation(Easing.EXPO_OUT, 500);
    private final Animation ghostHitAnimation = new Animation(Easing.BACK_OUT, 300);

    private Entity lastTarget = null;
    private boolean registered = false;
    private double rhombusPhase = 0.0;
    private long lastRhombusUpdateMs = 0;

    private float animationNurik = 0;
    private long currentTime = System.currentTimeMillis();
    private final LinkedList<Vec3d> targetHistory = new LinkedList<>();
    private final List<GhostParticle> ghostParticles = new ArrayList<>();
    private float ghostRotation = 0.0f;
    private long ghostLastFrame = System.currentTimeMillis();
    private String lastGhostMode = "Призраки 1";

    private final WorldRenderEvents.Last listener = context -> {
        onRenderWorldLast(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
    };

    @Override
    public void onEnable() {
        if (!registered) {
            WorldRenderEvents.LAST.register(listener);
            registered = true;
        }
        super.onEnable();
    }

    private void onRenderWorldLast(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!isEnabled()) return;

        Entity target = Onetap.getInstance().getModuleStorage().get(KillAura.class).getTarget();

        if (target != null && target != mc.player && !(target instanceof ArmorStandEntity)) {
            if (lastTarget != target) {
                targetHistory.clear();
                ghostParticles.clear();
            }
            lastTarget = target;
            animation.run(1);
        } else {
            animation.run(0);
            if (animation.getValue() == 0) {
                lastTarget = null;
                targetHistory.clear();
                ghostParticles.clear();
            }
        }

        if (lastTarget == null || animation.getValue() <= 0.01) return;

        if (mode.is("Призраки") && !ghostMode.getValue().equals(lastGhostMode)) {
            ghostParticles.clear();
            ghostRotation = 0.0f;
            ghostLastFrame = System.currentTimeMillis();
            lastGhostMode = ghostMode.getValue();
        }

        switch (mode.getValue()) {
            case "Круг" -> drawJelloMode(matrices, camera, tickDelta);
            case "Ромб" -> drawRhombus(matrices, camera, tickDelta);
            case "Призраки" -> drawGhostMode(matrices, camera, tickDelta);
            case "Кристаллы" -> drawCrystals(matrices, camera, tickDelta);
            case "Череп" -> drawSkull(matrices, camera, tickDelta);
        }
    }

    private void drawGhostMode(MatrixStack matrices, Camera camera, float tickDelta) {
        switch (ghostMode.getValue()) {
            case "Призраки 1" -> drawGhostsModern(matrices, camera, tickDelta);
            case "Призраки 2" -> drawGhostsGithub(matrices, camera, tickDelta);
            case "Призраки 3" -> drawSpirits(matrices, camera, tickDelta);
            case "Призраки 4" -> drawGhostsOrbit(matrices, camera, tickDelta);
        }
    }

    private float getAuraHurtFactor(Entity entity) {
        if (redOnAuraHit.getValue() && entity instanceof LivingEntity living) {
            return MathHelper.clamp(living.hurtTime / 10.0f, 0.0f, 1.0f);
        }
        return 0.0f;
    }

    private int mixWithHurt(int baseColor, float hurt) {
        if (hurt <= 0.0f) return baseColor;
        return interpolateColor(baseColor, 0xFFFF5555, hurt);
    }

    private int interpolateColor(int color1, int color2, float factor) {
        if (factor <= 0.0F) return color1;
        if (factor >= 1.0F) return color2;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        return (0xFF << 24) | ((int) (r1 + (r2 - r1) * factor) << 16) | ((int) (g1 + (g2 - g1) * factor) << 8) | (int) (b1 + (b2 - b1) * factor);
    }

    private int getGradient(int color1, int color2, float factor) {
        return interpolateColor(color1, color2, factor);
    }

    private double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    private void drawRhombus(MatrixStack matrices, Camera camera, float tickDelta) {
        if (lastTarget == null) return;

        double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta);
        double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta);
        double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta);
        Vec3d camPos = camera.getPos();

        float rawSize = rhombusSize.getFloatValue() / 65f;
        float displayedSize = MathHelper.lerp(animation.getValue(), 1f, rawSize);
        if (rhombusType.is("Тип 2")) displayedSize *= 1.5f;
        float halfSize = displayedSize / 2f;

        long now = System.currentTimeMillis();
        double deltaTime = lastRhombusUpdateMs == 0 ? 0 : (now - lastRhombusUpdateMs) / 1000.0;
        lastRhombusUpdateMs = now;
        rhombusPhase += 2.0 * deltaTime;

        matrices.push();
        matrices.translate(x - camPos.x, y + lastTarget.getHeight() / 2.0 - camPos.y, z - camPos.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (Math.sin(rhombusPhase) * 180.0)));

        float hurtFactor = getAuraHurtFactor(lastTarget);
        int baseColor = mixWithHurt(ColorProvider.getThemeColor(), hurtFactor);
        int color = ColorProvider.setAlpha(baseColor, 255);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, Identifier.of("mre", "images/targetesp/target" + rhombusType.getValue().replace("Тип ", "") + ".png"));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(mat, -halfSize, -halfSize + displayedSize, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(mat, halfSize, -halfSize + displayedSize, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(mat, halfSize, -halfSize, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(mat, -halfSize, -halfSize, 0.0f).texture(0.0f, 0.0f).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        matrices.pop();
    }

    private void drawJelloMode(MatrixStack matrices, Camera camera, float tickDelta) {
        double tPosX = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camera.getPos().x;
        double tPosY = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camera.getPos().y;
        double tPosZ = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camera.getPos().z;

        float height = lastTarget.getHeight() + 0.2f;
        float cycle = 2500.0f;
        float phase = (System.currentTimeMillis() % (long) cycle) / cycle;
        float orbit = phase * (float) (Math.PI * 2.0);
        float wave = 0.5f - 0.5f * (float) Math.cos(orbit);
        float speed = (float) Math.sin(orbit);
        float headY = height * wave;
        float trailShift = height * 0.24f * speed * Math.abs(speed);
        float tailY = headY - trailShift;
        float trailFade = Math.abs(speed);

        matrices.push();
        matrices.translate(tPosX, tPosY, tPosZ);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();

        if (mc.player.canSee(lastTarget)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        int color = ColorProvider.getThemeColor();
        int headColor = ColorProvider.setAlpha(color, (int) (210 * animation.getValue()));
        int tailColor = ColorProvider.setAlpha(color, (int) (75 * animation.getValue() * trailFade * trailFade));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i <= 360; ++i) {
            double angle = Math.toRadians(i);
            float localRadius = lastTarget.getWidth() * (0.97f + 0.06f * (1.0f - wave));
            float x = (float) (Math.cos(angle) * localRadius);
            float z = (float) (Math.sin(angle) * localRadius);
            buffer.vertex(matrix, x, tailY, z).color(tailColor);
            buffer.vertex(matrix, x, headY, z).color(headColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; ++i) {
            double angle = Math.toRadians(i);
            float localRadius = lastTarget.getWidth() * (0.97f + 0.06f * (1.0f - wave));
            float x = (float) (Math.cos(angle) * localRadius);
            float z = (float) (Math.sin(angle) * localRadius);
            buffer.vertex(matrix, x, headY, z).color(headColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableCull();
        if (mc.player.canSee(lastTarget)) RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        matrices.pop();
    }

    private void drawGhostsModern(MatrixStack matrices, Camera camera, float tickDelta) {
        drawGhostTrail(matrices, camera, tickDelta);
    }



    private void drawGhostTrail(MatrixStack matrices, Camera camera, float tickDelta) {
        float progress = (float) animation.getValue();
        double targetX = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta);
        double targetY = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta);
        double targetZ = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta);

        if (ghostParticles.size() < TargetESP.GHOST_PARTICLES) {
            for (int i = ghostParticles.size(); i < TargetESP.GHOST_PARTICLES; i++) {
                ghostParticles.add(new GhostParticle(new Vec3d(targetX, targetY, targetZ), GHOST_BASE_SIZE));
            }
        } else if (ghostParticles.size() > TargetESP.GHOST_PARTICLES) {
            ghostParticles.subList(TargetESP.GHOST_PARTICLES, ghostParticles.size()).clear();
        }

        long now = System.currentTimeMillis();
        float delta = Math.max(1, now - ghostLastFrame);
        ghostLastFrame = now;
        float fpsFactor = 500.0f / Math.max(mc.getCurrentFps(), 5);
        ghostRotation += (20.0f / 55.0f) * delta;

        float hurtTrigger = (lastTarget instanceof LivingEntity living && living.hurtTime > 7) ? 1.0f : 0.0f;
        ghostHitAnimation.run(hurtTrigger);
        float hitScale = ghostHitAnimation.getValue();

        RenderSystem.enableBlend();
        if (ghostGlow.getValue()) {
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }
        RenderSystem.setShaderTexture(0, GHOST_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableCull();
        if (ghostGlow.getValue()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int baseThemeColor = mixWithHurt(ColorProvider.getThemeColor(), getAuraHurtFactor(lastTarget));
        int secondThemeColor = mixWithHurt(ColorProvider.getThemeColorTwo(), getAuraHurtFactor(lastTarget));

        for (int particleIndex = 0; particleIndex < TargetESP.GHOST_PARTICLES; particleIndex++) {
            GhostParticle particle = ghostParticles.get(particleIndex);

            float angleOffset = particleIndex * (360.0f / TargetESP.GHOST_PARTICLES);
            float currentAngle = ghostRotation + angleOffset;
            double radians = Math.toRadians(currentAngle);

            float radius = GHOST_BASE_SIZE - hitScale * GHOST_BASE_SIZE;
            double x;
            double z;
            double vertical;
            if (false) {
                double progressRing = (ghostRotation * 0.0035 + (particleIndex / (double) TargetESP.GHOST_PARTICLES)) % 1.0;
                double height = MathHelper.lerp(progressRing, 0.05, lastTarget.getHeight() + 0.25);
                double ringRadians = progressRing * Math.PI * 2.0;
                x = Math.sin(ringRadians) * radius;
                z = Math.cos(ringRadians) * radius;
                vertical = height;
            } else {
                x = Math.sin(radians) * radius;
                z = Math.cos(radians) * radius;
                vertical = 0.2 + lastTarget.getHeight() / 2.0 * Math.sin(Math.toRadians(ghostRotation / (particleIndex + 1.0f)));
            }

            Vec3d destination = new Vec3d(targetX + x, targetY + vertical, targetZ + z);
            float mul = GHOST_BASE_MUL * fpsFactor;
            particle.motion = destination.subtract(particle.position).multiply(mul, mul, mul);
            particle.position = particle.position.add(particle.motion);
            particle.trail.add(new Vector4f((float) particle.position.x, (float) particle.position.y + 0.7f, (float) particle.position.z, GHOST_TRAIL_LENGTH));

            Iterator<Vector4f> iterator = particle.trail.iterator();
            int trailIndex = 0;
            while (iterator.hasNext()) {
                Vector4f point = iterator.next();
                if (point.w <= 0.0f) {
                    iterator.remove();
                    continue;
                }

                float miniSize = particle.size * point.w / GHOST_TRAIL_LENGTH;
                double posX = point.x - camera.getPos().x;
                double posY = point.y - camera.getPos().y;
                double posZ = point.z - camera.getPos().z;

                int trailColor = baseThemeColor;
                if (twoColorsTheme.getValue()) {
                    trailColor = ColorProvider.gradient(15, trailIndex * 15 + particleIndex * 120, baseThemeColor, secondThemeColor);
                }
                int alpha = (int) ((point.w / GHOST_TRAIL_LENGTH) * 255.0f * progress);
                int color = ColorProvider.setAlpha(trailColor, alpha);

                matrices.push();
                matrices.translate(posX, posY, posZ);
                matrices.multiply(camera.getRotation());
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = miniSize / 2.0f;
                buffer.vertex(matrix, -half, half, 0.0f).texture(0f, 1f).color(color);
                buffer.vertex(matrix, half, half, 0.0f).texture(1f, 1f).color(color);
                buffer.vertex(matrix, half, -half, 0.0f).texture(1f, 0f).color(color);
                buffer.vertex(matrix, -half, -half, 0.0f).texture(0f, 0f).color(color);
                matrices.pop();

                point.y += 0.004f * fpsFactor;
                point.w -= 0.3f * fpsFactor;
                trailIndex++;
            }

            particle.alpha = 1.0f;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
    }

    private void drawGhostsGithub(MatrixStack matrices, Camera camera, float tickDelta) {
        float tProgress = (float) animation.getValue();
        double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camera.getPos().getX();
        double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camera.getPos().getY() + (lastTarget.getHeight() / 2.0);
        double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camera.getPos().getZ();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int themeColor = mixWithHurt(ColorProvider.getThemeColor(), getAuraHurtFactor(lastTarget));
        int whiteGlow = mixWithHurt(0xFFFFFFFF, getAuraHurtFactor(lastTarget));
        int finalConstantColor = interpolateColor(themeColor, whiteGlow, 0.15f);


        matrices.push();
        matrices.translate(x, y, z);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float time = (System.currentTimeMillis() % 2500) / 2500f * (float) Math.PI * 2f;
        float radius = lastTarget.getWidth() * 1.65f;
        int trailSegments = 126;

        float electronBaseScale = 0.25f * tProgress;

        for (int i = 0; i < 3; i++) {
            float offset = i * ((float) Math.PI / 1.5f);
            float currentOrbitTime = time * 4.0f + offset;

            for (int j = 1; j <= trailSegments; j++) {
                float trailTime = currentOrbitTime - ((j / (float) trailSegments) *2f);
                float fade = 1.0f - (j / (float) (trailSegments + 1));

                float tx = (float) (radius * Math.cos(trailTime) * Math.cos(offset) - radius * Math.sin(trailTime) * Math.sin(offset) * 0.5f);
                float ty = (float) (radius * Math.sin(trailTime) * 0.8f);
                float tz = (float) (radius * Math.cos(trailTime) * Math.sin(offset) + radius * Math.sin(trailTime) * Math.cos(offset) * 0.5f);

                int trailAlpha = (int) (tProgress * 180f * fade * fade);
                int currentTrailColor = finalConstantColor;
                if (twoColorsTheme.getValue()) {
                    currentTrailColor = getGradient(finalConstantColor, finalConstantColor, (float) (Math.sin(trailTime * 1.5f + i) * 0.5f + 0.5f));
                }
                drawQuad(buffer, matrices, camera, tx, ty, tz, electronBaseScale * (0.4f + 0.6f * fade), ColorProvider.setAlpha(currentTrailColor, trailAlpha));
            }

            float ex = (float) (radius * Math.cos(currentOrbitTime) * Math.cos(offset) - radius * Math.sin(currentOrbitTime) * Math.sin(offset) * 0.5f);
            float ey = (float) (radius * Math.sin(currentOrbitTime) * 0.8f);
            float ez = (float) (radius * Math.cos(currentOrbitTime) * Math.sin(offset) + radius * Math.sin(currentOrbitTime) * Math.cos(offset) * 0.5f);

            int currentHeadColor = finalConstantColor;
            if (twoColorsTheme.getValue()) {
                currentHeadColor = getGradient(finalConstantColor, finalConstantColor, (float) (Math.sin(currentOrbitTime * 1.5f + i) * 0.5f + 0.5f));
            }
            drawQuad(buffer, matrices, camera, ex, ey, ez, electronBaseScale, ColorProvider.setAlpha(currentHeadColor, (int) (tProgress * 255f)));
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
    }

    private void drawSpirits(MatrixStack matrices, Camera camera, float tickDelta) {
        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        animation.run(aura.getTarget() != null && aura.isEnabled() ? 1 : 0);

        if (animation.getValue() == 0.0) {
            return;
        }

        if (aura.getTarget() != null) {
            if (lastTarget == null) {
                currentTime = System.currentTimeMillis();
            }
            lastTarget = aura.getTarget();
        }

        if (lastTarget == null) {
            return;
        }

        float hurtFactorSpeed = 0.0f;
        if (lastTarget instanceof LivingEntity living) {
            hurtFactorSpeed = MathHelper.clamp(living.hurtTime / 10.0f, 0.0f, 1.0f);
        }
        float speedMultiplier = 1.5f + (1.2f * hurtFactorSpeed);

        animationNurik += (3 * (System.currentTimeMillis() - currentTime) / 800.0f) * speedMultiplier;
        currentTime = System.currentTimeMillis();

        float hurt = getAuraHurtFactor(lastTarget);
        float hitEffect = redOnAuraHit.getValue() ? (float) Math.pow(hurt, 5) : 0.0f;

        int themeColor = mixWithHurt(ColorProvider.getThemeColor(), hurt);
        int whiteGlow = mixWithHurt(0xFFFFFFFF, hurt);
        int finalColor = interpolateColor(themeColor, whiteGlow, 0.15f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camera.getPos().getX();
        double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camera.getPos().getY();
        double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camera.getPos().getZ();

        int rings = 3;
        int particles = 16;
        int maxHeight = rings * 3;

        matrices.push();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i < maxHeight; i += rings) {
            for (int j = 0; j < particles; ++j) {
                float phase = animationNurik + j * 0.095f;
                float radius = 0.85f;
                float yOffset = 0.5f;
                int shift = i * i;

                matrices.push();
                matrices.translate(
                        x + radius * MathHelper.sin(phase + shift),
                        y + yOffset + 0.3f * MathHelper.sin(animationNurik + j * 0.2f) + 0.2f * i,
                        z + radius * MathHelper.cos(phase - shift)
                );

                float scale = (float) (animation.getValue() * (0.0056f + j / 2000.0f));
                matrices.scale(scale, scale, scale);
                matrices.multiply(camera.getRotation());

                int min = -35;
                int size = 50;
                int alpha = (int) (animation.getValue() * 255f);

                int currentParticleColor = finalColor;
                if (twoColorsTheme.getValue()) {
                    currentParticleColor = getGradient(finalColor, finalColor, (float) (Math.sin(phase * 1.5f + i) * 0.5f + 0.5f));
                }

                int color = ColorProvider.setAlpha(currentParticleColor, alpha);

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                buffer.vertex(matrix, min, min + size, 0.0f).texture(0.0f, 1.0f).color(color);
                buffer.vertex(matrix, min + size, min + size, 0.0f).texture(1.0f, 1.0f).color(color);
                buffer.vertex(matrix, min + size, min, 0.0f).texture(1.0f, 0.0f).color(color);
                buffer.vertex(matrix, min, min, 0.0f).texture(0.0f, 0.0f).color(color);
                matrices.pop();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private void drawGhostsOrbit(MatrixStack matrices, Camera camera, float tickDelta) {
        float progress = (float) animation.getValue();
        if (progress <= 0.0f) {
            return;
        }

        double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta) - camera.getPos().x;
        double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta) - camera.getPos().y + (lastTarget.getHeight() / 2.0);
        double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta) - camera.getPos().z;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        float hurtFactor = getAuraHurtFactor(lastTarget);
        int colorFirst = mixWithHurt(ColorProvider.getThemeColor(), hurtFactor);
        int colorSecond = mixWithHurt(ColorProvider.getThemeColorTwo(), hurtFactor);

        matrices.push();
        matrices.translate(x, y, z);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float time = (System.currentTimeMillis() % 3000L) / 3000.0f * (float) Math.PI * 2.0f;
        float radius = lastTarget.getWidth() * 1.65f;
        int trailSegments = 36;
        float trailLength = 1.5f;
        float orbitScale = 0.2f * progress;

        for (int orbitIndex = 0; orbitIndex < 3; orbitIndex++) {
            float offset = orbitIndex * ((float) Math.PI / 1.5f);
            float currentOrbitTime = time * 3.0f + offset;

            for (int segmentIndex = 1; segmentIndex <= trailSegments; segmentIndex++) {
                float timeDelta = segmentIndex / (float) trailSegments * trailLength;
                float trailTime = currentOrbitTime - timeDelta;
                float fade = 1.0f - segmentIndex / (float) (trailSegments + 1);

                float tx = (float) (radius * Math.cos(trailTime) * Math.cos(offset) - radius * Math.sin(trailTime) * Math.sin(offset) * 0.5f);
                float ty = (float) (radius * Math.sin(trailTime) * 0.8f);
                float tz = (float) (radius * Math.cos(trailTime) * Math.sin(offset) + radius * Math.sin(trailTime) * Math.cos(offset) * 0.5f);

                float scale = orbitScale * (0.4f + 0.6f * fade);
                int alpha = (int) (progress * 180.0f * fade * fade);
                int currentColor = colorFirst;
                if (twoColorsTheme.getValue()) {
                    float gradientProgress = (float) (Math.sin(trailTime * 1.5f + orbitIndex) * 0.5f + 0.5f);
                    currentColor = getGradient(colorFirst, colorSecond, gradientProgress);
                }

                drawQuad(buffer, matrices, camera, tx, ty, tz, scale, ColorProvider.setAlpha(currentColor, alpha));
            }

            float ex = (float) (radius * Math.cos(currentOrbitTime) * Math.cos(offset) - radius * Math.sin(currentOrbitTime) * Math.sin(offset) * 0.5f);
            float ey = (float) (radius * Math.sin(currentOrbitTime) * 0.8f);
            float ez = (float) (radius * Math.cos(currentOrbitTime) * Math.sin(offset) + radius * Math.sin(currentOrbitTime) * Math.cos(offset) * 0.5f);

            int currentColor = colorFirst;
            if (twoColorsTheme.getValue()) {
                float gradientProgress = (float) (Math.sin(currentOrbitTime * 1.5f + orbitIndex) * 0.5f + 0.5f);
                currentColor = getGradient(colorFirst, colorSecond, gradientProgress);
            }

            drawQuad(buffer, matrices, camera, ex, ey, ez, orbitScale, ColorProvider.setAlpha(currentColor, (int) (progress * 255.0f)));
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
    }

    private void drawCrystals(MatrixStack matrices, Camera camera, float tickDelta) {
        float tProgress = (float) animation.getValue();
        double targetX = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta);
        double targetY = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta);
        double targetZ = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta);
        Vec3d camPos = camera.getPos();

        float hurtFactor = getAuraHurtFactor(lastTarget);
        int baseColor = mixWithHurt(ColorProvider.getThemeColor(), hurtFactor);
        int secondColor = mixWithHurt(ColorProvider.getThemeColorTwo(), hurtFactor);

        float width = lastTarget.getWidth() * 1.6f;
        float timeOffset = (System.currentTimeMillis() % 4000) / 4000f * 360f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (mc.player.canSee(lastTarget)) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();

        int numLayers = 4;
        float crystalsPerLayer = (float) crystalCount.getValue();
        float speed = (float) crystalSpeed.getValue();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        int crystalIdx = 0;
        for (int layer = 0; layer < numLayers; layer++) {
            float baseHeight = lastTarget.getHeight() * ((layer + 0.5f) / numLayers);
            for (int i = 0; i < 360; i += (360 / crystalsPerLayer)) {
                float radiusScale = 1.2f - 0.5f * tProgress;
                float angle = i + layer * 25f + timeOffset * speed;
                float currentRadius = width * radiusScale;
                float sin = (float) (Math.sin(Math.toRadians(angle)) * currentRadius);
                float cos = (float) (Math.cos(Math.toRadians(angle)) * currentRadius);

                float crystalDelay = crystalIdx * 0.03f;
                float crystalAppearProgress = Math.max(0, Math.min(1, (tProgress - crystalDelay) / (1.0f - crystalDelay)));
                if (crystalAppearProgress < 0.01f) {
                    crystalIdx++;
                    continue;
                }

                float size = crystalSize.getFloatValue() * crystalAppearProgress;
                float heightOffset = baseHeight + (1.0f - crystalAppearProgress) * -0.5f;

                matrices.push();
                matrices.translate(targetX - camPos.x + sin, targetY - camPos.y + heightOffset, targetZ - camPos.z + cos);

                Vec3d crystalPos = new Vec3d(targetX + sin, targetY + heightOffset, targetZ + cos);
                Vec3d targetPos = new Vec3d(targetX, targetY + lastTarget.getHeight() / 2.0, targetZ);
                Vector3f directionToTarget = new Vector3f(
                        (float) (targetPos.x - crystalPos.x),
                        (float) (targetPos.y - crystalPos.y),
                        (float) (targetPos.z - crystalPos.z)
                ).normalize();
                matrices.multiply(new Quaternionf().rotationTo(new Vector3f(0.0f, 1.0f, 0.0f), directionToTarget));

                int currentCrystalColor = baseColor;
                if (twoColorsTheme.getValue()) {
                    float gradientProgress = (float) (Math.sin(Math.toRadians(angle) + crystalIdx) * 0.5f + 0.5f);
                    currentCrystalColor = getGradient(baseColor, secondColor, gradientProgress);
                }

                drawSolidCrystalToBuffer(buffer, matrices, size, ColorProvider.setAlpha(currentCrystalColor, (int) (255 * tProgress * crystalAppearProgress)));
                matrices.pop();
                crystalIdx++;
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        setupGlowTextureSampling();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        BufferBuilder glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        crystalIdx = 0;
        for (int layer = 0; layer < numLayers; layer++) {
            float baseHeight = lastTarget.getHeight() * ((layer + 0.5f) / numLayers);
            for (int i = 0; i < 360; i += (360 / crystalsPerLayer)) {
                float radiusScale = 1.2f - 0.5f * tProgress;
                float angle = i + layer * 25f + timeOffset * speed;
                float currentRadius = width * radiusScale;
                float sin = (float) (Math.sin(Math.toRadians(angle)) * currentRadius);
                float cos = (float) (Math.cos(Math.toRadians(angle)) * currentRadius);

                float crystalDelay = crystalIdx * 0.03f;
                float crystalAppearProgress = Math.max(0, Math.min(1, (tProgress - crystalDelay) / (1.0f - crystalDelay)));
                if (crystalAppearProgress < 0.01f) {
                    crystalIdx++;
                    continue;
                }

                float heightOffset = baseHeight + (1.0f - crystalAppearProgress) * -0.5f;
                float bloomAlpha = tProgress * crystalAppearProgress * (0.35f + 0.1f * (float) Math.sin(System.currentTimeMillis() / 300.0 + crystalIdx * 0.5));

                int currentGlowColor = baseColor;
                if (twoColorsTheme.getValue()) {
                    float gradientProgress = (float) (Math.sin(Math.toRadians(angle) + crystalIdx) * 0.5f + 0.5f);
                    currentGlowColor = getGradient(baseColor, secondColor, gradientProgress);
                }

                matrices.push();
                matrices.translate(targetX - camPos.x + sin, targetY - camPos.y + heightOffset, targetZ - camPos.z + cos);
                matrices.multiply(camera.getRotation());

                Matrix4f m = matrices.peek().getPositionMatrix();
                float hs = crystalSize.getFloatValue() * 6.5f * crystalAppearProgress / 2.0f;
                int color = ColorProvider.setAlpha(currentGlowColor, (int) (255 * bloomAlpha));

                glowBuffer.vertex(m, -hs, -hs, 0).texture(0, 0).color(color);
                glowBuffer.vertex(m, -hs, hs, 0).texture(0, 1).color(color);
                glowBuffer.vertex(m, hs, hs, 0).texture(1, 1).color(color);
                glowBuffer.vertex(m, hs, -hs, 0).texture(1, 0).color(color);
                matrices.pop();
                crystalIdx++;
            }
        }
        BufferRenderer.drawWithGlobalProgram(glowBuffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private void drawSkull(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!(lastTarget instanceof LivingEntity living)) return;

        double x = interpolate(lastTarget.getX(), lastTarget.lastRenderX, tickDelta);
        double y = interpolate(lastTarget.getY(), lastTarget.lastRenderY, tickDelta);
        double z = interpolate(lastTarget.getZ(), lastTarget.lastRenderZ, tickDelta);

        Vec3d camPos = camera.getPos();
        double entX = x - camPos.x;
        double entY = y - camPos.y + lastTarget.getHeight() / 2.0;
        double entZ = z - camPos.z;

        float currentHP = living.getHealth();
        float maxHP = Math.max(1.0f, living.getMaxHealth());
        float hpPercent = MathHelper.clamp(currentHP / maxHP, 0.0f, 1.0f);
        int hurtTicks = living.hurtTime;
        boolean isHurt = hurtTicks > 0;
        float lowHpShake = (1.0f - hpPercent) * 0.08f;
        float hurtShake = (hurtTicks / 8.0f) * 0.2f;
        float totalShake = lowHpShake + hurtShake;

        if (totalShake > 0.001f) {
            entX += (Math.random() - 0.4) * totalShake;
            entY += (Math.random() - 0.4) * totalShake;
            entZ += (Math.random() - 0.4) * totalShake;
        }

        matrices.push();
        matrices.translate(entX, entY, entZ);

        float alphaPC = (float) animation.getValue();
        Identifier skullTexture;
        if (hpPercent > 0.6f) {
            skullTexture = SKULL_STATE_0;
        } else if (hpPercent > 0.3f) {
            skullTexture = SKULL_STATE_1;
        } else {
            skullTexture = SKULL_STATE_2;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int baseColor = ColorProvider.getThemeColor();
        int redColor = ColorProvider.pack(255, 50, 50, (int) (alphaPC * 255));
        int skullColor;
        if (isHurt) {
            float hurtPC = (float) Math.sin((double) hurtTicks * (Math.PI / 10.0));
            skullColor = ColorProvider.interpolateColor(baseColor, redColor, hurtPC);
        } else if (hpPercent < 0.3f) {
            skullColor = ColorProvider.interpolateColor(baseColor, redColor, (1.0f - hpPercent / 0.3f) * 0.4f);
        } else {
            skullColor = baseColor;
        }
        int finalColor = ColorProvider.setAlpha(skullColor, (int) (alphaPC * 255));

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, skullTexture);

        matrices.multiply(camera.getRotation());

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float skullSize = 0.6f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -skullSize, -skullSize, 0.0f).texture(0.0f, 1.0f).color(finalColor);
        buffer.vertex(matrix, skullSize, -skullSize, 0.0f).texture(1.0f, 1.0f).color(finalColor);
        buffer.vertex(matrix, skullSize, skullSize, 0.0f).texture(1.0f, 0.0f).color(finalColor);
        buffer.vertex(matrix, -skullSize, skullSize, 0.0f).texture(0.0f, 0.0f).color(finalColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        float glowSize = skullSize * (1.5f + (1.0f - hpPercent) * 0.5f);
        int glowAlpha = (int) (alphaPC * (100.0f + (1.0f - hpPercent) * 50.0f));
        int glowColor = ColorProvider.setAlpha(skullColor, glowAlpha);

        BufferBuilder glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        glowBuffer.vertex(matrix, -glowSize, -glowSize, 0.0f).texture(0.0f, 0.0f).color(glowColor);
        glowBuffer.vertex(matrix, -glowSize, glowSize, 0.0f).texture(0.0f, 1.0f).color(glowColor);
        glowBuffer.vertex(matrix, glowSize, glowSize, 0.0f).texture(1.0f, 1.0f).color(glowColor);
        glowBuffer.vertex(matrix, glowSize, -glowSize, 0.0f).texture(1.0f, 0.0f).color(glowColor);
        BufferRenderer.drawWithGlobalProgram(glowBuffer.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        matrices.pop();
    }

    private void setupGlowTextureSampling() {
        int textureId = mc.getTextureManager().getTexture(GLOW_TEXTURE).getGlId();
        RenderSystem.bindTexture(textureId);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
    }

    private void drawQuad(BufferBuilder buffer, MatrixStack ms, Camera camera, float x, float y, float z, float scale, int color) {
        ms.push();
        ms.translate(x, y, z);
        ms.scale(scale, scale, scale);
        ms.multiply(camera.getRotation());
        Matrix4f m = ms.peek().getPositionMatrix();
        buffer.vertex(m, -1f, 1f, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(m, 1f, 1f, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(m, 1f, -1f, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(m, -1f, -1f, 0.0f).texture(0.0f, 0.0f).color(color);
        ms.pop();
    }

    private void drawSolidCrystalToBuffer(BufferBuilder b, MatrixStack ms, float size, int color) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float w = size / 2f, h = size;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, bCol = color & 0xFF, a = (color >> 24) & 0xFF;
        int darkColor = (((int) (r * 0.6f)) << 16) | (((int) (g * 0.6f)) << 8) | ((int) (bCol * 0.6f)) | (a << 24);

        b.vertex(m, 0, h, 0).color(color); b.vertex(m, -w, 0, -w).color(color); b.vertex(m, w, 0, -w).color(color);
        b.vertex(m, 0, h, 0).color(darkColor); b.vertex(m, w, 0, -w).color(darkColor); b.vertex(m, w, 0, w).color(darkColor);
        b.vertex(m, 0, h, 0).color(color); b.vertex(m, w, 0, w).color(color); b.vertex(m, -w, 0, w).color(color);
        b.vertex(m, 0, h, 0).color(darkColor); b.vertex(m, -w, 0, w).color(darkColor); b.vertex(m, -w, 0, -w).color(darkColor);
        b.vertex(m, 0, -h, 0).color(darkColor); b.vertex(m, w, 0, -w).color(darkColor); b.vertex(m, -w, 0, -w).color(darkColor);
        b.vertex(m, 0, -h, 0).color(color); b.vertex(m, w, 0, w).color(color); b.vertex(m, w, 0, -w).color(color);
        b.vertex(m, 0, -h, 0).color(darkColor); b.vertex(m, -w, 0, w).color(darkColor); b.vertex(m, w, 0, w).color(darkColor);
        b.vertex(m, 0, -h, 0).color(color); b.vertex(m, -w, 0, -w).color(color); b.vertex(m, -w, 0, w).color(color);
    }

    private static class GhostParticle {
        private Vec3d position;
        private Vec3d motion = Vec3d.ZERO;
        private final List<Vector4f> trail = new ArrayList<>();
        private final float size;
        private float alpha = 1.0f;

        private GhostParticle(Vec3d position, float size) {
            this.position = position;
            this.size = size;
        }
    }

}
