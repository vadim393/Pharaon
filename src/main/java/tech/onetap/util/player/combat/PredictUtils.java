package tech.onetap.util.player.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.server.Server;

@UtilityClass
public class PredictUtils implements IMinecraft {
    public static Vec3d predict(LivingEntity target, double ticksAhead) {
        Vec3d currentPos = target.getPos();
        Vec3d previousPos = new Vec3d(target.prevX, target.prevY, target.prevZ);
        Vec3d sampledVelocity = currentPos.subtract(previousPos);
        Vec3d trackedVelocity = target.getVelocity();
        Vec3d velocity = sampledVelocity.multiply(0.6).add(trackedVelocity.multiply(0.4));

        if (velocity.horizontalLength() < 0.015 && Math.abs(velocity.y) < 0.08) {
            return currentPos.add(0.0, target.getHeight() * 0.5, 0.0);
        }

        double predictTicks = Math.max(0.0, ticksAhead + getLatencyCompensation(target));
        double remaining = Math.min(20.0, predictTicks);

        Vec3d point = currentPos;
        float yaw = target.getYaw();
        float pitch = target.getPitch();
        float yawSpeed = MathHelper.wrapDegrees(target.getYaw() - target.prevYaw);
        float pitchSpeed = MathHelper.wrapDegrees(target.getPitch() - target.prevPitch);

        while (remaining > 0.0) {
            double step = Math.min(1.0, remaining);
            if (target.isGliding()) {
                GlideStep glideStep = glideStep(velocity, yaw, pitch);
                velocity = glideStep.velocity();
                point = point.add(velocity.multiply(step));
                yaw += yawSpeed * 0.85f;
                pitch += pitchSpeed * 0.85f;
            } else {
                velocity = regularStep(target, velocity);
                point = point.add(velocity.multiply(step));
            }
            remaining -= step;
        }

        return point.add(0.0, target.getHeight() * 0.5, 0.0);
    }

    private static double getLatencyCompensation(LivingEntity target) {
        if (!(target instanceof PlayerEntity player) || mc.getNetworkHandler() == null) {
            return 0.0;
        }

        int ping = Math.max(0, Server.getPing(player));
        return MathHelper.clamp((ping / 50.0) * 0.4, 0.0, 3.0);
    }

    private static Vec3d regularStep(LivingEntity target, Vec3d velocity) {
        double gravity = target.hasNoGravity() ? 0.0 : target.getFinalGravity();
        boolean slowFalling = velocity.y <= 0.0 && target.hasStatusEffect(StatusEffects.SLOW_FALLING);
        if (slowFalling) {
            gravity = Math.min(gravity, 0.01);
        }

        Vec3d withGravity = velocity.add(0.0, -gravity, 0.0);
        double dragXZ = target.isOnGround() ? 0.6 : 0.91;
        double dragY = target.isOnGround() ? 0.98 : 0.98;
        return new Vec3d(withGravity.x * dragXZ, withGravity.y * dragY, withGravity.z * dragXZ);
    }

    private static GlideStep glideStep(Vec3d velocity, float yaw, float pitch) {
        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        float pitchRad = pitch * 0.017453292F;
        double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double horizontalSpeed = velocity.horizontalLength();
        double liftFactor = MathHelper.square(Math.cos(pitchRad));

        Vec3d nextVelocity = velocity.add(0.0, 0.08 * (-1.0 + liftFactor * 0.75), 0.0);

        if (nextVelocity.y < 0.0 && lookHorizontal > 0.0) {
            double drop = nextVelocity.y * -0.1 * liftFactor;
            nextVelocity = nextVelocity.add(look.x * drop / lookHorizontal, drop, look.z * drop / lookHorizontal);
        }

        if (pitchRad < 0.0F && lookHorizontal > 0.0) {
            double glideBoost = horizontalSpeed * (double) (-MathHelper.sin(pitchRad)) * 0.04;
            nextVelocity = nextVelocity.add(-look.x * glideBoost / lookHorizontal, glideBoost * 2.2, -look.z * glideBoost / lookHorizontal);
        }

        if (lookHorizontal > 0.0) {
            nextVelocity = nextVelocity.add((look.x / lookHorizontal * horizontalSpeed - nextVelocity.x) * 0.1, 0.0, (look.z / lookHorizontal * horizontalSpeed - nextVelocity.z) * 0.1);
        }

        return new GlideStep(new Vec3d(nextVelocity.x * 0.99, nextVelocity.y * 0.98, nextVelocity.z * 0.99));
    }

    private record GlideStep(Vec3d velocity) {}
}
