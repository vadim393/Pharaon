package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.combat.ElytraTarget;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.render.GlowESP;
import tech.onetap.module.list.render.NameTags;
import tech.onetap.module.list.render.WorldTweaks;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.base.Instance;
import tech.onetap.util.math.RotationUtil;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M>, IMinecraft {

    @Unique
    private PlayerEntityModel onetap$childPlayerModel;

    @Unique
    private M onetap$adultRenderModel;

    @Unique
    private boolean onetap$usingChildPlayerModel;

    @Shadow
    protected M model;

    @Shadow
    @Final
    private List<FeatureRenderer<S, M>> features;

    @Shadow
    private static float clampBodyYaw(LivingEntity entity, float degrees, float tickDelta) {
        return 0;
    }

    @Shadow
    public static boolean shouldFlipUpsideDown(LivingEntity entity) {
        return false;
    }

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), argsOnly = true)
    private VertexConsumerProvider modifyVertexConsumers(VertexConsumerProvider original, S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        return original;
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    public void onRender(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.onetap$swapToChildPlayerModel(state);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void onRenderReturn(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.onetap$restoreAdultPlayerModel();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRenderMainModel(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        NameTags tags = Instance.get(NameTags.class);
        if (tags != null && tags.shouldRenderEntityGradientShader(playerState)) {
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }
            tags.renderPlayerGradientOverlay(matrices, playerState, model, light);
        }

        GlowESP glowESP = Instance.get(GlowESP.class);
        if (glowESP != null) {
            glowESP.capture(playerState, matrices, vertexConsumers, model, light);
        }
    }

    /**
     * @author onetap
     * @reason elytra visual rotation to target with Перегон check
     */
    @Overwrite
    public void updateRenderState(T livingEntity, S livingEntityRenderState, float f) {
        super.updateRenderState(livingEntity, livingEntityRenderState, f);

        float yaw = MathHelper.lerpAngleDegrees(f, livingEntity.prevHeadYaw, livingEntity.headYaw);

        livingEntityRenderState.bodyYaw = clampBodyYaw(livingEntity, yaw, f);
        livingEntityRenderState.yawDegrees = MathHelper.wrapDegrees(yaw - livingEntityRenderState.bodyYaw);
        livingEntityRenderState.pitch = livingEntity.getLerpedPitch(f);

        livingEntityRenderState.customName = livingEntity.getCustomName();
        livingEntityRenderState.flipUpsideDown = shouldFlipUpsideDown(livingEntity);

        if (livingEntityRenderState.flipUpsideDown) {
            livingEntityRenderState.pitch *= -1.0F;
            livingEntityRenderState.yawDegrees *= -1.0F;
        }

        if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
            livingEntityRenderState.limbFrequency = livingEntity.limbAnimator.getPos(f);
            livingEntityRenderState.limbAmplitudeMultiplier = livingEntity.limbAnimator.getSpeed(f);
        } else {
            livingEntityRenderState.limbFrequency = 0.0F;
            livingEntityRenderState.limbAmplitudeMultiplier = 0.0F;
        }

        if (livingEntity.getVehicle() instanceof LivingEntity livingEntity2)
            livingEntityRenderState.headItemAnimationProgress = livingEntity2.limbAnimator.getPos(f);
        else livingEntityRenderState.headItemAnimationProgress = livingEntityRenderState.limbFrequency;

        boolean smallPlayer = livingEntity instanceof PlayerEntity player && WorldTweaks.shouldRenderSmallPlayer(player);

        livingEntityRenderState.baseScale = livingEntity.getScale();
        livingEntityRenderState.ageScale = livingEntity.getScaleFactor();
        if (smallPlayer) {
            livingEntityRenderState.ageScale *= 0.5F;
        }
        livingEntityRenderState.pose = livingEntity.getPose();
        livingEntityRenderState.sleepingDirection = livingEntity.getSleepingDirection();
        if (livingEntityRenderState.sleepingDirection != null)
            livingEntityRenderState.standingEyeHeight = livingEntity.getEyeHeight(EntityPose.STANDING);
        livingEntityRenderState.shaking = livingEntity.isFrozen();
        livingEntityRenderState.baby = livingEntity.isBaby() || smallPlayer;
        livingEntityRenderState.touchingWater = livingEntity.isTouchingWater();
        livingEntityRenderState.usingRiptide = livingEntity.isUsingRiptide();
        livingEntityRenderState.hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;
        livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? (float) livingEntity.deathTime + f : 0.0F;
        livingEntityRenderState.invisibleToPlayer = livingEntityRenderState.invisible && livingEntity.isInvisibleTo(MinecraftClient.getInstance().player);
        livingEntityRenderState.hasOutline = MinecraftClient.getInstance().hasOutline(livingEntity);

        if (livingEntity instanceof PlayerEntity player) {
            NameTags tags = Instance.get(NameTags.class);
            if (tags != null && tags.isEnabled() && tags.shouldGlowEntity(player)) {
                livingEntityRenderState.hasOutline = true;
            }
        }

        if (livingEntity == mc.player && livingEntity.isGliding()) {
            KillAura killAura = Instance.get(KillAura.class);
            ElytraTarget elytraTarget = Instance.get(ElytraTarget.class);

            if (killAura != null && elytraTarget != null && killAura.isEnabled() && killAura.visualElytraRotation.getValue()) {
                LivingEntity target = killAura.getTarget();

                if (target != null && target.isAlive() && target.isGliding()) {

                    Vec3d playerPos = livingEntity.getLerpedPos(f);
                    Vec3d targetPos = target.getLerpedPos(f);

                    Vec3d targetLook = target.getRotationVec(f).normalize();

                    Vec3d targetToPlayer = playerPos.subtract(targetPos);

                    double dot = targetToPlayer.dotProduct(targetLook);

                    Vec3d predict = killAura.getPredictPoint(target, elytraTarget.predictValue.getValue());
                    double distToPredict = playerPos.distanceTo(predict);

                    if (dot > 0.0 && distToPredict < 6.0) {

                        Vec3d center = targetPos.add(0.0, target.getHeight() / 2.0, 0.0);
                        Vec2f rotation = RotationUtil.calculate(center);

                        livingEntityRenderState.bodyYaw = rotation.x;
                        livingEntityRenderState.yawDegrees = 0.0F;
                        livingEntityRenderState.pitch = rotation.y;
                    }
                }
            }
        }

    }

    @Unique
    private void onetap$swapToChildPlayerModel(S state) {
        if (this.onetap$usingChildPlayerModel || !(state instanceof PlayerEntityRenderState) || !state.baby) {
            return;
        }
        if (!(this.model instanceof PlayerEntityModel playerModel)) {
            return;
        }
        this.onetap$ensureChildPlayerModel(playerModel);
        if (this.onetap$childPlayerModel == null) {
            return;
        }

        this.onetap$adultRenderModel = this.model;
        this.model = (M) this.onetap$childPlayerModel;
        this.onetap$usingChildPlayerModel = true;
    }

    @Unique
    private void onetap$restoreAdultPlayerModel() {
        if (!this.onetap$usingChildPlayerModel) {
            return;
        }

        this.model = this.onetap$adultRenderModel;
        this.onetap$adultRenderModel = null;
        this.onetap$usingChildPlayerModel = false;
    }

    @Unique
    private void onetap$ensureChildPlayerModel(PlayerEntityModel playerModel) {
        if (this.onetap$childPlayerModel != null) {
            return;
        }

        boolean thinArms = ((PlayerEntityModelAccessor) playerModel).onetap$isThinArms();
        ModelData modelData = PlayerEntityModel.getTexturedModelData(Dilation.NONE, thinArms);
        TexturedModelData texturedModelData = TexturedModelData.of(modelData, 64, 64).transform(BipedEntityModel.BABY_TRANSFORMER);
        this.onetap$childPlayerModel = new PlayerEntityModel(texturedModelData.createModel(), thinArms);
    }
}
