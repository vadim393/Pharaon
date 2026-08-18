package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Auto Eat", moduleDesc = "Автоматически ест когда голод ниже порога", moduleCategory = ModuleCategory.PLAYER)
public class AutoEat extends Module {

    private final SliderSetting hungerThreshold = new SliderSetting("Порог голода", 14, 1, 20, 1);
    private final BooleanSetting stopTapeMouse = new BooleanSetting("Останавливать TapeMouse", true);

    private boolean isEating;
    private int previousSlot = -1;
    private int eatingHotbarSlot = -1;
    private int swappedInventorySlot = -1;

    public boolean shouldPauseTapeMouse() {
        return stopTapeMouse.getValue() && isEating;
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            stopEating();
            return;
        }

        if (mc.currentScreen != null) {
            stopEating();
            return;
        }

        if (!needsToEat()) {
            stopEating();
            return;
        }

        if (isEating) {
            continueEating();
            return;
        }

        int foodSlot = findFoodSlot();
        if (foodSlot == -1) {
            stopEating();
            return;
        }

        startEating(foodSlot);
    }

    private boolean needsToEat() {
        return mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.getValue();
    }

    private int findFoodSlot() {
        int hotbarSlot = findFoodSlotInRange(0, 9);
        if (hotbarSlot != -1) {
            return hotbarSlot;
        }

        return findFoodSlotInRange(9, 36);
    }

    private int findFoodSlotInRange(int start, int end) {
        for (int slot = start; slot < end; slot++) {
            if (isFoodStack(mc.player.getInventory().getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void startEating(int foodSlot) {
        previousSlot = mc.player.getInventory().selectedSlot;
        eatingHotbarSlot = foodSlot <= 8 ? foodSlot : previousSlot;
        swappedInventorySlot = -1;

        if (foodSlot <= 8) {
            if (previousSlot != foodSlot) {
                mc.player.getInventory().selectedSlot = foodSlot;
                mc.interactionManager.syncSelectedSlot();
            }
        } else {
            swappedInventorySlot = foodSlot;
            mc.interactionManager.clickSlot(0, foodSlot, eatingHotbarSlot, SlotActionType.SWAP, mc.player);
        }

        isEating = true;
        mc.options.useKey.setPressed(true);
    }

    private void continueEating() {
        if (eatingHotbarSlot == -1) {
            stopEating();
            return;
        }

        if (mc.player.getInventory().selectedSlot != eatingHotbarSlot) {
            mc.player.getInventory().selectedSlot = eatingHotbarSlot;
            mc.interactionManager.syncSelectedSlot();
        }

        if (!isFoodStack(mc.player.getInventory().getStack(eatingHotbarSlot))) {
            stopEating();
            return;
        }

        mc.options.useKey.setPressed(true);
    }

    private boolean isFoodStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null;
    }

    private void stopEating() {
        mc.options.useKey.setPressed(false);

        if (mc.player != null && mc.interactionManager != null) {
            if (swappedInventorySlot != -1 && eatingHotbarSlot != -1) {
                mc.interactionManager.clickSlot(0, swappedInventorySlot, eatingHotbarSlot, SlotActionType.SWAP, mc.player);
            }

            if (previousSlot != -1 && mc.player.getInventory().selectedSlot != previousSlot) {
                mc.player.getInventory().selectedSlot = previousSlot;
                mc.interactionManager.syncSelectedSlot();
            }
        }

        isEating = false;
        previousSlot = -1;
        eatingHotbarSlot = -1;
        swappedInventorySlot = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopEating();
    }
}
