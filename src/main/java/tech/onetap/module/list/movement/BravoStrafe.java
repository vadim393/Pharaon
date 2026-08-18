package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.chat.ChatUtil;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "BravoStrafe", moduleDesc = "Быстрый свап элитры и нагрудника с запуском феерверка в полёте", moduleCategory = ModuleCategory.MOVEMENT)
public class BravoStrafe extends Module {

    private final ModeSetting swapMode = new ModeSetting("Мод свапа", "Vanilla", "Vanilla", "Grim", "Polar");
    private final ModeSetting fireworkMode = new ModeSetting("Мод феерверка", "Обычный", "Обычный", "Легитный");
    private final SliderSetting swapDelay = new SliderSetting("Задержка свапа", 50, 0, 500, 10);

    private final StopWatch stopWatch = new StopWatch();

    public BravoStrafe() {
        swapDelay.setValue(50);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.player != null) {
            stopWatch.reset();
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && !mc.player.isGliding()) {
                ChatUtil.send("BravoStrafe » Ты должен быть в полёте на элитре");
            }
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.player.isOnGround()) return;

        if (!stopWatch.isReached((long) swapDelay.getValue())) return;
        stopWatch.reset();

        var chest = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();

        if (chest != Items.ELYTRA) {
            int elytraSlot = InventoryUtil.findBestElytraSlot();
            if (elytraSlot == -1) return;
            swapToChest(elytraSlot);

            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                if (!mc.player.isGliding() && !mc.player.isOnGround()) {
                    NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startGliding();
                }
                fireWork();
            }
        } else {
            int chestSlot = InventoryUtil.findBestChestplateSlot();
            if (chestSlot == -1) return;
            swapToChest(chestSlot);
        }
    }

    private void swapToChest(int slot) {
        Runnable action = () -> {
            if (slot >= 0 && slot <= 8) {
                mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
            } else if (slot >= 8 && slot <= 45) {
                mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
            }
        };

        switch (swapMode.getValue()) {
            case "Grim" -> InventoryUtil.swapWithBypassGrim(action);
            case "Polar" -> InventoryUtil.swapWithBypassPolar(action);
            default -> action.run();
        }
    }

    private void fireWork() {
        if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.FIREWORK_ROCKET))) return;

        switch (fireworkMode.getValue()) {
            case "Легитный" -> InventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
            default -> InventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
        }
    }

    @Override
    public void onDisable() {
        stopWatch.reset();
        super.onDisable();
    }
}