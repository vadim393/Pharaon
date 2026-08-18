package tech.onetap.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

//    @Inject(method = "render", at = @At("HEAD"))
//    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
//        // Intentionally left blank to keep vanilla SplashOverlay rendering.
//    }
}
