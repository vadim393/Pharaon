package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.event.list.EventNoPush;

@Mixin(Entity.class)
public class EntityNoPushMixin {
    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void onIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        EventNoPush event = new EventNoPush(EventNoPush.NoPushType.Water);
        event.post();
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}
