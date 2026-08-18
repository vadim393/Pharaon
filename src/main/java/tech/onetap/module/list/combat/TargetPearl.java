package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ModuleInformation(moduleName = "TargetPearl", moduleDesc = "Таргет Перл", moduleCategory = ModuleCategory.COMBAT)
public class TargetPearl extends Module {
    private final Set<UUID> seenPearls = new HashSet<>();

    private final SliderSetting radius = new SliderSetting("Радиус", 30.0f, 5.0f, 100.0f, 1.0f);
    private final SliderSetting delay = new SliderSetting("Тики", 10.0f, 1.0f, 40.0f, 1.0f);

    private int lastThrowTick = 0;
    private int throwStage = 0;
    private int pearlSlot = -1;
    private int oldSlot = -1;
    private float targetYaw = 0f;
    private float targetPitch = 0f;

    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        lastThrowTick++;

        if (throwStage > 0) {
            handleThrowSequence();
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl) {
                UUID pearlId = pearl.getUuid();
                if (seenPearls.contains(pearlId)) continue;
                if (pearl.age > 2) continue;

                Entity owner = pearl.getOwner();
                if (owner instanceof PlayerEntity player && player != mc.player) {
                    if (mc.player.distanceTo(player) > radius.getValue()) continue;
                    if (lastThrowTick < delay.getValue()) continue;

                    int slot = findPearlSlot();
                    if (slot == -1) continue;

                    Vec3d predictedLandingPos = predictPearlLanding(pearl);
                    float[] angles = calculateThrowAngles(predictedLandingPos);

                    this.pearlSlot = slot;
                    this.oldSlot = mc.player.getInventory().selectedSlot;
                    this.targetYaw = angles[0];
                    this.targetPitch = angles[1];

                    throwStage = 1;
                    lastThrowTick = 0;
                    seenPearls.add(pearlId);
                    break;
                }
            }
        }
    }

    private void handleThrowSequence() {
        switch (throwStage) {
            case 1:
                mc.player.getInventory().selectedSlot = pearlSlot;
                mc.player.setYaw(targetYaw);
                mc.player.setPitch(targetPitch);
                throwStage = 2;
                break;
            case 2:
                ItemStack stack = mc.player.getMainHandStack();
                if (stack.getItem() instanceof EnderPearlItem) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                throwStage = 3;
                break;
            case 3:
                mc.player.getInventory().selectedSlot = oldSlot;
                resetThrowState();
                break;
        }
    }

    private Vec3d predictPearlLanding(EnderPearlEntity pearl) {
        Vec3d pos = pearl.getPos();
        Vec3d vel = pearl.getVelocity();

        for (int i = 0; i < 200; i++) {
            Vec3d nextPos = pos.add(vel);
            HitResult hit = mc.world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    pearl
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos();
            }
            pos = nextPos;
            vel = new Vec3d(vel.x * 0.99, vel.y * 0.99 - 0.03, vel.z * 0.99);
        }
        return pos;
    }

    private float[] calculateThrowAngles(Vec3d targetLandPos) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = targetLandPos.x - eyePos.x;
        double dz = targetLandPos.z - eyePos.z;
        double dy = targetLandPos.y - eyePos.y;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float directPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        float bestPitch = directPitch;
        double minDiff = Double.MAX_VALUE;

        for (float pitch = directPitch; pitch >= -85.0f && pitch >= directPitch - 50.0f; pitch -= 0.5f) {
            Vec3d land = simulatePlayerThrow(eyePos, yaw, pitch);
            double dist = land.distanceTo(targetLandPos);
            if (dist < minDiff) {
                minDiff = dist;
                bestPitch = pitch;
            }
        }
        return new float[]{yaw, bestPitch};
    }

    private Vec3d simulatePlayerThrow(Vec3d eyePos, float yaw, float pitch) {
        double f = 1.5;
        double vx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f;
        double vy = -Math.sin(Math.toRadians(pitch)) * f;
        double vz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f;
        Vec3d vel = new Vec3d(vx, vy, vz);
        Vec3d pos = eyePos;

        for (int i = 0; i < 200; i++) {
            Vec3d nextPos = pos.add(vel);
            HitResult hit = mc.world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos();
            }
            pos = nextPos;
            vel = new Vec3d(vel.x * 0.99, vel.y * 0.99 - 0.03, vel.z * 0.99);
        }
        return pos;
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof EnderPearlItem) {
                return i;
            }
        }
        return -1;
    }

    private void resetThrowState() {
        throwStage = 0;
        pearlSlot = -1;
        oldSlot = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        seenPearls.clear();
        lastThrowTick = 0;
        resetThrowState();
    }
}
