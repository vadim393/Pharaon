package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInformation(moduleName = "Fire Fly", moduleDesc = "Светлячки вокруг игрока", moduleCategory = ModuleCategory.RENDER)
public class FireFly extends Module {

    private static final Identifier GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");
    private static final float CLASSIC_SPEED = 0.35f;
    private static final float CLASSIC_SPAWN_RADIUS = 35f;
    private static final int CLASSIC_TRAIL_LENGTH = 70;
    private static final long FIREFLY_LIFETIME_MS = 8000L;
    private static final long FIREFLY_FADE_MS = 500L;
    private static final int[] FIREFLY_PALETTE = {
            0xFFFFD700,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFFFF69B4,
            0xFFFFA500,
            0xFF00BFFF
    };

    public final ModeSetting mode = new ModeSetting("Режим", "Светлячки", "Светлячки", "Обычные");
    public final SliderSetting count = new SliderSetting("Количество", 20, 10, 300, 1);
    public final SliderSetting ffSpeed = new SliderSetting("Скорость", 0.15f, 0.05f, 0.5f, 0.05f)
            .setVisible(() -> mode.is("Светлячки"));
    public final SliderSetting ffRadius = new SliderSetting("Радиус", 25.0f, 10.0f, 50.0f, 5.0f)
            .setVisible(() -> mode.is("Светлячки"));
    public final SliderSetting ffTrail = new SliderSetting("Длина хвостика", 20.0f, 5.0f, 40.0f, 5.0f)
            .setVisible(() -> mode.is("Светлячки"));
    public final SliderSetting ffSize = new SliderSetting("Размер", 0.22f, 0.08f, 0.6f, 0.01f)
            .setVisible(() -> mode.is("Светлячки"));
    public final BooleanSetting ffRandomColor = new BooleanSetting("Рандомный цвет", true)
            .setVisible(() -> mode.is("Светлячки"));
    public final BooleanSetting themeColor = new BooleanSetting("Цвет от темы", true)
            .setVisible(() -> !mode.is("Светлячки") || !ffRandomColor.getValue());

    private static final class TrailPoint {
        private final double x;
        private final double y;
        private final double z;

        private TrailPoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class ClassicFireFly {
        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private double velX;
        private double velY;
        private double velZ;
        private double targetVelX;
        private double targetVelY;
        private double targetVelZ;
        private final int baseRandomColor;
        private final int maxTrailLength;
        private final Random random = new Random();
        private final List<TrailPoint> trail = new ArrayList<>();
        private final long spawnTime = System.currentTimeMillis();
        private long lastDirectionChange = System.currentTimeMillis();

        private ClassicFireFly(double x, double y, double z, double velX, double velY, double velZ, int baseRandomColor, int maxTrailLength) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.velX = velX;
            this.velY = velY;
            this.velZ = velZ;
            this.targetVelX = velX;
            this.targetVelY = velY;
            this.targetVelZ = velZ;
            this.baseRandomColor = baseRandomColor;
            this.maxTrailLength = maxTrailLength;
        }

        private void update(float speedMult, float maxSpeed, Vec3d playerPos) {
            prevX = x;
            prevY = y;
            prevZ = z;

            long timeSinceChange = System.currentTimeMillis() - lastDirectionChange;
            if (timeSinceChange > 2000 + random.nextInt(2000)) {
                double angle = Math.toRadians(random.nextDouble() * 360.0);
                double pitch = Math.toRadians((random.nextDouble() - 0.5) * 40.0);

                targetVelX = -Math.sin(angle) * Math.cos(pitch) * speedMult;
                targetVelY = Math.sin(pitch) * speedMult * 0.3;
                targetVelZ = Math.cos(angle) * Math.cos(pitch) * speedMult;
                lastDirectionChange = System.currentTimeMillis();
            }

            double dx = playerPos.x - x;
            double dy = playerPos.y + 1.0 - y;
            double dz = playerPos.z - z;
            double distanceToPlayer = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distanceToPlayer > CLASSIC_SPAWN_RADIUS) {
                targetVelX += (dx / distanceToPlayer) * speedMult * 0.15;
                targetVelY += (dy / distanceToPlayer) * speedMult * 0.15;
                targetVelZ += (dz / distanceToPlayer) * speedMult * 0.15;
            }

            double lerpFactor = 0.02;
            velX += (targetVelX - velX) * lerpFactor;
            velY += (targetVelY - velY) * lerpFactor;
            velZ += (targetVelZ - velZ) * lerpFactor;

            double wobble = 0.03;
            velX += (random.nextDouble() - 0.5) * wobble;
            velY += (random.nextDouble() - 0.5) * wobble;
            velZ += (random.nextDouble() - 0.5) * wobble;

            velX = MathHelper.clamp(velX, -maxSpeed, maxSpeed);
            velY = MathHelper.clamp(velY, -maxSpeed, maxSpeed);
            velZ = MathHelper.clamp(velZ, -maxSpeed, maxSpeed);

            x += velX;
            y += velY;
            z += velZ;

            trail.add(0, new TrailPoint(x, y, z));
            while (trail.size() > maxTrailLength) {
                trail.remove(trail.size() - 1);
            }
        }

        private boolean isDead(double px, double py, double pz) {
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            return dx * dx + dy * dy + dz * dz > 80 * 80;
        }

        private double getInterpolatedX(float tickDelta) {
            return MathHelper.lerp(tickDelta, prevX, x);
        }

        private double getInterpolatedY(float tickDelta) {
            return MathHelper.lerp(tickDelta, prevY, y);
        }

        private double getInterpolatedZ(float tickDelta) {
            return MathHelper.lerp(tickDelta, prevZ, z);
        }

        private int getPulseAlpha() {
            long age = System.currentTimeMillis() - spawnTime;
            double pulse = 0.8 + 0.2 * Math.sin(age / 200.0);
            return (int) (pulse * 255.0);
        }

        private float getLifeAlpha() {
            long age = System.currentTimeMillis() - spawnTime;
            long fadeInDuration = 1000L;
            if (age < fadeInDuration) {
                return age / (float) fadeInDuration;
            }
            return 1.0f;
        }
    }

    private static final class FireFlyV2 {
        private final int index;
        private final StopWatch timer = new StopWatch();
        private final Animation alpha = new Animation(Easing.QUAD_OUT, FIREFLY_FADE_MS);
        private final int color;
        private final List<Vec3d> trail = new ArrayList<>();
        private Vec3d position;
        private Vec3d velocity;

        private FireFlyV2(Vec3d position, Vec3d velocity, int index, int color) {
            this.position = position;
            this.velocity = velocity;
            this.index = index;
            this.color = color;
            this.alpha.setValue(0.0f);
            this.timer.reset();
            this.trail.add(position);
        }

        private void update(double maxSpeedSetting, int maxTrailLength, float frameDeltaTicks) {
            double randomness = 0.01;
            velocity = velocity.add(
                    (Math.random() - 0.5) * randomness * frameDeltaTicks,
                    (Math.random() - 0.5) * randomness * frameDeltaTicks,
                    (Math.random() - 0.5) * randomness * frameDeltaTicks
            );

            double maxSpeed = maxSpeedSetting * 1.5;
            velocity = new Vec3d(
                    MathHelper.clamp(velocity.x, -maxSpeed, maxSpeed),
                    MathHelper.clamp(velocity.y, -maxSpeed, maxSpeed),
                    MathHelper.clamp(velocity.z, -maxSpeed, maxSpeed)
            );

            position = position.add(velocity.multiply(frameDeltaTicks));
            if (trail.isEmpty() || trail.get(trail.size() - 1).distanceTo(position) > 0.02) {
                trail.add(position);
            }

            while (trail.size() > maxTrailLength) {
                trail.remove(0);
            }
        }

        private boolean isExpired(Vec3d playerPos) {
            return timer.isReached(FIREFLY_LIFETIME_MS) || position.distanceTo(playerPos) > 60.0;
        }

        private int updateAlpha() {
            long fadeOutStart = FIREFLY_LIFETIME_MS - alpha.getDuration();
            if (!timer.isReached(alpha.getDuration())) {
                alpha.run(255.0f);
            } else if (timer.isReached(fadeOutStart)) {
                alpha.run(0.0f);
            } else {
                alpha.run(255.0f);
            }
            return MathHelper.clamp((int) alpha.getValue(), 0, 255);
        }

        private int getPulseAlpha() {
            double pulse = (Math.sin(timer.getTime() / 300.0) + 1.0) / 2.0;
            return (int) (pulse * 255.0);
        }
    }

    private final List<ClassicFireFly> particles = new ArrayList<>();
    private final List<FireFlyV2> particlesV2 = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrameTimeNs;

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
        particlesV2.clear();
        lastFrameTimeNs = 0L;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
        particlesV2.clear();
        lastFrameTimeNs = 0L;
    }

    @Subscribe
    private void onPlayerTick(EventTick e) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Vec3d playerPos = mc.player.getPos();
        int targetCount = count.getIntValue();

        if (mode.is("Светлячки")) {
            particles.clear();
            return;
        }

        particlesV2.clear();
        float speedMult = CLASSIC_SPEED;
        float maxSpeed = speedMult * 1.5f;
        particles.forEach(particle -> particle.update(speedMult, maxSpeed, playerPos));
        particles.removeIf(particle -> particle.isDead(playerPos.x, playerPos.y, playerPos.z));

        while (particles.size() > targetCount) {
            particles.remove(particles.size() - 1);
        }
        while (particles.size() < targetCount) {
            spawnClassicParticle(playerPos);
        }
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mode.is("Светлячки")) {
            updateFireFlyMode();
        }

        onRender3D(event.getMatrixStack(), mc.gameRenderer.getCamera(), event.getTickDelta());
    }

    private void spawnClassicParticle(Vec3d playerPos) {
        double distance = random.nextDouble() * (CLASSIC_SPAWN_RADIUS - 5.0) + 5.0;
        double yawRad = Math.toRadians(random.nextDouble() * 360.0);
        double xOffset = -Math.sin(yawRad) * distance;
        double zOffset = Math.cos(yawRad) * distance;
        double yOffset = (random.nextDouble() - 0.3) * 8.0 + 1.0;

        double velocityYaw = Math.toRadians(random.nextDouble() * 360.0);
        double velocityPitch = Math.toRadians((random.nextDouble() - 0.5) * 60.0);
        double velX = -Math.sin(velocityYaw) * Math.cos(velocityPitch) * CLASSIC_SPEED;
        double velY = Math.sin(velocityPitch) * CLASSIC_SPEED * 0.5;
        double velZ = Math.cos(velocityYaw) * Math.cos(velocityPitch) * CLASSIC_SPEED;

        int randomColor = FIREFLY_PALETTE[random.nextInt(FIREFLY_PALETTE.length)];
        particles.add(new ClassicFireFly(
                playerPos.x + xOffset,
                playerPos.y + yOffset,
                playerPos.z + zOffset,
                velX,
                velY,
                velZ,
                randomColor,
                CLASSIC_TRAIL_LENGTH
        ));
    }

    private void spawnFireFly(Vec3d playerPos) {
        double distance = random(5.0, ffRadius.getValue());
        double yawRad = Math.toRadians(random(0.0, 360.0));
        double xOffset = -Math.sin(yawRad) * distance;
        double zOffset = Math.cos(yawRad) * distance;
        double yOffset = random(-5.0, 10.0);

        double velocitySpeed = ffSpeed.getValue();
        double velocityYaw = Math.toRadians(random(0.0, 360.0));
        double velocityPitch = Math.toRadians(random(-30.0, 30.0));

        Vec3d initialVelocity = new Vec3d(
                -Math.sin(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed,
                Math.sin(velocityPitch) * velocitySpeed * 0.5,
                Math.cos(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed
        );

        int color = ffRandomColor.getValue() ? randomFireFlyColor() : getPaletteColor(particlesV2.size());
        particlesV2.add(new FireFlyV2(playerPos.add(xOffset, yOffset, zOffset), initialVelocity, particlesV2.size(), color));
    }

    private void updateFireFlyMode() {
        Vec3d playerPos = mc.player.getPos();
        int targetCount = count.getIntValue();
        int maxTrail = Math.max(1, ffTrail.getIntValue());
        float frameDeltaTicks = getFrameDeltaTicks();

        particles.clear();
        particlesV2.forEach(particle -> particle.update(ffSpeed.getValue(), maxTrail, frameDeltaTicks));
        particlesV2.removeIf(particle -> particle.isExpired(playerPos));

        while (particlesV2.size() > targetCount) {
            particlesV2.remove(particlesV2.size() - 1);
        }
        while (particlesV2.size() < targetCount) {
            spawnFireFly(playerPos);
        }
    }

    private float getFrameDeltaTicks() {
        long now = System.nanoTime();
        if (lastFrameTimeNs == 0L) {
            lastFrameTimeNs = now;
            return 1.0f;
        }

        float delta = (now - lastFrameTimeNs) / 50_000_000.0f;
        lastFrameTimeNs = now;
        return MathHelper.clamp(delta, 0.05f, 3.0f);
    }

    private void onRender3D(MatrixStack stack, Camera camera, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Vec3d cameraPos = camera.getPos();
        int themeColorValue = ColorProvider.getThemeColor();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(770, 1, 1, 0);
        RenderSystem.enableDepthTest();

        BufferBuilder buffer = null;
        if (mode.is("Светлячки")) {
            if (!particlesV2.isEmpty()) {
                buffer = renderFireFlyMode(stack, camera, cameraPos, buffer);
            }
        } else if (!particles.isEmpty()) {
            buffer = renderClassicMode(stack, camera, tickDelta, cameraPos, themeColorValue, buffer);
        }

        if (buffer != null) {
            BuiltBuffer builtBuffer = buffer.end();
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
    }

    private BufferBuilder renderClassicMode(MatrixStack stack, Camera camera, float tickDelta, Vec3d cameraPos, int themeColorValue, BufferBuilder buffer) {
        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f globalMatrix = stack.peek().getPositionMatrix();

        for (ClassicFireFly particle : particles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f || particle.trail.size() < 2) {
                continue;
            }

            int renderColor = themeColor.getValue() ? themeColorValue : particle.baseRandomColor;
            double px = particle.getInterpolatedX(tickDelta);
            double py = particle.getInterpolatedY(tickDelta);
            double pz = particle.getInterpolatedZ(tickDelta);

            List<Vec3d> points = new ArrayList<>();
            points.add(new Vec3d(px, py, pz));
            for (TrailPoint point : particle.trail) {
                points.add(new Vec3d(point.x, point.y, point.z));
            }

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3d current = points.get(i);
                Vec3d next = points.get(i + 1);
                float t1 = (float) i / (points.size() - 1);
                float t2 = (float) (i + 1) / (points.size() - 1);
                float w1 = 0.12f * (1.0f - t1);
                float w2 = 0.12f * (1.0f - t2);
                float alpha1 = (1.0f - t1) * (1.0f - t1) * lifeAlpha * 0.6f;
                float alpha2 = (1.0f - t2) * (1.0f - t2) * lifeAlpha * 0.6f;
                if (alpha1 <= 0.01f && alpha2 <= 0.01f) {
                    continue;
                }

                Vec3d dir = current.subtract(next);
                if (dir.lengthSquared() < 0.0001) {
                    continue;
                }

                Vec3d camToCur = cameraPos.subtract(current);
                Vec3d cross1 = dir.crossProduct(camToCur);
                if (cross1.lengthSquared() < 0.0001) {
                    continue;
                }

                Vec3d right1 = cross1.normalize().multiply(w1);
                Vec3d camToNext = cameraPos.subtract(next);
                Vec3d cross2 = dir.crossProduct(camToNext);
                if (cross2.lengthSquared() < 0.0001) {
                    continue;
                }

                Vec3d right2 = cross2.normalize().multiply(w2);
                int c1 = ColorProvider.setAlpha(renderColor, (int) (alpha1 * 255.0f));
                int c2 = ColorProvider.setAlpha(renderColor, (int) (alpha2 * 255.0f));
                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                }

                buffer.vertex(globalMatrix, (float) (current.x + right1.x), (float) (current.y + right1.y), (float) (current.z + right1.z)).texture(0, t1).color(c1);
                buffer.vertex(globalMatrix, (float) (current.x - right1.x), (float) (current.y - right1.y), (float) (current.z - right1.z)).texture(1, t1).color(c1);
                buffer.vertex(globalMatrix, (float) (next.x - right2.x), (float) (next.y - right2.y), (float) (next.z - right2.z)).texture(1, t2).color(c2);
                buffer.vertex(globalMatrix, (float) (next.x + right2.x), (float) (next.y + right2.y), (float) (next.z + right2.z)).texture(0, t2).color(c2);
            }
        }

        stack.pop();

        for (ClassicFireFly particle : particles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f) {
                continue;
            }

            float pulseFloat = particle.getPulseAlpha() / 255.0f;
            float finalAlpha = pulseFloat * lifeAlpha;
            if (finalAlpha <= 0.01f) {
                continue;
            }

            int renderColor = themeColor.getValue() ? themeColorValue : particle.baseRandomColor;
            double px = particle.getInterpolatedX(tickDelta);
            double py = particle.getInterpolatedY(tickDelta);
            double pz = particle.getInterpolatedZ(tickDelta);

            stack.push();
            stack.translate(px - cameraPos.x, py - cameraPos.y, pz - cameraPos.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f localMatrix = stack.peek().getPositionMatrix();

            if (buffer == null) {
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            drawQuad(buffer, localMatrix, 0.35f, renderColor, finalAlpha * 0.6f);
            drawQuad(buffer, localMatrix, 0.22f, renderColor, finalAlpha);
            drawQuad(buffer, localMatrix, 0.10f, 0xFFFFFFFF, finalAlpha);
            stack.pop();
        }

        return buffer;
    }

    private BufferBuilder renderFireFlyMode(MatrixStack stack, Camera camera, Vec3d cameraPos, BufferBuilder buffer) {
        for (FireFlyV2 particle : particlesV2) {
            int baseAlpha = particle.updateAlpha();
            if (baseAlpha <= 3) {
                continue;
            }

            int color = getFireFlyRenderColor(particle);
            buffer = renderFireFlyTrail(stack, camera, cameraPos, buffer, particle, color, baseAlpha);
            buffer = renderFireFlyBody(stack, camera, cameraPos, buffer, particle, color, baseAlpha);
        }
        return buffer;
    }

    private BufferBuilder renderFireFlyTrail(MatrixStack stack, Camera camera, Vec3d cameraPos, BufferBuilder buffer, FireFlyV2 particle, int color, int baseAlpha) {
        List<Vec3d> trail = particle.trail;
        if (trail.size() < 2) {
            return buffer;
        }

        float baseSize = ffSize.getFloatValue();
        for (int i = 0; i < trail.size(); i++) {
            Vec3d pos = trail.get(i);
            float fade = (float) i / (float) trail.size();
            if (fade <= 0.01f) {
                continue;
            }

            float quadSize = baseSize * 0.68f * fade;
            int trailAlpha = (int) (baseAlpha * fade * 0.8f);
            if (trailAlpha <= 1) {
                continue;
            }

            int trailColor = ColorProvider.setAlpha(color, trailAlpha);
            stack.push();
            stack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            if (buffer == null) {
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }
            drawQuad(buffer, stack.peek().getPositionMatrix(), quadSize, trailColor, 1.0f);
            stack.pop();

            if (i % 3 == 0 && fade > 0.3f) {
                int miniParticleCount = 2 + random.nextInt(3);
                for (int j = 0; j < miniParticleCount; j++) {
                    double offsetX = (random.nextDouble() - 0.5) * 0.3;
                    double offsetY = (random.nextDouble() - 0.5) * 0.3;
                    double offsetZ = (random.nextDouble() - 0.5) * 0.3;
                    float miniSize = baseSize * 0.18f + random.nextFloat() * 0.03f;
                    int miniAlpha = (int) (trailAlpha * 0.6f);
                    if (miniAlpha <= 1) {
                        continue;
                    }

                    int miniColor = ColorProvider.setAlpha(color, miniAlpha);
                    stack.push();
                    stack.translate(
                            pos.x + offsetX - cameraPos.x,
                            pos.y + offsetY - cameraPos.y,
                            pos.z + offsetZ - cameraPos.z
                    );
                    stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                    if (buffer == null) {
                        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    }
                    drawQuad(buffer, stack.peek().getPositionMatrix(), miniSize, miniColor, 1.0f);
                    stack.pop();
                }
            }
        }

        return buffer;
    }

    private BufferBuilder renderFireFlyBody(MatrixStack stack, Camera camera, Vec3d cameraPos, BufferBuilder buffer, FireFlyV2 particle, int color, int baseAlpha) {
        int finalAlpha = Math.min(baseAlpha, particle.getPulseAlpha());
        if (finalAlpha <= 3) {
            return buffer;
        }

        stack.push();
        stack.translate(
                particle.position.x - cameraPos.x,
                particle.position.y - cameraPos.y,
                particle.position.z - cameraPos.z
        );
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        if (buffer == null) {
            buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        }

        Matrix4f matrix = stack.peek().getPositionMatrix();
        float baseSize = ffSize.getFloatValue();
        drawQuad(buffer, matrix, baseSize * 1.6f, ColorProvider.setAlpha(color, (int) (finalAlpha * 0.6f)), 1.0f);
        drawQuad(buffer, matrix, baseSize, ColorProvider.setAlpha(color, finalAlpha), 1.0f);
        drawQuad(buffer, matrix, baseSize * 0.45f, ColorProvider.setAlpha(0xFFFFFFFF, finalAlpha), 1.0f);
        stack.pop();
        return buffer;
    }

    private int getFireFlyRenderColor(FireFlyV2 particle) {
        if (ffRandomColor.getValue()) {
            return particle.color;
        }
        if (themeColor.getValue()) {
            return getThemeMixedColor(particle.index);
        }
        return particle.color;
    }

    private int getThemeMixedColor(int index) {
        int first = ColorProvider.getThemeColor();
        int second = ColorProvider.getThemeColorTwo();
        float mix = (float) ((Math.sin((System.currentTimeMillis() + index * 40L) / 450.0) + 1.0) * 0.5);
        return ColorProvider.interpolate(first, second, mix);
    }

    private int getPaletteColor(int index) {
        return FIREFLY_PALETTE[Math.floorMod(index, FIREFLY_PALETTE.length)];
    }

    private int randomFireFlyColor() {
        return ColorProvider.rgba(random.nextInt(256), random.nextInt(256), random.nextInt(256), 255);
    }

    private double random(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, int color, float alphaMod) {
        if (alphaMod <= 0.01f) {
            return;
        }

        int finalColor = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * alphaMod));
        buffer.vertex(matrix, -size, -size, 0.0f).texture(0.0f, 0.0f).color(finalColor);
        buffer.vertex(matrix, -size, size, 0.0f).texture(0.0f, 1.0f).color(finalColor);
        buffer.vertex(matrix, size, size, 0.0f).texture(1.0f, 1.0f).color(finalColor);
        buffer.vertex(matrix, size, -size, 0.0f).texture(1.0f, 0.0f).color(finalColor);
    }
}
