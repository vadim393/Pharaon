package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.math.StopWatch;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "InvSaver", moduleDesc = "Быстрый сейв инвентаря через хаб", moduleCategory = ModuleCategory.MISC)
public class InvSaver extends Module {
    private static final long WHITE_RISE_MESSAGE_DELAY_MS = 60_000L;

    private final ModeSetting mode = new ModeSetting("Режим", "Well", "Well", "WhiteRise");
    private final SliderSetting grief = new SliderSetting("Гриф", 1, 1, 3, 1);
    private final StringSetting warpName = new StringSetting("Варп", "demaz", 30);

    private final StopWatch tickTimer = new StopWatch();
    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch spamTimer = new StopWatch();
    private State state;

    private static final String[] MESSAGES = {
            "Лан бань, я с клиентом",
            "Ну ок, бань если хочешь",
            "Лан откинь, я с софтом",
            "Да, я с читом, делай что должен",
            "Ну че, оформляй бан"
    };

    @Override
    public void onEnable() {
        super.onEnable();
        tickTimer.reset();
        stateTimer.reset();
        spamTimer.reset();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (mode.is("Well")) {
            state = State.WAITING_FOR_JOIN;
            sendRandomMessage(true);
            mc.player.networkHandler.sendChatCommand("hub");
        } else {
            state = State.SPAMMING;
            sendRandomMessage(false);
            spamTimer.reset();
        }
    }

    private void sendRandomMessage(boolean appendRandomSuffix) {
        if (mc.player != null && mc.player.networkHandler != null) {
            String msg = MESSAGES[ThreadLocalRandom.current().nextInt(MESSAGES.length)];

            if (appendRandomSuffix) {
                msg += " " + ThreadLocalRandom.current().nextInt(100000);
            }
            mc.player.networkHandler.sendChatMessage(msg);
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        
        if (state == State.SPAMMING && spamTimer.isReached(WHITE_RISE_MESSAGE_DELAY_MS)) {
            sendRandomMessage(false);
            spamTimer.reset();
        }

        if (!tickTimer.every(500)) return;

        WellJoiner wellJoiner = Instance.get(WellJoiner.class);

        switch (state) {
            case SPAMMING -> {}
            case WAITING_FOR_JOIN -> {
                if (wellJoiner != null && !wellJoiner.isEnabled()) {
                    applyWellJoinerGrief(wellJoiner, grief.getIntValue());
                    wellJoiner.setEnabled(true);
                }
                state = State.JOINING_GRIEF;
            }
            case JOINING_GRIEF -> {
                if (wellJoiner != null && !wellJoiner.isEnabled()) {
                    mc.getNetworkHandler().sendChatCommand("warp " + getWarpTarget());
                    state = State.WAITING_WARP;
                    tickTimer.reset();
                    stateTimer.reset();
                }
            }
            case WAITING_WARP -> {
                if (stateTimer.getTime() >= 250) {
                    dropInventory();
                    setEnabled(false);
                }
            }
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (state == State.SPAMMING && e.getType() == EventPacket.Type.RECEIVE) {
            if (e.getPacket() instanceof GameMessageS2CPacket p) {
                String text = p.content().getString();
                if (text.contains("Kicked for spamming")) {
                    state = State.WAITING_FOR_JOIN;
                }
            }
        }
    }

    private void applyWellJoinerGrief(WellJoiner wellJoiner, int griefId) {
        for (Setting setting : wellJoiner.getSettings()) {
            if (setting instanceof SliderSetting slider && "Гриф".equals(slider.getName())) {
                slider.setValue(griefId);
                return;
            }
        }
    }

    private void dropInventory() {
        int syncId = mc.player.currentScreenHandler.syncId;
        for (int slotId = 9; slotId <= 45; slotId++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
            if (slot == null || !slot.hasStack()) continue;
            mc.interactionManager.clickSlot(syncId, slotId, 1, SlotActionType.THROW, mc.player);
        }
    }

    public String getWarpTarget() {
        return normalizeWarpTarget(warpName.getValue());
    }

    public void setWarpTarget(String value) {
        warpName.setValue(normalizeWarpTarget(value));
    }

    private String normalizeWarpTarget(String value) {
        if (value == null) return "demaz";

        String normalized = value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("warp ")) {
            normalized = normalized.substring(5).trim();
        }

        return normalized.isEmpty() ? "demaz" : normalized;
    }

    private enum State {
        SPAMMING,
        WAITING_FOR_JOIN,
        JOINING_GRIEF,
        WAITING_WARP
    }
}
