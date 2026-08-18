package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.event.list.EventNoPush;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
    @Inject(method = "pullHookedEntity", at = @At("HEAD"), cancellable = true)
    private void onPullHookedEntity(Entity entity, CallbackInfo ci) {
        if (entity != MinecraftClient.getInstance().player) {
            return;
        }

        EventNoPush event = new EventNoPush(EventNoPush.NoPushType.FishingRod);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
