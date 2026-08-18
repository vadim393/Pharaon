package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ModuleInformation(
        moduleName = "Arrows",
        moduleDesc = "Показывает стрелки на игроков",
        moduleCategory = ModuleCategory.RENDER
)
public final class Arrows extends Module {

    private static final Identifier ICON_ID = Identifier.of("mre", "images/arrow3.png");

    private boolean textureLoaded = false;
    private final Map<UUID, Float> smoothedYawByPlayer = new HashMap<>();

    private final SliderSetting radiusSetting = new SliderSetting("Радиус", 50.0f, 30.0f, 100.0f, 1.0f);
    private final SliderSetting sizeSetting = new SliderSetting("Размер", 16.0f, 8.0f, 20.0f, 1.0f);
    private final BooleanSetting showHpBar = new BooleanSetting("Показывать хп бар", true);

    @Subscribe
    private void onHud(EventHUD e) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) return;
        if (!mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) return;

        if (!textureLoaded) {
            mc.getTextureManager().registerTexture(ICON_ID, new ResourceTexture(ICON_ID));
            textureLoaded = true;
        }

        MatrixStack matrix = e.getDrawContext().getMatrices();

        float tickDelta = e.getRenderTickCounter().getTickDelta(true);

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> !isInFieldOfView(p, tickDelta))
                .toList();

        if (players.isEmpty()) return;
        Set<UUID> visibleIds = players.stream().map(AbstractClientPlayerEntity::getUuid).collect(Collectors.toSet());
        smoothedYawByPlayer.keySet().removeIf(id -> !visibleIds.contains(id));

        float middleW = mc.getWindow().getScaledWidth() / 2.0f;
        float middleH = mc.getWindow().getScaledHeight() / 2.0f;
        float posY = middleH - radiusSetting.getFloatValue();
        float size = sizeSetting.getFloatValue();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderTexture(0, ICON_ID);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        players.forEach(player -> {
            // базовый статичный градиент, не зависящий от темы
            int color = ColorProvider.rgba(255, 255, 255, 255);
            int color2 = ColorProvider.rgba(180, 180, 180, 255);

            // для друзей оставляем зелёный
            if (FriendRepository.isFriend(player.getNameForScoreboard())) {
                color = ColorProvider.rgba(0, 255, 0, 255);
                color2 = ColorProvider.rgba(0, 200, 0, 255);
            }

            float targetYaw = getRotations(player, tickDelta)
                    - MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
            float previousYaw = smoothedYawByPlayer.getOrDefault(player.getUuid(), targetYaw);
            float yaw = lerpAngle(previousYaw, targetYaw, 0.2f);
            smoothedYawByPlayer.put(player.getUuid(), yaw);

            matrix.push();
            matrix.translate(middleW, middleH, 0.0f);
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
            matrix.translate(-middleW, -middleH, 0.0f);

            Matrix4f matrix4f = matrix.peek().getPositionMatrix();

            long time = System.currentTimeMillis();
            double phase = time * (8.0 / 1000.0) + (player.getId() * 0.35);
            float factor = (float) (Math.sin(phase) * 0.5 + 0.5);
            int finalColor = ColorProvider.interpolateColor(color, color2, factor);

            buffer.vertex(matrix4f, middleW - (size / 2.0f), posY + size, 0.0f).texture(0.0f, 1.0f).color(finalColor);
            buffer.vertex(matrix4f, middleW + size / 2.0f, posY + size, 0.0f).texture(1.0f, 1.0f).color(finalColor);
            buffer.vertex(matrix4f, middleW + size / 2.0f, posY, 0.0f).texture(1.0f, 0.0f).color(finalColor);
            buffer.vertex(matrix4f, middleW - (size / 2.0f), posY, 0.0f).texture(0.0f, 0.0f).color(finalColor);

            matrix.pop();
        });

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (showHpBar.getValue()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            float barWidth = 1.5f;
            float barHeight = 8f;

            for (AbstractClientPlayerEntity player : players) {
                float targetYaw = getRotations(player, tickDelta)
                        - MathHelper.lerp(tickDelta, mc.player.prevYaw, mc.player.getYaw());
                float previousYaw = smoothedYawByPlayer.getOrDefault(player.getUuid(), targetYaw);
                float yaw = lerpAngle(previousYaw, targetYaw, 0.2f);

                matrix.push();
                matrix.translate(middleW, middleH, 0.0f);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                matrix.translate(-middleW, -middleH, 0.0f);

                float maxHp = player.getMaxHealth();
                float hp = Math.min(player.getHealth() + player.getAbsorptionAmount(), maxHp);
                float hpPercent = maxHp > 0 ? hp / maxHp : 0f;
                hpPercent = MathHelper.clamp(hpPercent, 0f, 1f);

                float barX = middleW + size / 2;
                float barY = posY + size / 2f - barHeight / 2f;

                DrawUtil.drawRect(matrix, barX - 0.5f, barY - 0.5f, barWidth + 1f, barHeight + 1f, 0xFF000000);
                DrawUtil.drawRect(matrix, barX, barY, barWidth, barHeight, ColorProvider.rgba(71, 71, 71, 255));

                float hpFillHeight = barHeight * hpPercent;
                if (hpFillHeight > 0.01f) {
                    int hpColor = ColorProvider.interpolateColor(
                            ColorProvider.rgba(255, 0, 0, 255),
                            ColorProvider.rgba(0, 255, 0, 255),
                            hpPercent
                    );
                    DrawUtil.drawRect(matrix, barX, barY + (barHeight - hpFillHeight), barWidth, hpFillHeight, hpColor);
                }

                matrix.pop();
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    public void onDisable() {
        smoothedYawByPlayer.clear();
        super.onDisable();
    }

    private float lerpAngle(float from, float to, float speed) {
        return from + MathHelper.wrapDegrees(to - from) * speed;
    }

    public static float getRotations(Entity entity, float tickDelta) {
        double ex = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double ez = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        double px = MathHelper.lerp(tickDelta, MinecraftClient.getInstance().player.prevX, MinecraftClient.getInstance().player.getX());
        double pz = MathHelper.lerp(tickDelta, MinecraftClient.getInstance().player.prevZ, MinecraftClient.getInstance().player.getZ());
        double x = ex - px;
        double z = ez - pz;
        return (float) -(Math.atan2(x, z) * (180.0 / Math.PI));
    }

    private boolean isInFieldOfView(AbstractClientPlayerEntity player, float tickDelta) {
        float x = (float) MathHelper.lerp(tickDelta, player.prevX, player.getX());
        float y = (float) MathHelper.lerp(tickDelta, player.prevY, player.getY());
        float z = (float) MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
        float width = player.getWidth() * 0.5f;
        float height = player.getHeight();

        return isPointOnScreen(new Vec3d(x, y + height * 0.15f, z))
                || isPointOnScreen(new Vec3d(x, y + height * 0.5f, z))
                || isPointOnScreen(new Vec3d(x, y + height * 0.9f, z))
                || isPointOnScreen(new Vec3d(x - width, y + height * 0.5f, z))
                || isPointOnScreen(new Vec3d(x + width, y + height * 0.5f, z));
    }

    private boolean isPointOnScreen(Vec3d point) {
        var projected = ProjectionUtil.project(point);
        if (projected.getX() == Float.MAX_VALUE || projected.getY() == Float.MAX_VALUE) {
            return false;
        }

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        return projected.getX() >= 0.0f && projected.getX() <= screenWidth
                && projected.getY() >= 0.0f && projected.getY() <= screenHeight;
    }
}
