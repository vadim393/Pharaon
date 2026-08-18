package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.util.base.Instance;
import tech.onetap.util.math.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "Grim Glide", moduleCategory = ModuleCategory.MOVEMENT)
public class GrimGlide extends Module {
    private final StopWatch ticks = new StopWatch();
    private int ticksTwo = 0;

    @Subscribe
    private void onEvent(MoveInputEvent event) {
        ElytraJump elytraJump = Instance.get(ElytraJump.class);
        if (elytraJump != null && elytraJump.isEnabled()) {
            return;
        }

        if (mc.player != null && mc.world != null && mc.player.isGliding()) {
            ++ticksTwo;
            Vec3d pos = mc.player.getPos();
            float yaw = mc.player.getYaw();
            double forward = mc.player.age % 2 == 0 ? 0.087D : 0.09D;


            double dx = -Math.sin(Math.toRadians((double) yaw)) * forward;
            double dz = Math.cos(Math.toRadians((double) yaw)) * forward;

            mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            ticks.reset();

            if (ticks.finished(40)) {

                mc.player.setVelocity(
                        dx * (double) ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F),
                        mc.player.getVelocity().y + 0.00600000075995922D,
                        dz * (double) ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F)
                );
            }

        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate event) {
//        if (mc.player == null || mc.world == null || !mc.player.isGliding()) return;
//
//        KillAura aura = Instance.get(KillAura.class);
//        LivingEntity target = aura != null ? aura.getTarget() : null;
//
//        float yaw = target != null ? target.getYaw() : mc.player.getYaw();
//        float pitch = mc.player.age % 2 == 0 ? mc.player.getPitch() : -25.0f;
//
//        mc.player.setYaw(yaw);
//        mc.player.setPitch(pitch);
//        mc.player.headYaw = yaw;
//        mc.player.bodyYaw = yaw;
    }

    @Override
    public void onDisable() {
        ticks.reset();
        ticksTwo = 0;
        super.onDisable();
    }
}
