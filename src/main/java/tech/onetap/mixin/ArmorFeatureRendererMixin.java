package tech.onetap.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.GlowESP;
import tech.onetap.util.base.Instance;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> {

    @Unique
    private PlayerEntityRenderState onetap$glowPlayerState;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("HEAD"))
    private void onetap$capturePlayerState(MatrixStack matrices,
                                           VertexConsumerProvider vertexConsumers,
                                           int light,
                                           S state,
                                           float limbAngle,
                                           float limbDistance,
                                           CallbackInfo ci) {
        this.onetap$glowPlayerState = state instanceof PlayerEntityRenderState playerState ? playerState : null;
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("RETURN"))
    private void onetap$clearPlayerState(MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers,
                                         int light,
                                         S state,
                                         float limbAngle,
                                         float limbDistance,
                                         CallbackInfo ci) {
        this.onetap$glowPlayerState = null;
    }

    @Inject(method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V", at = @At("TAIL"))
    private void onetap$captureArmor(MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     ItemStack stack,
                                     EquipmentSlot slot,
                                     int light,
                                     A armorModel,
                                     CallbackInfo ci) {
        if (stack == null || stack.isEmpty() || armorModel == null || this.onetap$glowPlayerState == null) {
            return;
        }

        GlowESP glowESP = Instance.get(GlowESP.class);
        if (glowESP == null) {
            return;
        }

        glowESP.capture(this.onetap$glowPlayerState, matrices, vertexConsumers, armorModel, light);
    }
}
