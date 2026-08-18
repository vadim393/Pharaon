package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.move.MoveUtil;
import tech.onetap.util.time.TimerManager;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInformation(moduleName = "Speed", moduleCategory = ModuleCategory.MOVEMENT)
public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Цель", "Цель", "RWnew");
    private final ModeSetting rwMode = new ModeSetting("Режим", "Обычный", "Обычный", "Быстрый").setVisible(() -> mode.is("RWnew"));

    private final SliderSetting boost = new SliderSetting("Сила буста", 8.0f, 1.0f, 20.0f, 0.1f).setVisible(() -> mode.is("Цель"));
    private final SliderSetting targetRange = new SliderSetting("Радиус цели", 3.0f, 0.5f, 10.0f, 0.1f).setVisible(() -> mode.is("Цель"));
    private final SliderSetting contactRange = new SliderSetting("Радиус контакта", 0.5f, 0.1f, 2.0f, 0.1f).setVisible(() -> mode.is("Цель"));

    private final BooleanSetting playersOnly = new BooleanSetting("Только игроки", true).setVisible(() -> mode.is("Цель"));
    private final BooleanSetting onlyWhileMoving = new BooleanSetting("Только в движении", true).setVisible(() -> mode.is("Цель"));
    private final BooleanSetting onlyWithAura = new BooleanSetting("Только с Aura", false).setVisible(() -> mode.is("Цель"));

    private final BooleanSetting predict = new BooleanSetting("Предикт", true).setVisible(() -> mode.is("Цель"));
    private final SliderSetting predictStrength = new SliderSetting("Сила предикта", 2.0f, 0.1f, 10.0f, 0.1f).setVisible(() -> mode.is("Цель") && predict.getValue());

    private static final float CHARGE_TIMER = 0.05F;
    private static final float BOOST_TIMER = 1.7F;
    private static final long CHARGE_DURATION_NANOS = 1_250_000_000L;
    private static final long MAX_BOOST_DURATION_NANOS = 2_400_000_000L;
    private static final long FAST_MAX_BOOST_DURATION_NANOS = 3_200_000_000L;
    private static final int FULL_BOOST_JUMPS = 4;
    private static final int FAST_FULL_BOOST_JUMPS = 5;

    private boolean isFast() {
        return mode.is("RWnew") && rwMode.is("Быстрый");
    }

    private long getMaxBoostDurationNanos() {
        return isFast() ? FAST_MAX_BOOST_DURATION_NANOS : MAX_BOOST_DURATION_NANOS;
    }

    private int getFullBoostJumps() {
        return isFast() ? FAST_FULL_BOOST_JUMPS : FULL_BOOST_JUMPS;
    }

    private Phase phase = Phase.CHARGING;
    private boolean cycleActive;
    private boolean airborne;
    private int completedJumps;
    private int groundTicks;
    private long phaseStartedAt;
    private final Queue<CommonPongC2SPacket> delayedTransactions = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        super.onEnable();
        stopCycle();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopCycle();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (!mode.is("RWnew")) {
            stopCycle();
            if (mc.player == null || mc.world == null) return;

            if (onlyWithAura.getValue()) {
                KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
                if (aura == null || !aura.isEnabled() || aura.getTarget() == null) return;
            }

            Box contactBox = mc.player.getBoundingBox().expand(contactRange.getValue());
            int contactCount = 0;

            for (Entity entity : mc.world.getEntities()) {
                if (!isValidTarget(entity)) continue;
                if (contactBox.intersects(entity.getBoundingBox())) contactCount++;
            }

            if (contactCount <= 0) return;
            if (onlyWhileMoving.getValue() && !MoveUtil.hasPlayerMovement()) return;

            double motionBoost = boost.getValue() * 0.01 * contactCount;
            if (motionBoost <= 0.0) return;

            Entity nearest = findNearestTarget(targetRange.getValue());
            if (nearest == null) return;

            Vec3d targetPos = nearest.getPos();
            if (predict.getValue()) {
                Vec3d targetMotion = nearest.getVelocity();
                double horizontalMotionSq = targetMotion.x * targetMotion.x + targetMotion.z * targetMotion.z;
                if (horizontalMotionSq > 1.0E-4) {
                    targetPos = targetPos.add(targetMotion.x * predictStrength.getValue(), 0.0, targetMotion.z * predictStrength.getValue());
                }
            }

            double[] direction = getDirectionToPoint(mc.player.getPos(), targetPos, motionBoost);
            mc.player.addVelocity(direction[0], 0.0, direction[1]);
            return;
        }

        if (mc.player == null || mc.world == null) {
            stopCycle();
            return;
        }

        if (phase == Phase.BOOSTING && isMoving(mc.player)) {
            mc.player.setSprinting(true);
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate ignored) {
        if (!mode.is("RWnew") || mc.player == null || !cycleActive || phase != Phase.BOOSTING) return;

        if (mc.player.verticalCollision) {
            if (airborne) {
                airborne = false;
                completedJumps++;
            }
        } else {
            airborne = true;
            if (completedJumps >= getFullBoostJumps() && mc.player.getVelocity().y <= 0.0D) {
                beginCharging();
            }
        }
    }

    @Subscribe
    private void onWorldRender(EventWorldRender ignored) {
        if (!mode.is("RWnew")) return;

        if (mc.player == null || !isMoving(mc.player)) {
            stopCycle();
            return;
        }

        long now = System.nanoTime();
        if (!cycleActive) {
            cycleActive = true;
            beginCharging();
            return;
        }

        if (phase == Phase.CHARGING && now - phaseStartedAt >= CHARGE_DURATION_NANOS) {
            beginBoosting();
        } else if (phase == Phase.BOOSTING && now - phaseStartedAt >= getMaxBoostDurationNanos()) {
            beginCharging();
        }
    }

    @Subscribe
    private void onMoveInput(MoveInputEvent event) {
        if (!mode.is("RWnew") || mc.player == null || phase != Phase.BOOSTING || !isMoving(event)) {
            groundTicks = 0;
            return;
        }

        groundTicks = mc.player.verticalCollision ? groundTicks + 1 : 0;
        mc.player.setSprinting(true);
        if (groundTicks > 0) {
            event.jump = true;
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (!mode.is("RWnew") || !cycleActive) return;

        if (event.getType() == EventPacket.Type.SEND && event.getPacket() instanceof CommonPongC2SPacket packet) {
            delayedTransactions.add(packet);
            event.cancelEvent();
            return;
        }

        if (event.getType() == EventPacket.Type.RECEIVE && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            beginCharging();
        }
    }

    private boolean isMoving(net.minecraft.client.network.ClientPlayerEntity player) {
        var input = player.input.playerInput;
        return input.forward() || input.backward() || input.left() || input.right();
    }

    private boolean isMoving(MoveInputEvent event) {
        return event.getForward() != 0.0f || event.getStrafe() != 0.0f;
    }

    private void beginCharging() {
        flushTransactions();
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        TimerManager.setTimer(CHARGE_TIMER);
    }

    private void beginBoosting() {
        phase = Phase.BOOSTING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        TimerManager.setTimer(BOOST_TIMER);
    }

    private void stopCycle() {
        flushTransactions();
        cycleActive = false;
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = 0L;
        TimerManager.reset();
    }

    private void flushTransactions() {
        CommonPongC2SPacket packet;
        while ((packet = delayedTransactions.poll()) != null) {
            NetworkUtils.sendSilentPacket(packet);
        }
    }

    private Entity findNearestTarget(double maxRange) {
        Entity nearest = null;
        double bestDistanceSq = Double.MAX_VALUE;
        double maxDistanceSq = maxRange * maxRange;

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;

            double dx = entity.getX() - mc.player.getX();
            double dz = entity.getZ() - mc.player.getZ();
            double distanceSq = dx * dx + dz * dz;

            if (distanceSq <= maxDistanceSq && distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                nearest = entity;
            }
        }

        return nearest;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;
        if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) return false;
        return entity instanceof LivingEntity || entity instanceof BoatEntity;
    }

    private double[] getDirectionToPoint(Vec3d from, Vec3d to, double speedValue) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6) return new double[]{0.0, 0.0};
        return new double[]{dx / length * speedValue, dz / length * speedValue};
    }

    private enum Phase {
        CHARGING,
        BOOSTING
    }
}