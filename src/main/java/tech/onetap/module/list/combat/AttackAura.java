package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import tech.onetap.Onetap;
import tech.onetap.event.EventGameUpdate;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.player.FreeCamera;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.BestPoint;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.player.combat.RaytraceUtil;
import tech.onetap.util.render.math.GCDFixer;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

import java.util.Random;

@ModuleInformation(moduleName = "AttackAura", moduleCategory = ModuleCategory.COMBAT)
public class AttackAura extends Module {
    public final SliderSetting distance = new SliderSetting("Дистанция", 3f, 2f, 5f, 0.1f);
    public final SliderSetting fov = new SliderSetting("ФОВ", 45f, 5f, 180f, 1f);
    public final SliderSetting yawSpeed = new SliderSetting("Скорость Yaw", 20f, 5f, 90f, 1f);
    public final SliderSetting pitchSpeed = new SliderSetting("Скорость Pitch", 12f, 5f, 60f, 1f);
    public final SliderSetting attackDelay = new SliderSetting("Задержка удара", 1f, 0f, 5f, 0.1f);
    public final BooleanSetting raycastCheck = new BooleanSetting("Проверка наведения", true);
    public final BooleanSetting criticals = new BooleanSetting("Только криты", false);

    private final Random random = new Random();
    @Getter
    private LivingEntity target;
    private float lastYaw;
    private float lastPitch;
    private int attackDelayTicks;

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
        attackDelayTicks = 0;
        target = null;
    }

    @Subscribe
    private void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (attackDelayTicks > 0) attackDelayTicks--;

        updateTarget();

        LivingEntity t = target;
        if (t == null || !t.isAlive()) {
            target = null;
            return;
        }

        rotateToTarget(t);
        tryAttack(t);
    }

    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (target != null && (!target.isAlive() || mc.player == null)) {
            target = null;
        }
    }

    private void updateTarget() {
        LivingEntity best = null;
        double bestScore = -1;

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double range = distance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidEntity(living)) continue;

            Vec3d nearest = BestPoint.getNearestPoint(living);
            if (nearest == null) continue;

            double dist = eye.distanceTo(nearest);
            if (dist > range) continue;

            Vec3d dir = nearest.subtract(eye).normalize();
            double dot = look.dotProduct(dir);
            if (dot <= 0.0) continue;

            double score = dot - (dist / (range * 2.0));
            if (score > bestScore) {
                bestScore = score;
                best = living;
            }
        }

        if (target == null || !isValidEntity(target)) {
            target = best;
        }
    }

    private boolean isValidEntity(LivingEntity entity) {
        if (!entity.isAlive()) return false;
        if (entity == mc.player) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;

        FreeCamera freeCamera = Onetap.getInstance().getModuleStorage().get(FreeCamera.class);
        if (freeCamera != null && entity == freeCamera.fakePlayer) return false;

        if (entity instanceof PlayerEntity p) {
            AntiBot antiBot = Onetap.getInstance().getModuleStorage().get(AntiBot.class);
            if (antiBot != null && antiBot.isBot(p)) return false;
            if (!FriendRepository.shouldAttack(p)) return false;
        }
        return true;
    }

    private void rotateToTarget(LivingEntity target) {
        Box box = target.getBoundingBox();
        double x = box.minX + box.getLengthX() * (0.5f + (random.nextFloat() - 0.5f) * 0.25f);
        double y = box.minY + box.getLengthY() * (0.55f + (random.nextFloat() - 0.5f) * 0.2f);
        double z = box.minZ + box.getLengthZ() * (0.5f + (random.nextFloat() - 0.5f) * 0.25f);

        Vec2f angles = RotationUtil.calculate(new Vec3d(x, y, z));
        float targetYaw = angles.x;
        float targetPitch = angles.y;

        float yawDelta = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - lastPitch);

        float yawFactor = 0.28f + random.nextFloat() * 0.08f;
        float pitchFactor = 0.22f + random.nextFloat() * 0.06f;

        float maxYaw = yawSpeed.getFloatValue();
        float maxPitch = pitchSpeed.getFloatValue();

        float yawStep = MathHelper.clamp(yawDelta * yawFactor, -maxYaw, maxYaw);
        float pitchStep = MathHelper.clamp(pitchDelta * pitchFactor, -maxPitch, maxPitch);

        float newYaw = lastYaw + yawStep;
        float newPitch = lastPitch + pitchStep;

        float gcd = GCDFixer.getGCDValue();
        if (gcd > 0.0f) {
            newYaw -= (newYaw - lastYaw) % gcd;
            newPitch -= (newPitch - lastPitch) % gcd;
        }
        newPitch = MathHelper.clamp(newPitch, -89.9f, 89.9f);

        Rotation smoothRotation = new Rotation(newYaw, newPitch);
        RotationComponent.update(smoothRotation, 360, 360, yawSpeed.getFloatValue(), pitchSpeed.getFloatValue(), 0, 1, true);
        lastYaw = smoothRotation.getYaw();
        lastPitch = smoothRotation.getPitch();
    }

    private void tryAttack(LivingEntity t) {
        if (attackDelayTicks > 0) return;
        if (mc.player.getAttackCooldownProgress(1.0f) < 1.0f) return;
        if (criticals.getValue() && (mc.player.isOnGround() || mc.player.fallDistance <= 0f)) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d nearest = BestPoint.getNearestPoint(t);
        if (nearest == null) return;

        double range = distance.getValue();
        if (eye.distanceTo(nearest) > range) return;

        Vec3d dir = nearest.subtract(eye).normalize();
        double dot = look.dotProduct(dir);
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
        if (angle > fov.getValue()) return;

        if (raycastCheck.getValue() && !RaytraceUtil.rayTrace(mc.player.getRotationVector(), range, t.getBoundingBox())) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackDelayTicks = (int) Math.ceil(attackDelay.getValue() + random.nextFloat() * 2f);
    }
}