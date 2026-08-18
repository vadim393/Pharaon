package tech.onetap.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.list.render.AspectRatio;
import tech.onetap.module.list.render.GlowESP;
import tech.onetap.util.base.Instance;
import tech.onetap.util.render.renderers.DrawUtil;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow private float zoom;
    @Shadow private float zoomX;
    @Shadow private float zoomY;

    @Shadow public abstract float getFarPlaneDistance();

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        var matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);

        var event = new EventWorldRender(matrixStack, tickCounter.getTickDelta(false));
        event.post();
        DrawUtil.onRender3D(event.getMatrixStack());

        GlowESP glowESP = Instance.get(GlowESP.class);
        if (glowESP != null) {
            glowESP.renderComposite();
        }
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    private void hookBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatio aspectRatio = Instance.get(AspectRatio.class);
        if (aspectRatio == null || !aspectRatio.isEnabled()) {
            return;
        }

        Matrix4f matrix4f = new Matrix4f();
        if (zoom != 1.0f) {
            matrix4f.translate(zoomX, -zoomY, 0.0f);
            matrix4f.scale(zoom, zoom, 1.0f);
        }

        matrix4f.perspective((float) Math.toRadians(fovDegrees), aspectRatio.getAspectRatio(), 0.05f, getFarPlaneDistance());
        cir.setReturnValue(matrix4f);
    }
}
