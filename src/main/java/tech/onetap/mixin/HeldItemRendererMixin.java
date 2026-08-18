package tech.onetap.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.SwingAnimations;
import tech.onetap.module.list.render.ViewModel;
import tech.onetap.util.base.Instance;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Shadow
    private static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
        throw new AssertionError();
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var viewModel = Instance.get(ViewModel.class);
        var isMainHand = hand == Hand.MAIN_HAND;
        var arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        if (viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            viewModel.applyHandPosition(matrices, arm);
        }

        var swingAnimations = Instance.get(SwingAnimations.class);
        if (swingAnimations.isEnabled() && swingAnimations.isHmiMode() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            swingAnimations.applyHmiHandPosition(matrices, arm, isMainHand);
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        var swingAnimation = Instance.get(SwingAnimations.class);
        if (swingAnimation.isEnabled()) {
            if (arm == Arm.RIGHT || swingAnimation.isHmiMode()) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                swingArm(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }

    @Overwrite
    public void renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        float swingProgress = player.getHandSwingProgress(tickDelta);
        Hand preferredHand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float pitch = player.getLerpedPitch(tickDelta);
        HeldItemRenderer.HandRenderType handRenderType = getHandRenderType(player);

        if (handRenderType.renderMainHand) {
            float handSwing = preferredHand == Hand.MAIN_HAND ? swingProgress : 0.0F;
            float equipProgress = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
            this.renderFirstPersonItem(player, tickDelta, pitch, Hand.MAIN_HAND, handSwing, this.mainHand, equipProgress, matrices, vertexConsumers, light);
        }

        if (handRenderType.renderOffHand) {
            float handSwing = preferredHand == Hand.OFF_HAND ? swingProgress : 0.0F;
            float equipProgress = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
            this.renderFirstPersonItem(player, tickDelta, pitch, Hand.OFF_HAND, handSwing, this.offHand, equipProgress, matrices, vertexConsumers, light);
        }

        vertexConsumers.draw();
    }
}
