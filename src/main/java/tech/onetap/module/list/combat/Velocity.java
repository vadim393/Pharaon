package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.movement.ElytraMotion;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;

@ModuleInformation(moduleName = "Velocity", moduleDesc = "Уменьшает отдачу от ударов", moduleCategory = ModuleCategory.COMBAT)
public class Velocity extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Default", "Default", "BravoHvH", "RW");
    private final SliderSetting rwHorizontal = new SliderSetting("RW Горизонталь %", 35.0, 0.0, 100.0, 1.0).setVisible(() -> mode.is("RW"));
    private final SliderSetting rwVertical = new SliderSetting("RW Вертикаль %", 100.0, 0.0, 100.0, 1.0).setVisible(() -> mode.is("RW"));
    private final BooleanSetting rwOnlyGround = new BooleanSetting("RW Только на земле", false).setVisible(() -> mode.is("RW"));
    private final BooleanSetting rwSmartReduce = new BooleanSetting("RW Умное снижение", true).setVisible(() -> mode.is("RW"));
    private int rwTicksAfterHit = 0;
    private boolean rwReceivedVelocity = false;
    private double rwStoredVelX = 0.0;
    private double rwStoredVelY = 0.0;
    private double rwStoredVelZ = 0.0;

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                AntiThorns antiThorns = AntiThorns.getInstance();
                if (antiThorns == null || !antiThorns.isEnabled() || !antiThorns.isVelocitySuppressActive()) {
                    ElytraMotion elytraMotion = Instance.get(ElytraMotion.class);
                    if (elytraMotion == null || !elytraMotion.isEnabled() || !mc.player.isGliding() || !elytraMotion.isNoKnockbackEnabled()) {
                        if (mode.is("BravoHvH")) {
                            if (!mc.player.isGliding()) {
                                if (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                                    e.cancelEvent();
                                }
                            }
                        } else if (mode.is("RW")) {
                            handleRWMode(e, packet);
                        } else {
                            e.cancelEvent();
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player != null) {
            if (mode.is("RW") && rwTicksAfterHit > 0) {
                rwTicksAfterHit--;
                if (rwTicksAfterHit == 0 && rwReceivedVelocity) {
                    applyReducedVelocity();
                    rwReceivedVelocity = false;
                }
            }
        }
    }

    private void handleRWMode(EventPacket e, EntityVelocityUpdateS2CPacket packet) {
        ElytraMotion elytraMotion = Instance.get(ElytraMotion.class);
        if (elytraMotion == null || !elytraMotion.isEnabled() || !mc.player.isGliding() || !elytraMotion.isNoKnockbackEnabled()) {
            if (!rwOnlyGround.getValue() || mc.player.isOnGround()) {
                rwStoredVelX = packet.getVelocityX() / 8000.0;
                rwStoredVelY = packet.getVelocityY() / 8000.0;
                rwStoredVelZ = packet.getVelocityZ() / 8000.0;
                e.cancelEvent();
                if (rwSmartReduce.getValue()) {
                    double horizontalVel = Math.sqrt(rwStoredVelX * rwStoredVelX + rwStoredVelZ * rwStoredVelZ);
                    if (horizontalVel > 0.5) {
                        rwReceivedVelocity = false;
                        rwTicksAfterHit = 0;
                        return;
                    }
                }

                rwReceivedVelocity = true;
                rwTicksAfterHit = 1;
            }
        }
    }

    private void applyReducedVelocity() {
        if (mc.player != null) {
            double currentVelX = mc.player.getVelocity().x;
            double currentVelY = mc.player.getVelocity().y;
            double currentVelZ = mc.player.getVelocity().z;
            double newVelX = currentVelX + rwStoredVelX * (rwHorizontal.getValue() / 100.0);
            double newVelY = currentVelY + rwStoredVelY * (rwVertical.getValue() / 100.0);
            double newVelZ = currentVelZ + rwStoredVelZ * (rwHorizontal.getValue() / 100.0);
            mc.player.setVelocity(newVelX, newVelY, newVelZ);
        }
    }

    @Override
    public void onDisable() {
        rwTicksAfterHit = 0;
        rwReceivedVelocity = false;
        rwStoredVelX = 0.0;
        rwStoredVelY = 0.0;
        rwStoredVelZ = 0.0;
        super.onDisable();
    }
}