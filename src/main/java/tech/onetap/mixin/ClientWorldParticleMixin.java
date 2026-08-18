package tech.onetap.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.Particles;

@Mixin(ClientWorld.class)
public class ClientWorldParticleMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (parameters != ParticleTypes.TOTEM_OF_UNDYING) return;
        if (Onetap.getInstance() == null || Onetap.getInstance().getModuleStorage() == null) return;

        Particles particles = Onetap.getInstance().getModuleStorage().get(Particles.class);
        if (particles != null && particles.shouldReplaceVanillaTotem()) {
            ci.cancel();
        }
    }
}
