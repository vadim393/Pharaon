package tech.onetap.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.ItemPhysics;
import tech.onetap.util.base.Instance;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Shadow @Final private Random random;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void capturePhysicsState(net.minecraft.entity.ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ItemPhysics.capture(state, entity.isOnGround());
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void renderWithPhysics(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ItemPhysics module = Instance.get(ItemPhysics.class);
        if (module == null || !module.isEnabled() || state.itemRenderState.isEmpty() || state.displayName != null) {
            return;
        }

        ci.cancel();
        matrices.push();

        float scaleY = state.itemRenderState.getTransformation().scale.y();
        ItemPhysics.PhysicsState physicsState = ItemPhysics.getState(state);
        boolean onGround = physicsState != null && physicsState.onGround();

        if (onGround) {
            matrices.translate(0.0F, 0.02F + 0.0625F * scaleY, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.uniqueOffset * 180.0F));
        } else {
            float swing = (state.age + state.uniqueOffset) * 2.0F;
            float bob = MathHelper.sin(state.age / 10.0F + state.uniqueOffset) * 0.06F + 0.1F;
            matrices.translate(0.0F, bob + 0.25F * scaleY, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swing * 1.5F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35.0F + swing));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F + swing * 0.5F));
        }

        ItemEntityRenderer.renderStack(matrices, vertexConsumers, light, state, this.random);
        matrices.pop();
    }
}
