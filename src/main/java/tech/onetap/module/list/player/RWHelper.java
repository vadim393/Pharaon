package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.other.InventoryUtil;

import java.util.Locale;

@ModuleInformation(moduleName = "RW Helper", moduleCategory = ModuleCategory.MISC)
public class RWHelper extends Module {

    public final BooleanSetting antipolet = new BooleanSetting("Антиполет", false);
    private final ModeSetting antiFlightMode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Grim", "Polar");
    private final BindSetting antiFlightKey = new BindSetting("Кнопочка антиполета", -1);

    private static final int OFFHAND_SLOT = 45;
    private static final int OFFHAND_SWAP_BUTTON = 40;
    private static final int ANTI_FLIGHT_SNEAK_TICKS = 3;

    boolean need;
    public boolean fireworkUse;

    private boolean antiFlightRequested;
    private boolean antiFlightSwapped;
    private int antiFlightSourceSlot = -1;
    private int antiFlightSneakTicks;

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (e.getAction() == 0 || e.getAction() == 2) return;

        if (e.getKey() == antiFlightKey.getValue()) {
            antiFlightRequested = true;
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof GameMessageS2CPacket p) {
            if (p.content().getString().contains("РђРЅС‚Рё РџРѕР»РµС‚ В» Р’С‹ РЅРµ РјРѕР¶РµС‚Рµ РІР·Р»РµС‚РµС‚СЊ!")) {
                need = true;
            }
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        handleAntiFlightButton();

        if (!antipolet.getValue()) return;

        if (need) {
            if (!mc.player.isOnGround() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                mc.player.startGliding();
                if (fireworkUse) {
                    InventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
                    fireworkUse = false;
                }
            } else {
                need = false;
            }
        }
    }

    @Override
    public void onDisable() {
        if (antiFlightSneakTicks > 0) {
            finishAntiFlightButton();
        } else {
            resetAntiFlightButtonState();
        }
        super.onDisable();
    }

    private void handleAntiFlightButton() {
        if (antiFlightRequested) {
            antiFlightRequested = false;
            if (antiFlightSneakTicks > 0) return;

            antiFlightSourceSlot = findAntiFlightSlot();
            if (antiFlightSourceSlot == -1) return;

            antiFlightSwapped = antiFlightSourceSlot != OFFHAND_SWAP_BUTTON;
            if (antiFlightSwapped && !swapSlotWithOffhand(antiFlightSourceSlot)) {
                resetAntiFlightButtonState();
                return;
            }

            if (mc.getNetworkHandler() == null) {
                resetAntiFlightButtonState();
                return;
            }

            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            antiFlightSneakTicks = ANTI_FLIGHT_SNEAK_TICKS;
            return;
        }

        if (antiFlightSneakTicks <= 0) return;

        antiFlightSneakTicks--;
        if (antiFlightSneakTicks == 0) {
            finishAntiFlightButton();
        }
    }

    private void finishAntiFlightButton() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        if (antiFlightSwapped && antiFlightSourceSlot != -1 && mc.player != null && mc.interactionManager != null) {
            swapSlotWithOffhand(antiFlightSourceSlot);
        }

        resetAntiFlightButtonState();
    }

    private void resetAntiFlightButtonState() {
        antiFlightRequested = false;
        antiFlightSwapped = false;
        antiFlightSourceSlot = -1;
        antiFlightSneakTicks = 0;
    }

    private int findAntiFlightSlot() {
        if (isAntiFlightStack(mc.player.getOffHandStack())) {
            return OFFHAND_SWAP_BUTTON;
        }

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (isAntiFlightStack(stack)) {
                return slot;
            }
        }

        return -1;
    }

    private boolean isAntiFlightStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String normalized = stack.getName().getString()
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace(" ", "");

        return normalized.contains("Р°РЅС‚РёРїРѕР»РµС‚")
                || normalized.contains("antiflight")
                || (normalized.contains("Р°РЅС‚Рё") && normalized.contains("РїРѕР»РµС‚"))
                || (normalized.contains("anti") && normalized.contains("flight"));
    }

    private boolean swapSlotWithOffhand(int slot) {
        if (slot < 0 || slot > 35) return false;

        Runnable swapAction = () -> {
            int syncId = mc.player.currentScreenHandler.syncId;
            if (slot <= 8) {
                mc.interactionManager.clickSlot(syncId, OFFHAND_SLOT, slot, SlotActionType.SWAP, mc.player);
                return;
            }

            mc.interactionManager.clickSlot(syncId, slot, OFFHAND_SWAP_BUTTON, SlotActionType.SWAP, mc.player);
        };

        switch (antiFlightMode.getValue()) {
            case "Grim" -> InventoryUtil.swapWithBypassGrim(swapAction);
            case "Polar" -> InventoryUtil.swapWithBypassPolar(swapAction);
            default -> swapAction.run();
        }

        return true;
    }
}
