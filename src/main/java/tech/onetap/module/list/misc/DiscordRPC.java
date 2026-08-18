package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

@ModuleInformation(moduleName = "Discord RPC", moduleDesc = "Discord Rich Presence", moduleCategory = ModuleCategory.MISC)
public class DiscordRPC extends Module {

    private static final String APPLICATION_ID = "1538545323092344852";

    private DiscordRpc rpc;
    private boolean connected;
    private long sessionStartTimestamp;

    @Override
    public void onEnable() {
        super.onEnable();

        rpc = new DiscordRpc();
        connected = false;
        sessionStartTimestamp = System.currentTimeMillis() / 1000L;

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                connected = true;
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                connected = false;
            }
        };

        try {
            rpc.init(APPLICATION_ID, handler, false);
        } catch (Exception ignored) {
            connected = false;
        }
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (rpc == null || !connected) return;
        try {
            String details;
            String state;
            if (mc.player == null) {
                details = "";
                state = "";
            } else if (mc.currentScreen instanceof TitleScreen || mc.currentScreen instanceof MultiplayerScreen || mc.currentScreen instanceof OptionsScreen) {
                details = "";
                state = "";
            } else {
                details = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null
                        ? mc.getCurrentServerEntry().address
                        : "";
                state = "";
            }

            DiscordRichPresence presence = DiscordRichPresence.builder()
                    .activityType(ActivityType.PLAYING)
                    .name("")
                    .details(details)
                    .state(state)
                    .startTimestamp(sessionStartTimestamp)
                    .build();
            rpc.updatePresence(presence);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rpc != null) {
            try {
                rpc.shutdown();
            } catch (Exception ignored) {
            }
            rpc = null;
        }
        connected = false;
    }
}

