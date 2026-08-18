package tech.onetap.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.AmbienceSkyRenderer;
import tech.onetap.module.list.render.WorldTweaks;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "method_62215", at = @At("TAIL"))
    private void onRenderSkyPass(Fog fog, DimensionEffects.SkyType skyType, float tickDelta, DimensionEffects dimensionEffects, CallbackInfo ci) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            WorldTweaks worldTweaks = Onetap.getInstance().getModuleStorage().get(WorldTweaks.class);
            if (worldTweaks != null && worldTweaks.isEnabled() && worldTweaks.isShaderSkyEnabled()) {
                AmbienceSkyRenderer.render(worldTweaks, tickDelta);
            }
        } catch (Exception ignored) {}
    }
}
