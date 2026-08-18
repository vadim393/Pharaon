package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventChangeSprint;
import tech.onetap.event.list.EventNoPush;
import tech.onetap.module.list.combat.ElytraTarget;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.render.SwingAnimations;
import tech.onetap.util.base.Instance;
import tech.onetap.util.player.combat.PredictUtils;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void cancelPushAway(Entity entity, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        EventNoPush event = new EventNoPush(EventNoPush.NoPushType.Player);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void onSetSprinting(boolean sprinting, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity && sprinting) {
            var event = new EventChangeSprint(true);
            event.post();

            if (!event.isSprinting()) ci.cancel();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        var swing = Instance.get(SwingAnimations.class);

        boolean allow = swing != null && swing.isEnabled();
        if (allow && swing.onlyWithKillAura.getValue()) {
            var killAura = Instance.get(KillAura.class);
            allow = killAura != null && killAura.isEnabled();
        }

        if (allow) {
            var speed = (int) swing.speed.getValue();
            cir.setReturnValue(25 - speed * 2);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity player && player.isGliding()) {
            try {
                KillAura killAura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
                ElytraTarget elytraTarget = Onetap.getInstance().getModuleStorage().get(ElytraTarget.class);

                if (killAura != null && killAura.isEnabled() && elytraTarget != null && elytraTarget.elytraSlowdown.getValue()
                        && elytraTarget.slowdownMode.is("По радиусу") && killAura.getTarget() != null) {
                    Vec3d predictedPos = PredictUtils.predict(killAura.getTarget(), elytraTarget.predictValue.getValue());
                    double dist = player.getEyePos().distanceTo(predictedPos);
                    double radius = elytraTarget.slowdownRadius.getValue();

                    Vec3d toTarget = predictedPos.subtract(player.getEyePos());
                    boolean movingTowards = player.getVelocity().dotProduct(toTarget) > 0;

                    if (dist < radius && movingTowards) {
                        double ratio = MathHelper.clamp(dist / radius, 0.0, 1.0);
                        double smoothCurve = ratio * ratio * (3.0 - 2.0 * ratio);
                        double speedFactor = elytraTarget.minSpeed.getValue() + (1.0 - elytraTarget.minSpeed.getValue()) * smoothCurve;

                        player.setVelocity(player.getVelocity().multiply(speedFactor, speedFactor, speedFactor));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
