package tech.onetap.mixin;

import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerCapeModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {

    @Shadow
    @Final
    @Mutable
    private BipedEntityModel<PlayerEntityRenderState> model;

    @Unique
    private PlayerCapeModel<PlayerEntityRenderState> onetap$childCapeModel;

    @Unique
    private BipedEntityModel<PlayerEntityRenderState> onetap$adultCapeModel;

    @Unique
    private boolean onetap$usingChildCapeModel;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At("HEAD"))
    private void onetap$swapCapeModel(net.minecraft.client.util.math.MatrixStack matrices,
                                      net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
                                      int light,
                                      PlayerEntityRenderState state,
                                      float limbAngle,
                                      float limbDistance,
                                      CallbackInfo ci) {
        if (this.onetap$usingChildCapeModel || !state.baby) {
            return;
        }

        if (this.onetap$childCapeModel == null) {
            this.onetap$childCapeModel = new PlayerCapeModel<>(
                    PlayerCapeModel.getTexturedModelData()
                            .transform(BipedEntityModel.BABY_TRANSFORMER)
                            .createModel()
            );
        }

        this.onetap$adultCapeModel = this.model;
        this.model = this.onetap$childCapeModel;
        this.onetap$usingChildCapeModel = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At("RETURN"))
    private void onetap$restoreCapeModel(net.minecraft.client.util.math.MatrixStack matrices,
                                         net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
                                         int light,
                                         PlayerEntityRenderState state,
                                         float limbAngle,
                                         float limbDistance,
                                         CallbackInfo ci) {
        if (!this.onetap$usingChildCapeModel) {
            return;
        }

        this.model = this.onetap$adultCapeModel;
        this.onetap$adultCapeModel = null;
        this.onetap$usingChildCapeModel = false;
    }
}
