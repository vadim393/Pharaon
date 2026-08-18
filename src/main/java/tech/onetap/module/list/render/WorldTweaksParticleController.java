package tech.onetap.module.list.render;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class WorldTweaksParticleController {
    private static final Identifier GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");
    private static final float CLASSIC_SPEED = 0.35f;
    private static final float CLASSIC_SPAWN_RADIUS = 35.0f;
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

    private final WorldTweaks owner;
    private final List<ClassicFireFly> classicParticles = new ArrayList<>();
    private final List<FireFlyV2> fireFlyParticles = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrameTimeNs;

    WorldTweaksParticleController(WorldTweaks owner) {
        this.owner = owner;
    }

    void reset() {
        classicParticles.clear();
        fireFlyParticles.clear();
        lastFrameTimeNs = 0L;
    }

    void onTick() {
        if (!shouldRun()) {
            reset();
            return;
        }

        Vec3d playerPos = owner.mc.player.getPos();
        int targetCount = owner.worldParticleCount.getIntValue();

        if (owner.isWorldParticleMode(WorldTweaks.WORLD_PARTICLE_MODE_FIREFLY)) {
            classicParticles.clear();
            return;
        }

        fireFlyParticles.clear();
        float speedMult = CLASSIC_SPEED;
        float maxSpeed = speedMult * 1.5f;
        classicParticles.forEach(particle -> particle.update(speedMult, maxSpeed, playerPos, owner.mc.world));
        classicParticles.removeIf(particle -> particle.isDead(playerPos.x, playerPos.y, playerPos.z));

        while (classicParticles.size() > targetCount) {
            classicParticles.remove(classicParticles.size() - 1);
        }
        while (classicParticles.size() < targetCount) {
            if (!spawnClassicParticle(playerPos)) {
                break;
            }
        }
    }

    void onWorldRender(EventWorldRender event) {
        if (!shouldRun()) {
            reset();
            return;
        }

        if (owner.isWorldParticleMode(WorldTweaks.WORLD_PARTICLE_MODE_FIREFLY)) {
            updateFireFlyMode();
        }

        renderParticles(event.getMatrixStack(), owner.mc.gameRenderer.getCamera(), event.getTickDelta());
    }

    private boolean shouldRun() {
        return owner.isEnabled()
                && owner.isWorldParticlesEnabled()
                && owner.mc.player != null
                && owner.mc.world != null;
    }

    private boolean spawnClassicParticle(Vec3d playerPos) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double distance = random.nextDouble() * (CLASSIC_SPAWN_RADIUS - 5.0) + 5.0;
            double yawRad = Math.toRadians(random.nextDouble() * 360.0);
            double xOffset = -Math.sin(yawRad) * distance;
            double zOffset = Math.cos(yawRad) * distance;
            double yOffset = (random.nextDouble() - 0.3) * 8.0 + 1.0;
            Vec3d spawnPos = playerPos.add(xOffset, yOffset, zOffset);

            if (!isParticleSpaceFree(owner.mc.world, spawnPos)) {
                continue;
            }

            double velocityYaw = Math.toRadians(random.nextDouble() * 360.0);
            double velocityPitch = Math.toRadians((random.nextDouble() - 0.5) * 60.0);
            double velX = -Math.sin(velocityYaw) * Math.cos(velocityPitch) * CLASSIC_SPEED;
            double velY = Math.sin(velocityPitch) * CLASSIC_SPEED * 0.5;
            double velZ = Math.cos(velocityYaw) * Math.cos(velocityPitch) * CLASSIC_SPEED;

            int randomColor = FIREFLY_PALETTE[random.nextInt(FIREFLY_PALETTE.length)];
            classicParticles.add(new ClassicFireFly(
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    velX,
                    velY,
                    velZ,
                    randomColor,
                    CLASSIC_TRAIL_LENGTH
            ));
            return true;
        }

        return false;
    }

    private boolean spawnFireFly(Vec3d playerPos) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double distance = random(5.0, owner.worldParticleRadius.getValue());
            double yawRad = Math.toRadians(random(0.0, 360.0));
            double xOffset = -Math.sin(yawRad) * distance;
            double zOffset = Math.cos(yawRad) * distance;
            double yOffset = random(-5.0, 10.0);
            Vec3d spawnPos = playerPos.add(xOffset, yOffset, zOffset);

            if (!isParticleSpaceFree(owner.mc.world, spawnPos)) {
                continue;
            }

            double velocitySpeed = owner.worldParticleSpeed.getValue();
            double velocityYaw = Math.toRadians(random(0.0, 360.0));
            double velocityPitch = Math.toRadians(random(-30.0, 30.0));

            Vec3d initialVelocity = new Vec3d(
                    -Math.sin(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed,
                    Math.sin(velocityPitch) * velocitySpeed * 0.5,
                    Math.cos(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed
            );

            int color = owner.worldParticleRandomColor.getValue() ? randomFireFlyColor() : getPaletteColor(fireFlyParticles.size());
            fireFlyParticles.add(new FireFlyV2(spawnPos, initialVelocity, fireFlyParticles.size(), color));
            return true;
        }

        return false;
    }

    private void updateFireFlyMode() {
        Vec3d playerPos = owner.mc.player.getPos();
        int targetCount = owner.worldParticleCount.getIntValue();
        int maxTrail = Math.max(1, owner.worldParticleTrail.getIntValue());
        float frameDeltaTicks = getFrameDeltaTicks();

        classicParticles.clear();
        fireFlyParticles.forEach(particle -> particle.update(owner.worldParticleSpeed.getValue(), maxTrail, frameDeltaTicks, owner.mc.world));
        fireFlyParticles.removeIf(particle -> particle.isExpired(playerPos));

        while (fireFlyParticles.size() > targetCount) {
            fireFlyParticles.remove(fireFlyParticles.size() - 1);
        }
        while (fireFlyParticles.size() < targetCount) {
            if (!spawnFireFly(playerPos)) {
                break;
            }
        }
    }

    private static boolean isParticleSpaceFree(World world, Vec3d pos) {
        return isParticleSpaceFree(world, pos.x, pos.y, pos.z);
    }

    private static boolean isParticleSpaceFree(World world, double x, double y, double z) {
        if (world == null || y <= world.getBottomY() || y >= world.getTopYInclusive()) {
            return false;
        }

        return world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir();
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

    private void renderParticles(MatrixStack stack, Camera camera, float tickDelta) {
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
        if (owner.isWorldParticleMode(WorldTweaks.WORLD_PARTICLE_MODE_FIREFLY)) {
            if (!fireFlyParticles.isEmpty()) {
                buffer = renderFireFlyMode(stack, camera, cameraPos, buffer);
            }
        } else if (!classicParticles.isEmpty()) {
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

        for (ClassicFireFly particle : classicParticles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f || particle.trail.size() < 2) {
                continue;
            }

            int renderColor = owner.worldParticleThemeColor.getValue() ? themeColorValue : particle.baseRandomColor;
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

        for (ClassicFireFly particle : classicParticles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f) {
                continue;
            }

            float pulseFloat = particle.getPulseAlpha() / 255.0f;
            float finalAlpha = pulseFloat * lifeAlpha;
            if (finalAlpha <= 0.01f) {
                continue;
            }

            int renderColor = owner.worldParticleThemeColor.getValue() ? themeColorValue : particle.baseRandomColor;
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
        for (FireFlyV2 particle : fireFlyParticles) {
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

        float baseSize = owner.worldParticleSize.getFloatValue();
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
                    stack.translate(pos.x + offsetX - cameraPos.x, pos.y + offsetY - cameraPos.y, pos.z + offsetZ - cameraPos.z);
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
        stack.translate(particle.position.x - cameraPos.x, particle.position.y - cameraPos.y, particle.position.z - cameraPos.z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        if (buffer == null) {
            buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        }

        Matrix4f matrix = stack.peek().getPositionMatrix();
        float baseSize = owner.worldParticleSize.getFloatValue();
        drawQuad(buffer, matrix, baseSize * 1.6f, ColorProvider.setAlpha(color, (int) (finalAlpha * 0.6f)), 1.0f);
        drawQuad(buffer, matrix, baseSize, ColorProvider.setAlpha(color, finalAlpha), 1.0f);
        drawQuad(buffer, matrix, baseSize * 0.45f, ColorProvider.setAlpha(0xFFFFFFFF, finalAlpha), 1.0f);
        stack.pop();
        return buffer;
    }

    private int getFireFlyRenderColor(FireFlyV2 particle) {
        if (owner.worldParticleRandomColor.getValue()) {
            return particle.color;
        }
        if (owner.worldParticleThemeColor.getValue()) {
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

        private void update(float speedMult, float maxSpeed, Vec3d playerPos, World world) {
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

            double nextX = x + velX;
            if (isParticleSpaceFree(world, nextX, y, z)) {
                x = nextX;
            } else {
                velX *= -0.55;
                targetVelX *= -0.35;
            }

            double nextY = y + velY;
            if (isParticleSpaceFree(world, x, nextY, z)) {
                y = nextY;
            } else {
                velY *= -0.4;
                targetVelY *= -0.25;
            }

            double nextZ = z + velZ;
            if (isParticleSpaceFree(world, x, y, nextZ)) {
                z = nextZ;
            } else {
                velZ *= -0.55;
                targetVelZ *= -0.35;
            }

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

        private void update(double maxSpeedSetting, int maxTrailLength, float frameDeltaTicks, World world) {
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

            Vec3d frameVelocity = velocity.multiply(frameDeltaTicks);
            double nextX = position.x + frameVelocity.x;
            double nextY = position.y + frameVelocity.y;
            double nextZ = position.z + frameVelocity.z;

            if (isParticleSpaceFree(world, nextX, position.y, position.z)) {
                position = new Vec3d(nextX, position.y, position.z);
            } else {
                velocity = new Vec3d(-velocity.x * 0.55, velocity.y, velocity.z);
            }

            if (isParticleSpaceFree(world, position.x, nextY, position.z)) {
                position = new Vec3d(position.x, nextY, position.z);
            } else {
                velocity = new Vec3d(velocity.x, -velocity.y * 0.4, velocity.z);
            }

            if (isParticleSpaceFree(world, position.x, position.y, nextZ)) {
                position = new Vec3d(position.x, position.y, nextZ);
            } else {
                velocity = new Vec3d(velocity.x, velocity.y, -velocity.z * 0.55);
            }

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
}
