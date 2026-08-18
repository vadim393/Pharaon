package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.chat.ChatUtil;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Well Joiner", moduleDesc = "Авто вход на гриф через компас", moduleCategory = ModuleCategory.MISC)
public class WellJoiner extends Module {

    private final SliderSetting grief = new SliderSetting("Гриф", 1, 1, 3, 1);

    private final StopWatch timer = new StopWatch();
    private boolean wasInMenu;

    @Override
    public void onEnable() {
        super.onEnable();
        wasInMenu = false;
        timer.reset();
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!timer.every(50)) return;

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            wasInMenu = true;
            int headSlot = findHeadSlot(screen, Integer.toString(grief.getIntValue()));
            if (headSlot != -1) {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, headSlot, 0, SlotActionType.PICKUP, mc.player);
            }
            return;
        }

        if (wasInMenu) {
            wasInMenu = false;
            ChatUtil.send("Успешно зашел на гриф " + grief.getIntValue());
            setEnabled(false);
            return;
        }

        int compassSlot = InventoryUtil.searchItemHotbar(Items.COMPASS);
        if (compassSlot == -1) return;

        if (mc.player.getInventory().selectedSlot != compassSlot) {
            mc.player.getInventory().selectedSlot = compassSlot;
            mc.interactionManager.syncSelectedSlot();
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
    }

    private int findHeadSlot(GenericContainerScreen screen, String griefId) {
        int containerSlots = screen.getScreenHandler().getRows() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (slot.getStack().getItem() != Items.PLAYER_HEAD) continue;

            String itemName = slot.getStack().getName().getString();
            if (itemName.contains(griefId)) {
                return i;
            }
        }
        return -1;
    }
}