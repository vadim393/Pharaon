package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventPopTotem;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "Particles", moduleDesc = "Custom particles", moduleCategory = ModuleCategory.RENDER)
public class Particles extends Module {
    private static final Identifier GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");
    private static final Identifier STAR_TEXTURE = Identifier.of("mre", "images/star.png");
    private static final Identifier DOLLAR_TEXTURE = Identifier.of("mre", "images/dollar.png");
    private static final Identifier HEART_TEXTURE = Identifier.of("mre", "images/heart.png");
    private static final Identifier SNOWFLAKE_TEXTURE = Identifier.of("mre", "images/snowflake.png");

    private static final float PARTICLE_SIZE = 0.25f;
    private static final int PARTICLE_LIFE = 50;
    private static final float PARTICLE_SPEED = 0.4f;
    private static final float WALK_PARTICLE_MULTIPLIER = 0.25f;
    private static final int WORLD_PARTICLE_LIFE = 100;
    private static final double WORLD_PARTICLE_SPAWN_RADIUS = 50.0;
    private static final double WORLD_PARTICLE_DESPAWN_RADIUS = 70.0;
    private static final float WORLD_PARTICLE_START_FADE_DISTANCE = 60.0f;
    private static final float WORLD_PARTICLE_END_FADE_DISTANCE = 70.0f;

    private final ModeSetting texture = new ModeSetting("Текстура", "Glow", "Glow", "Star", "Dollar", "Heart", "Snowflake");
    private final ModeSetting physics = new ModeSetting("Физика", "Fly", "Fly", "Drop");
    private final BooleanSetting worldMode = new BooleanSetting("В мире", false);
    private final SliderSetting worldLimit = new SliderSetting("Лимит в мире", 250, 50, 500, 10).setVisible(worldMode::getValue);
    private final SliderSetting worldSize = new SliderSetting("Размер в мире", 2.0f, 1.0f, 4.0f, 0.1f).setVisible(worldMode::getValue);
    private final BooleanSetting walkMode = new BooleanSetting("При ходьбе", false);
    private final BooleanSetting hitMode = new BooleanSetting("При ударе", true);
    private final BooleanSetting pearlMode = new BooleanSetting("Эндер перл", false);
    private final BooleanSetting totemMode = new BooleanSetting("При сносе тотема", false);
    private final SliderSetting count = new SliderSetting("Количество", 20, 1, 50, 1);

    private static final int[] TOTEM_COLORS = {
            0xFFFFE45A,
            0xFFFFF247,
            0xFFB4FF28,
            0xFF74FF31
    };

    private final List<ParticleData> particles = new ArrayList<>();
    private double lastWalkX;
    private double lastWalkY;
    private double lastWalkZ;

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
        if (mc.player != null) {
            lastWalkX = mc.player.getX();
            lastWalkY = mc.player.getY();
            lastWalkZ = mc.player.getZ();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.world == null) {
            particles.clear();
            return;
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            if (particles.get(i).update()) {
                particles.remove(i);
            }
        }

        if (walkMode.getValue()) {
            spawnWalkParticles();
        }
        if (pearlMode.getValue()) {
            spawnPearlParticles();
        }
        if (worldMode.getValue()) {
            maintainWorldParticles();
        }
    }

    @Subscribe
    private void onAttack(EventAttack event) {
        if (!hitMode.getValue()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        spawnBurst(target.getPos().add(0, target.getHeight() * 0.5, 0));
    }

    @Subscribe
    private void onPop(EventPopTotem event) {
        if (!totemMode.getValue()) {
            return;
        }

        PlayerEntity target = event.getPlayer();
        if (target == null) {
            return;
        }

        spawnTotemBurst(target);
    }

    @Subscribe
    private void onRender(EventWorldRender event) {
        if (particles.isEmpty() || mc.player == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        float tickDelta = event.getTickDelta();
        MatrixStack matrixStack = event.getMatrixStack();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        for (ParticleData particle : particles) {
            particle.render(matrixStack, camera, cameraPos, tickDelta);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
    }

    private void spawnWalkParticles() {
        PlayerEntity player = mc.player;
        double moved = player.squaredDistanceTo(lastWalkX, lastWalkY, lastWalkZ);
        lastWalkX = player.getX();
        lastWalkY = player.getY();
        lastWalkZ = player.getZ();

        if (moved < 0.0009 || player.isSneaking()) {
            return;
        }

        int amount = Math.max(1, Math.round(count.getFloatValue() * WALK_PARTICLE_MULTIPLIER));
        Vec3d base = player.getPos();
        boolean smallPlayer = WorldTweaks.shouldRenderSmallPlayer(player);
        double bodyHeight = player.getHeight();
        double bodyWidth = player.getWidth();
        if (smallPlayer) {
            bodyHeight = Math.max(0.35, bodyHeight * 0.5);
            bodyWidth *= 0.5;
        }
        int themeColor = ColorProvider.getThemeColor();

        for (int i = 0; i < amount; i++) {
            double progress = amount == 1 ? 0.5 : i / (double) (amount - 1);
            double y = base.y + (bodyHeight * progress);
            double side = (ThreadLocalRandom.current().nextDouble() - 0.5) * bodyWidth * 1.2;
            double front = (ThreadLocalRandom.current().nextDouble() - 0.5) * bodyWidth * 0.9;
            float vx = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.15f;
            float vy = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.08f;
            float vz = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.15f;
            particles.add(new ParticleData(base.x + side, y, base.z + front, vx, vy, vz, PARTICLE_SIZE, PARTICLE_LIFE, physics.is("Drop"), true, resolveTexture(), themeColor));
        }
    }

    private void spawnBurst(Vec3d center) {
        int amount = count.getIntValue();
        int themeColor = ColorProvider.getThemeColor();

        for (int i = 0; i < amount; i++) {
            float vx = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED);
            float vy = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED);
            float vz = randomRange(-PARTICLE_SPEED, PARTICLE_SPEED);
            particles.add(new ParticleData(center.x, center.y, center.z, vx, vy, vz, PARTICLE_SIZE, PARTICLE_LIFE, physics.is("Drop"), true, resolveTexture(), themeColor));
        }
    }

    private void spawnPearlParticles() {
        int amount = Math.max(1, Math.round(count.getFloatValue() * 0.15f));
        int themeColor = ColorProvider.getThemeColor();

        for (EnderPearlEntity pearl : mc.world.getEntitiesByClass(EnderPearlEntity.class, mc.player.getBoundingBox().expand(96.0), entity -> entity.isAlive())) {
            if (pearl.squaredDistanceTo(pearl.prevX, pearl.prevY, pearl.prevZ) < 0.0001) {
                continue;
            }

            Vec3d velocity = pearl.getVelocity();
            double width = Math.max(0.28, pearl.getWidth() * 0.75);
            double height = Math.max(0.28, pearl.getHeight() * 0.75);

            for (int i = 0; i < amount; i++) {
                double spawnX = pearl.getX() + randomRange(-width, width);
                double spawnY = pearl.getY() + pearl.getHeight() * 0.5 + randomRange(-height, height);
                double spawnZ = pearl.getZ() + randomRange(-width, width);
                float vx = (float) (velocity.x * 0.18 + randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.08f);
                float vy = (float) (velocity.y * 0.18 + randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.05f);
                float vz = (float) (velocity.z * 0.18 + randomRange(-PARTICLE_SPEED, PARTICLE_SPEED) * 0.08f);
                float size = 0.25F * randomRange(0.7, 1.0);
                int life = ThreadLocalRandom.current().nextInt(26, 40);
                particles.add(new ParticleData(spawnX, spawnY, spawnZ, vx, vy, vz, size, life, physics.is("Drop"), true, resolveTexture(), themeColor));
            }
        }
    }

    private void spawnTotemBurst(PlayerEntity target) {
        int amount = Math.max(4, Math.round(count.getFloatValue() * 0.55f));
        boolean smallPlayer = WorldTweaks.shouldRenderSmallPlayer(target);
        double bodyHeight = target.getHeight();
        double bodyWidth = target.getWidth();
        if (smallPlayer) {
            bodyHeight = Math.max(0.35, bodyHeight * 0.5);
            bodyWidth *= 0.5;
        }

        for (int i = 0; i < amount; i++) {
            double spawnX = target.getX() + randomRange(-bodyWidth * 0.45, bodyWidth * 0.45);
            double spawnY = target.getY() + 0.1 + ThreadLocalRandom.current().nextDouble(bodyHeight * 0.9);
            double spawnZ = target.getZ() + randomRange(-bodyWidth * 0.45, bodyWidth * 0.45);

            Vec3d direction = new Vec3d(
                    randomRange(-1.0, 1.0),
                    randomRange(-0.45, 0.95),
                    randomRange(-1.0, 1.0)
            );
            if (direction.lengthSquared() < 0.01) {
                direction = new Vec3d(0.0, 0.35, 0.0);
            }

            direction = direction.normalize();
            float speed = randomRange(0.12, 0.28);
            float vx = (float) (direction.x * speed + randomRange(-0.03, 0.03));
            float vy = (float) (direction.y * speed + randomRange(-0.015, 0.05));
            float vz = (float) (direction.z * speed + randomRange(-0.03, 0.03));
            int life = ThreadLocalRandom.current().nextInt(24, 36);
            float size = PARTICLE_SIZE * randomRange(0.85, 1.25);
            particles.add(new ParticleData(
                    spawnX,
                    spawnY,
                    spawnZ,
                    vx,
                    vy,
                    vz,
                    size,
                    life,
                    physics.is("Drop"),
                    true,
                    resolveTexture(),
                    randomTotemColor()
            ));
        }
    }

    private void maintainWorldParticles() {
        int worldTarget = worldLimit.getIntValue();

        int currentWorld = 0;
        for (ParticleData particle : particles) {
            if (particle.worldParticle) {
                currentWorld++;
            }
        }

        int attempts = 0;
        while (currentWorld < worldTarget && attempts < 8) {
            if (!spawnWorldParticle()) {
                attempts++;
                continue;
            }
            currentWorld++;
            attempts++;
        }
    }

    private boolean spawnWorldParticle() {
        Vec3d playerPos = mc.player.getPos();

        for (int attempt = 0; attempt < 8; attempt++) {
            double offsetX;
            double offsetY;
            double offsetZ;
            do {
                offsetX = randomRange(-WORLD_PARTICLE_SPAWN_RADIUS, WORLD_PARTICLE_SPAWN_RADIUS);
                offsetY = randomRange(-WORLD_PARTICLE_SPAWN_RADIUS, WORLD_PARTICLE_SPAWN_RADIUS);
                offsetZ = randomRange(-WORLD_PARTICLE_SPAWN_RADIUS, WORLD_PARTICLE_SPAWN_RADIUS);
            } while (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ > WORLD_PARTICLE_SPAWN_RADIUS * WORLD_PARTICLE_SPAWN_RADIUS);

            double x = playerPos.x + offsetX;
            double y = playerPos.y + offsetY;
            double z = playerPos.z + offsetZ;

            BlockPos pos = BlockPos.ofFloored(x, y, z);
            BlockState state = mc.world.getBlockState(pos);
            if (!state.getCollisionShape(mc.world, pos).isEmpty()) {
                continue;
            }

            float vx = randomRange(-0.02, 0.02);
            float vy = randomRange(-0.01, 0.01);
            float vz = randomRange(-0.02, 0.02);
            particles.add(new ParticleData(x, y, z, vx, vy, vz, PARTICLE_SIZE, WORLD_PARTICLE_LIFE, false, false, resolveTexture(), ColorProvider.getThemeColor(), true));
            return true;
        }

        return false;
    }

    private Identifier resolveTexture() {
        if (texture.is("Glow")) {
            return GLOW_TEXTURE;
        }
        if (texture.is("Star")) {
            return STAR_TEXTURE;
        }
        if (texture.is("Dollar")) {
            return DOLLAR_TEXTURE;
        }
        if (texture.is("Heart")) {
            return HEART_TEXTURE;
        }
        if (texture.is("Snowflake")) {
            return SNOWFLAKE_TEXTURE;
        }

        return GLOW_TEXTURE;
    }

    private float randomRange(double min, double max) {
        if (max <= min) {
            return (float) min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public boolean shouldReplaceVanillaTotem() {
        return isEnabled() && totemMode.getValue();
    }

    private int randomTotemColor() {
        return TOTEM_COLORS[ThreadLocalRandom.current().nextInt(TOTEM_COLORS.length)];
    }

    private final class ParticleData {
        private double prevX;
        private double prevY;
        private double prevZ;
        private double x;
        private double y;
        private double z;
        private float motionX;
        private float motionY;
        private float motionZ;
        private float prevRotation;
        private float rotation;
        private float prevSize;
        private int age;
        private final int maxLife;
        private final float size;
        private final boolean drop;
        private final boolean rotate;
        private final Identifier texture;
        private final int baseColor;
        private final boolean worldParticle;

        private ParticleData(double x, double y, double z, float motionX, float motionY, float motionZ, float size, int maxLife, boolean drop, boolean rotate, Identifier texture, int baseColor) {
            this(x, y, z, motionX, motionY, motionZ, size, maxLife, drop, rotate, texture, baseColor, false);
        }

        private ParticleData(double x, double y, double z, float motionX, float motionY, float motionZ, float size, int maxLife, boolean drop, boolean rotate, Identifier texture, int baseColor, boolean worldParticle) {
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.size = size;
            this.maxLife = maxLife;
            this.drop = drop;
            this.rotate = rotate;
            this.texture = texture;
            this.baseColor = baseColor;
            this.worldParticle = worldParticle;
            this.rotation = randomRange(-180, 180);
        }

        private boolean update() {
            age++;
            prevX = x;
            prevY = y;
            prevZ = z;

            x += motionX;
            y += motionY;
            z += motionZ;

            if (worldParticle) {
                motionX *= 0.9f;
                motionY *= 0.9f;
                motionZ *= 0.9f;
                motionY -= 0.001f;
                if (mc.player != null && mc.player.squaredDistanceTo(x, y, z) > WORLD_PARTICLE_DESPAWN_RADIUS * WORLD_PARTICLE_DESPAWN_RADIUS) {
                    return true;
                }
                return age >= maxLife;
            }

            if (drop) {
                motionY -= 0.018f;
            }

            BlockPos pos = BlockPos.ofFloored(x, y, z);
            if (!mc.world.getBlockState(pos).isAir()) {
                motionX *= -0.7f;
                motionZ *= -0.7f;
                motionY *= -0.5f;
            }

            prevRotation = rotation;
            rotation += 6f;
            motionX *= 0.92f;
            motionY *= 0.92f;
            motionZ *= 0.92f;

            return age >= maxLife;
        }

        private void render(MatrixStack matrices, Camera camera, Vec3d cameraPos, float tickDelta) {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            double ix = MathHelper.lerp(tickDelta, prevX, x) - cameraPos.x;
            double iy = MathHelper.lerp(tickDelta, prevY, y) - cameraPos.y;
            double iz = MathHelper.lerp(tickDelta, prevZ, z) - cameraPos.z;
            float life = 1.0f - (age / (float) maxLife);

            matrices.push();
            matrices.translate(ix, iy, iz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            if (rotate) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, prevRotation, rotation)));
            }

            float drawSize;
            int alpha;
            if (worldParticle && mc.player != null) {
                double distanceSq = mc.player.squaredDistanceTo(x, y, z);
                float distanceFactor = 1.0f;
                if (distanceSq > WORLD_PARTICLE_START_FADE_DISTANCE * WORLD_PARTICLE_START_FADE_DISTANCE) {
                    double distance = Math.sqrt(distanceSq);
                    float progress = (float) ((distance - WORLD_PARTICLE_START_FADE_DISTANCE) / (WORLD_PARTICLE_END_FADE_DISTANCE - WORLD_PARTICLE_START_FADE_DISTANCE));
                    progress = MathHelper.clamp(progress, 0.0f, 1.0f);
                    distanceFactor = 1.0f - progress * progress * progress;
                }

                float timeProgress = MathHelper.clamp(age / (float) maxLife, 0.0f, 1.0f);
                float timeFactor = 1.0f - timeProgress * timeProgress * timeProgress;
                float spawnProgress = MathHelper.clamp(age / 8.0f, 0.0f, 1.0f);
                float spawnOffset = spawnProgress - 1.0f;
                float spawnFactor = 1.0f + 2.70158f * spawnOffset * spawnOffset * spawnOffset + 1.70158f * spawnOffset * spawnOffset;
                float fade = Math.min(timeFactor, distanceFactor) * spawnFactor;
                drawSize = Math.max(0.0f, (0.3f * (2.0f + 1.5f * (worldSize.getFloatValue() - 1.0f))) * fade);
                prevSize = drawSize;
                alpha = drawSize < 0.3f ? MathHelper.clamp((int) (Math.max(0.0f, drawSize / 0.3f) * 255), 0, 255) : 255;
            } else {
                drawSize = MathHelper.lerp(0.35f, prevSize, size * life);
                prevSize = drawSize;
                alpha = MathHelper.clamp((int) (255 * life), 0, 255);
            }
            int whiteGlow = 0xFFFFFFFF;
            int tinted = ColorProvider.interpolateColor(baseColor, whiteGlow, 0.52f);
            int color = ColorProvider.setAlpha(tinted, alpha);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, drawSize, -drawSize, 0f).texture(0f, 1f).color(color);
            buffer.vertex(matrix, -drawSize, -drawSize, 0f).texture(1f, 1f).color(color);
            buffer.vertex(matrix, -drawSize, drawSize, 0f).texture(1f, 0f).color(color);
            buffer.vertex(matrix, drawSize, drawSize, 0f).texture(0f, 0f).color(color);
            matrices.pop();

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }
}
