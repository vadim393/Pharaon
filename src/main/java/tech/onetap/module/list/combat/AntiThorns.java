package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "Anti Thorns", moduleDesc = "Блокирует урон от шипов", moduleCategory = ModuleCategory.COMBAT)
public class AntiThorns extends Module {
    private final BooleanSetting velocitySuppress = new BooleanSetting("Подавление отдачи", true);
    private static AntiThorns instance;

    public AntiThorns() {
        instance = this;
    }

    public static AntiThorns getInstance() {
        return instance;
    }

    public boolean isVelocitySuppressActive() {
        return this.isEnabled() && this.velocitySuppress.getValue();
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof DamageTiltS2CPacket packet) {
            if (this.mc.player != null && packet.id() == this.mc.player.getId()) {
                e.cancelEvent();
            }
        }

        if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (this.mc.player != null && packet.getEntity(this.mc.world) == this.mc.player && packet.getStatus() == 33) {
                e.cancelEvent();
            }
        }
    }
}
