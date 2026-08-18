package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Flight", moduleCategory = ModuleCategory.MOVEMENT)
public class Flight extends Module {

    public final SliderSetting speedXZ = new SliderSetting("Скорость XZ", 1.5f, 1.0f, 10.0f, 0.1f);
    public final SliderSetting speedY = new SliderSetting("Скорость Y", 1.5f, 0.0f, 10.0f, 0.1f);

    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        double motionY = getMotionY();
        setMotion(speedXZ.getFloatValue());
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, motionY, v.z);
        
        mc.player.setOnGround(false);
        mc.player.getAbilities().flying = false;
    }

    private double getMotionY() {
        if (mc.options.sneakKey.isPressed()) {
            return -speedY.getFloatValue();
        } else if (mc.options.jumpKey.isPressed()) {
            return speedY.getFloatValue();
        }
        return 0.0;
    }

    private void setMotion(float speed) {
        float yaw = mc.player.getYaw();
        float f = mc.player.input.movementForward;
        float s = mc.player.input.movementSideways;
        float speedScale = speed / 3.0F;
        double x = 0.0;
        double z = 0.0;
        if (f != 0.0F || s != 0.0F) {
            float yawRad = (float) Math.toRadians(yaw);
            x = -MathHelper.sin(yawRad) * speedScale * f + MathHelper.cos(yawRad) * speedScale * s;
            z = MathHelper.cos(yawRad) * speedScale * f + MathHelper.sin(yawRad) * speedScale * s;
        }
        mc.player.setVelocity(x, mc.player.getVelocity().y, z);
    }
}
