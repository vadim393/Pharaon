package tech.onetap.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.WorldTweaks;
import tech.onetap.util.render.providers.ColorProvider;

@Mixin(BackgroundRenderer.class)
public class FogMixin {

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, 
                                    float viewDistance, boolean thickenFog, float tickDelta, 
                                    CallbackInfoReturnable<Fog> cir) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            WorldTweaks worldTweaks = Onetap.getInstance().getModuleStorage().get(WorldTweaks.class);
            if (worldTweaks != null && worldTweaks.isEnabled() && worldTweaks.isFogEnabled()) {
                float fogEnd = viewDistance;
                if (worldTweaks.isCustomFogDistanceEnabled()) {
                    fogEnd = viewDistance * worldTweaks.getFogDistance();
                }
                fogEnd = Math.max(2.0f, fogEnd);
                float fogStart = Math.max(0.0f, fogEnd * 0.25f);

                int fogColor = worldTweaks.getFogColor();
                float r = ColorProvider.red(fogColor) / 255.0f;
                float g = ColorProvider.green(fogColor) / 255.0f;
                float b = ColorProvider.blue(fogColor) / 255.0f;
                float a = ColorProvider.alpha(fogColor) / 255.0f;
                if (a <= 0.0f) a = color.w;
                a = MathHelper.clamp(a, 0.0f, 1.0f);

                Fog customFog = new Fog(fogStart, fogEnd, FogShape.SPHERE, r, g, b, a);
                cir.setReturnValue(customFog);
            }
        } catch (Exception ignored) {}
    }
}
