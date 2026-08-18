package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerSync;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.move.MoveUtil;

@ModuleInformation(moduleName = "Glide Fly", moduleDesc = "Объединённые режимы полёта на элитре", moduleCategory = ModuleCategory.MOVEMENT)
public class GlideFly extends Module {
   private final ModeSetting mode = new ModeSetting("Режим", "Grim Exploit", "Grim Exploit", "Grim Exploit 2", "Grim Glide", "NosoGlide");
   private final SliderSetting hBoost = new SliderSetting("Горизонт. буст", 0.085, 0.01, 0.3, 0.005).setVisible(() -> this.mode.is("Grim Exploit"));
   private final SliderSetting vBoost = new SliderSetting("Верт. буст", 0.03, 0.01, 0.1, 0.005).setVisible(() -> this.mode.is("Grim Exploit"));
   private final SliderSetting delay = new SliderSetting("Задержка", 150.0, 50.0, 500.0, 10.0).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting forwardEven = new SliderSetting("Форвард чётный", 0.079, 0.01, 0.3, 0.001).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting forwardOdd = new SliderSetting("Форвард нечётный", 0.088, 0.01, 0.3, 0.001).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting stopNearTarget = new SliderSetting("Стоп у цели", 1.0E-6, 0.0, 0.01, 1.0E-7).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting stopNearTarget2 = new SliderSetting("Стоп у цели 2", 1.0E-6, 0.0, 0.01, 1.0E-7).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting angleThreshold = new SliderSetting("Угол стопа", 0.35, 0.0, 1.0, 0.01).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final BooleanSetting posCorrection = new BooleanSetting("Коррекция позиции", true).setVisible(() -> this.mode.is("Grim Exploit 2"));
   private final SliderSetting maxSpeed = new SliderSetting("Макс скорость", 48.0, 30.0, 60.0, 1.0).setVisible(() -> this.mode.is("Grim Glide"));
   public final BooleanSetting enhancedBPS = new BooleanSetting("Улучшенный БПС", false);
   private final StopWatch timer = new StopWatch();
   private final StopWatch ticks = new StopWatch();
   private int ticksCount = 0;
   private int ticksTwo = 0;
   private int groundTicks = 0;
   private Vec3d lastPos = null;
   private final List<GlideFly.BPSRecord> bpsHistory = new ArrayList<>();
   private double maxBPS = 0.0;

   @Subscribe
   private void onUpdateGrimExploit(EventPlayerUpdate e) {
      if (this.mode.is("Grim Exploit")) {
         if (this.mc.player != null) {
            if (this.ticksCount > 3 && this.ticksCount % 2 == 0) {
               Vec3d vel = this.mc.player.getVelocity();
               this.mc.player.setVelocity(vel.x, vel.y + this.vBoost.getValue(), vel.z);
               double bst = this.mc.player.isOnGround() ? this.hBoost.getValue() : this.vBoost.getValue();
               double[] dir = MoveUtil.calculateDirection(bst);
               double xt = MoveUtil.hasPlayerMovement() ? dir[0] : 0.0;
               double zt = MoveUtil.hasPlayerMovement() ? dir[1] : 0.0;
               Vec3d v = this.mc.player.getVelocity();
               this.mc.player.setVelocity(v.x + xt, v.y, v.z + zt);
            }

            this.ticksCount++;
         }
      }
   }

   @Subscribe
   private void onInputGrimExploit(MoveInputEvent e) {
      if (this.mode.is("Grim Exploit")) {
         if (this.mc.player != null) {
            if (this.mc.player.verticalCollision) {
               this.groundTicks++;
            } else {
               this.groundTicks = 0;
            }

            if (this.groundTicks >= 1) {
               e.jump = true;
            }
         }
      }
   }

   @Subscribe
   private void onPostMotionGrimExploit(EventPlayerSync e) {
      if (this.mode.is("Grim Exploit")) {
         if (this.mc.player != null) {
            if (this.ticksCount % 2 == 0) {
               NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
            }
         }
      }
   }

   @Subscribe
   private void onPacketGrimExploit(EventPacket e) {
      if (this.mode.is("Grim Exploit")) {
         if (this.mc.player != null) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
               if (this.ticksCount % 2 == 1) {
                  this.ticksCount++;
               }
            }
         }
      }
   }

   @Subscribe
   public void onSyncGlidyFly(EventPlayerSync event) {
      if (this.mode.is("Grim Exploit 2")) {
         if (this.mc.player != null && this.mc.world != null) {
            if (this.mc.player.isGliding()) {
               if (!(this.timer.getTime() <= this.delay.getValue())) {
                  if (!this.shouldStop()) {
                     this.timer.reset();
                     Vec3d pos = this.mc.player.getPos();
                     float yaw = this.mc.player.getYaw();
                     boolean even = this.mc.player.age % 2 == 0;
                     double forward = even ? this.forwardEven.getValue() : this.forwardOdd.getValue();
                     double dx = -Math.sin(Math.toRadians(yaw)) * forward;
                     double dz = Math.cos(Math.toRadians(yaw)) * forward;
                     this.mc.player.setVelocity(dx, this.mc.player.getVelocity().y, dz);
                     if (this.posCorrection.getValue() && even) {
                        this.mc.player.setPosition(pos.x + dx, pos.y, pos.z + dz);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldStop() {
      KillAura ka = Instance.get(KillAura.class);
      if (ka != null && ka.isEnabled() && ka.getTarget() != null) {
         LivingEntity target = ka.getTarget();
         if (!target.isAlive()) {
            return false;
         }

         Vec3d targetVel = target.getVelocity();
         Vec3d relVel = new Vec3d(targetVel.x, 0.0, targetVel.z);
         double velLen = relVel.length();
         if (velLen < this.stopNearTarget.getValue()) {
            relVel = new Vec3d(target.getVelocity().x, 0.0, target.getVelocity().z);
         }

         if (relVel.length() < this.stopNearTarget2.getValue()) {
            return false;
         }

         Vec3d toTarget = new Vec3d(this.mc.player.getX() - target.getX(), 0.0, this.mc.player.getZ() - target.getZ());
         Vec3d relVelNorm = relVel.normalize();
         Vec3d toTargetNorm = toTarget.normalize();
         return toTargetNorm.dotProduct(relVelNorm) > this.angleThreshold.getValue();
      } else {
         return false;
      }
   }

   @Subscribe
   public void onEventGrimGlide(EventPlayerSync event) {
      if (this.mode.is("Grim Glide")) {
         if (this.mc.player != null && this.mc.world != null && this.mc.player.isGliding()) {
            this.ticksTwo++;
            Vec3d pos = this.mc.player.getPos();
            float yaw = this.mc.player.getYaw();
            double forward = 0.087;
            double motion = Math.hypot(this.mc.player.prevX - this.mc.player.getX(), this.mc.player.prevZ - this.mc.player.getZ()) * 20.0;
            if (motion >= this.maxSpeed.getValue()) {
               forward = 0.0;
               motion = 0.0;
            }

            double dx = -Math.sin(Math.toRadians(yaw)) * forward;
            double dz = Math.cos(Math.toRadians(yaw)) * forward;
            Vec3d newVel = new Vec3d(
               dx * ThreadLocalRandom.current().nextFloat(1.1F, 1.21F),
               this.mc.player.getVelocity().y - 0.02,
               dz * ThreadLocalRandom.current().nextFloat(1.1F, 1.21F)
            );
            newVel = this.applyAntiStopLimit(newVel);
            this.mc.player.setVelocity(newVel.x, newVel.y, newVel.z);
            if (this.ticks.isReached(50L)) {
               this.mc.player.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
               this.ticks.reset();
            }

            Vec3d finalVel = new Vec3d(
               dx * ThreadLocalRandom.current().nextFloat(1.1F, 1.21F),
               this.mc.player.getVelocity().y + 0.016,
               dz * ThreadLocalRandom.current().nextFloat(1.1F, 1.21F)
            );
            finalVel = this.applyAntiStopLimit(finalVel);
            this.mc.player.setVelocity(finalVel.x, finalVel.y, finalVel.z);
         }
      }
   }

   @Subscribe
   private void onTick(EventTick e) {
      if (this.enhancedBPS.getValue()) {
         if (this.mc.player != null) {
            this.updateBPSHistory();
         }
      }
   }

   private void updateBPSHistory() {
      Vec3d currentPos = this.mc.player.getPos();
      if (this.lastPos != null) {
         double distance = Math.sqrt(
            Math.pow(currentPos.x - this.lastPos.x, 2.0) + Math.pow(currentPos.y - this.lastPos.y, 2.0) + Math.pow(currentPos.z - this.lastPos.z, 2.0)
         );
         double currentBPS = distance * 20.0;
         this.bpsHistory.add(new GlideFly.BPSRecord(currentBPS, System.currentTimeMillis()));
      }

      this.lastPos = currentPos;
      Iterator<GlideFly.BPSRecord> iterator = this.bpsHistory.iterator();

      while (iterator.hasNext()) {
         GlideFly.BPSRecord record = iterator.next();
         if (record.isExpired()) {
            iterator.remove();
         }
      }

      this.maxBPS = this.bpsHistory.stream().mapToDouble(recordx -> recordx.bps).max().orElse(0.0);
   }

   public double getMaxBPS() {
      return this.maxBPS;
   }

   public boolean isEnhancedBPSEnabled() {
      return this.isEnabled() && this.enhancedBPS.getValue();
   }

   @Subscribe
   private void onEventNosoGlide(MoveInputEvent event) {
      if (this.mode.is("NosoGlide")) {
         if (this.mc.player != null && this.mc.world != null) {
            if (this.mc.player.isGliding()) {
               this.ticksTwo++;
               Vec3d pos = this.mc.player.getPos();
               float yaw = this.mc.player.getYaw();
               double forward = this.mc.player.age % 2 == 0 ? 0.087 : 0.09;
               double dx = -Math.sin(Math.toRadians(yaw)) * forward;
               double dz = Math.cos(Math.toRadians(yaw)) * forward;
               this.mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
               this.ticks.reset();
               if (this.ticks.finished(40.0)) {
                  Vec3d newVel = new Vec3d(
                     dx * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F),
                     this.mc.player.getVelocity().y + 0.006000000759959221,
                     dz * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F)
                  );
                  newVel = this.applyAntiStopLimit(newVel);
                  this.mc.player.setVelocity(newVel.x, newVel.y, newVel.z);
               }
            }
         }
      }
   }

   private Vec3d applyAntiStopLimit(Vec3d velocity) {
      AntiStop antiStop = Instance.get(AntiStop.class);
      if (antiStop != null && antiStop.isEnabled() && antiStop.isBpsLimitEnabled()) {
         double maxBPS = antiStop.getMaxBPS();
         boolean includeY = antiStop.isIncludeVertical();
         double currentBPS;
         if (includeY) {
            currentBPS = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z) * 20.0;
         } else {
            currentBPS = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
         }

         if (currentBPS > maxBPS) {
            double smoothFactor = antiStop.getSmoothnessMultiplier();
            return velocity.multiply(smoothFactor);
         }
      }

      return velocity;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.timer.reset();
      this.ticks.reset();
      this.ticksCount = 0;
      this.ticksTwo = 0;
      this.groundTicks = 0;
      this.bpsHistory.clear();
      this.maxBPS = 0.0;
      this.lastPos = null;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.ticksCount = 0;
      this.ticksTwo = 0;
      this.groundTicks = 0;
      this.bpsHistory.clear();
      this.maxBPS = 0.0;
      this.lastPos = null;
   }

   private static class BPSRecord {
      final double bps;
      final long timestamp;

      BPSRecord(double bps, long timestamp) {
         this.bps = bps;
         this.timestamp = timestamp;
      }

      boolean isExpired() {
         return System.currentTimeMillis() - this.timestamp > 3000L;
      }
   }
}
