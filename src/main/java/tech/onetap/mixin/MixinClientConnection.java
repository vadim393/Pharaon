package tech.onetap.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventPacket;
import tech.onetap.module.list.player.RPSpoofer;
import tech.onetap.util.bot.BotSessionManager;
import tech.onetap.util.packet.NetworkUtils;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void send(Packet<?> packet, CallbackInfo ci) {
        ClientConnection connection = (ClientConnection) (Object) this;
        if (BotSessionManager.isBotConnection(connection)) {
            return;
        }
        if (NetworkUtils.getSilentPackets().contains(packet)) {
            NetworkUtils.getSilentPackets().remove(packet);
            return;
        }
        EventPacket event = new EventPacket(packet, EventPacket.Type.SEND);
        Onetap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        ClientConnection connection = (ClientConnection) (Object) this;
        boolean botConnection = BotSessionManager.isBotConnection(connection);
        RPSpoofer rpSpoofer = Onetap.getInstance().getModuleStorage().get(RPSpoofer.class);
        boolean spoofBecauseBotsActive = BotSessionManager.hasBackgroundSessions();

        if (packet instanceof ResourcePackSendS2CPacket resourcePack) {
            boolean spoofByModule = rpSpoofer != null && rpSpoofer.isEnabled();
            if (spoofByModule || botConnection || spoofBecauseBotsActive) {
                if (spoofBecauseBotsActive) {
                    BotSessionManager.clearServerResourcePacks();
                }
                BotSessionManager.acknowledgeResourcePack(connection, resourcePack);
                ci.cancel();
                return;
            }
        }

        if (botConnection) {
            // Safety net: background bot packets must never hit normal packet listeners.
            ci.cancel();
            return;
        }

        EventPacket event = new EventPacket(packet, EventPacket.Type.RECEIVE);
        Onetap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
