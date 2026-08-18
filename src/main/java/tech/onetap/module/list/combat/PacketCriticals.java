package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import tech.onetap.event.list.EventAttack;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.player.other.WorldUtils;

@ModuleInformation(moduleName = "Packet Criticals", moduleCategory = ModuleCategory.COMBAT)
public class PacketCriticals extends Module {

    @Subscribe
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;

        boolean inWeb = WorldUtils.isInWeb();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (inWeb) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.00300, z, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
        }
    }
}