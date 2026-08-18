package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.EventGameUpdate;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.base.Instance;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@ModuleInformation(moduleName = "ProjectileHelper", moduleCategory = ModuleCategory.COMBAT)
public class ProjectileHelper extends Module {

    private static final Identifier TARGET_TEXTURE = Identifier.of("mre", "images/target.png");
    private LivingEntity target;
    private float markerRotation = 0.0f;
    private float markerRotationSpeed = 0.0f;
    private boolean markerReverse = false;

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        target = findTarget();
    }

    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (!isEnabled()) return;
        if (target == null) return;

        if (isPullingBow()) {
            Vec3d playerPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

            Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.8, 0);
            Vec3d targetVel = new Vec3d(
                    -(target.prevX - target.getX()),
                    -(target.prevY - target.getY()),
                    -(target.prevZ - target.getZ())
            );

            double distance = playerPos.distanceTo(targetPos);
            double flightTime = distance;

            Vec3d predictedTarget = targetPos.add(targetVel.multiply(flightTime, flightTime / 8.0, flightTime));

            Vec2f rotation = RotationUtil.calculate(predictedTarget);
            Rotation targetRot = new Rotation(rotation.x, rotation.y);

            RotationComponent.update(targetRot, 360, 360, 360, 360, 0, 3, false);
        }
    }

    @Subscribe
    private void onWorldRender(EventWorldRender e) {
        if (!isEnabled() || mc.player == null || mc.world == null || target == null || !target.isAlive()) return;
        if (!isPullingBow()) return;

        float tickDelta = e.getTickDelta();
        double tx = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) + target.getHeight() * 0.55;
        double tz = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        updateMarkerRotation();

        e.getMatrixStack().push();
        e.getMatrixStack().translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);
        e.getMatrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        e.getMatrixStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        e.getMatrixStack().scale(-0.125f, -0.125f, 0.125f);
        e.getMatrixStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(markerRotation));

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, TARGET_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f matrix = e.getMatrixStack().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int color = 0xFFFFFFFF;
        float size = 6.5f;
        buffer.vertex(matrix, -size, size, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(matrix, size, size, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(matrix, size, -size, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(matrix, -size, -size, 0.0f).texture(0.0f, 0.0f).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        e.getMatrixStack().pop();
    }

    private boolean isPullingBow() {
        return mc.player != null
                && mc.player.getMainHandStack().getItem() == Items.BOW
                && mc.player.isUsingItem();
    }

    private void updateMarkerRotation() {
        if (!markerReverse) {
            markerRotationSpeed += 0.01F;
            if (markerRotationSpeed > 2.3F) {
                markerRotationSpeed = 2.3F;
                markerReverse = true;
            }
        } else {
            markerRotationSpeed -= 0.01F;
            if (markerRotationSpeed < -2.3F) {
                markerRotationSpeed = -2.3F;
                markerReverse = false;
            }
        }
        markerRotation += markerRotationSpeed;
        markerRotation %= 360.0F;
    }

    private boolean isValid(LivingEntity entity, AntiBot antiBot) {
        if (mc.player == null) return false;
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0) return false;

        if (entity instanceof PlayerEntity player) {
            if (!FriendRepository.shouldAttack(player)) return false;
            if (antiBot != null && antiBot.isBot(player)) return false;
            if (!hasLineOfSight(player)) return false;
            return true;
        }

        return false;
    }

    private boolean hasLineOfSight(LivingEntity entity) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d from = mc.player.getEyePos();
        Vec3d to = entity.getPos().add(0.0, entity.getHeight() * 0.8, 0.0);
        HitResult hit = mc.world.raycast(new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private LivingEntity findTarget() {
        if (mc.player == null || mc.world == null) return null;

        AntiBot antiBot = Instance.get(AntiBot.class);
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValid(living, antiBot)) continue;
            targets.add(living);
        }

        if (targets.isEmpty() || !isEnabled()) return null;

        targets.sort(Comparator.comparingDouble(entity -> {
            Vec2f vec = RotationUtil.calculate(entity.getBoundingBox().getCenter());
            double dy = Math.abs(MathHelper.wrapDegrees(vec.x - mc.player.getYaw()));
            double dp = Math.abs(MathHelper.wrapDegrees(vec.y - mc.player.getPitch()));
            return dy + dp;
        }));

        return targets.get(0);
    }
}
