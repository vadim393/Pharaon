package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.Hand;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.bot.BotSessionManager;

@ModuleInformation(
        moduleName = "Tape Mouse",
        moduleDesc = "Автоматические клики для фоновых ботов",
        moduleCategory = ModuleCategory.PLAYER
)
public class TapeMouse extends Module {
    private final SliderSetting clickDelay = new SliderSetting("Задержка клика", 100, 10, 1000, 5);
    private final ModeSetting clickButton = new ModeSetting("Кнопка", "Левая", "Левая", "Правая");
    private final BooleanSetting onlyBots = new BooleanSetting("Только боты", true);

    private long lastClick;

    @Subscribe
    private void onTick(EventTick eventTick) {
        AutoEat autoEat = Instance.get(AutoEat.class);
        if (autoEat != null && autoEat.shouldPauseTapeMouse()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClick < clickDelay.getIntValue()) {
            return;
        }

        if (!onlyBots.getValue()) {
            clickForLocalPlayer();
        }

        if (!BotSessionManager.getConnections().isEmpty()) {
            BotSessionManager.pulseBots(isRightClick());
        }

        lastClick = now;
    }

    private void clickForLocalPlayer() {
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        if (isRightClick()) {
            if (mc.interactionManager != null) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        } else {
            mc.doAttack();
        }
    }

    private boolean isRightClick() {
        return clickButton.getIndex() == 1;
    }
}
