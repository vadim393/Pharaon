package tech.onetap.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.NameTags;
import tech.onetap.util.base.Instance;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<S extends EntityRenderState> {
    @Inject(method = "renderLabelIfPresent", at = @At(value = "HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        NameTags tags = Instance.get(NameTags.class);
        if (tags != null && tags.shouldHideVanillaLabel(state)) {
            ci.cancel();
        }
    }
}
