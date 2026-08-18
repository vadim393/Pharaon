package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import tech.onetap.event.list.EventNoPush;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;

@ModuleInformation(
        moduleName = "No Push",
        moduleDesc = "Позволяет не отталкиваться при выбранных условиях",
        moduleCategory = ModuleCategory.MOVEMENT
)
public class NoPush extends Module {
    public final ModeListSetting mode = new ModeListSetting("Тип",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Блоки", true),
            new BooleanSetting("Вода", true),
            new BooleanSetting("Удочки", true)
    );

    @Subscribe
    private void onEvent(EventNoPush event) {
        boolean cancel = switch (event.getNoPushType()) {
            case Block -> mode.isEnabled("Блоки");
            case Water -> mode.isEnabled("Вода");
            case Player -> mode.isEnabled("Игроки");
            case FishingRod -> mode.isEnabled("Удочки");
        };

        if (cancel) {
            event.setCancelled(true);
        }
    }
}
