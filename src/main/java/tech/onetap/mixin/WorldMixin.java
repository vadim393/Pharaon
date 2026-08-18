package tech.onetap.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.WorldTweaks;

@Mixin(ClientWorld.Properties.class)
public class WorldMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            WorldTweaks worldTweaks = Onetap.getInstance().getModuleStorage().get(WorldTweaks.class);
            if (worldTweaks != null && worldTweaks.isEnabled() && worldTweaks.shouldOverrideTime()) {
                cir.setReturnValue(worldTweaks.getCustomTime());
            }
        } catch (Exception ignored) {}
    }
}
