package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.movement.Sprint;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.chat.ChatUtil;
import tech.onetap.util.player.other.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "ElytraHelper", moduleDesc = "Свап элитры и автоматический полёт", moduleCategory = ModuleCategory.PLAYER)
public class ElytraSwap extends Module {

    private static final String NO_ELYTRA = Formatting.RED + "" + Formatting.BOLD + "Нет элитры!";
    private static final String NO_CHESTPLATE = Formatting.RED + "" + Formatting.BOLD + "Нет нагрудника!";
    private static final String NO_FIREWORKS = Formatting.RED + "" + Formatting.BOLD + "Нет Фейерверков!";

    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");
    private final ModeSetting swapMode = new ModeSetting("Режим свапа", "1.21.4", "1.21.4", "1.17.1");
    private final BindSetting elytraBind = new BindSetting("Кнопка свапа", GLFW.GLFW_MOUSE_BUTTON_4);
    private final BindSetting fireworkBind = new BindSetting("Кнопка фейерверка", GLFW.GLFW_MOUSE_BUTTON_5);
    private final ModeSetting throwFireworkMode = new ModeSetting("Мод пуска фейерверка", "Обычный", "Обычный", "Легитный");
    private final BooleanSetting autofly = new BooleanSetting("Автовзлёт", true);

    private boolean swapElytraQueued;
    private boolean useFirework;
    private int bypassTicks;
    private boolean sprintPaused;
    private int swapCooldown;
    private int fireworkReturnSlot = -1;
    private int fireworkReturnTicks = -1;
    private boolean packetSwapActive;
    private int packetSwapStage;
    private int packetSwapSlot;
    private final List<ClickTask> pendingClicks = new ArrayList<>();
    private boolean swappingActive;

    @Subscribe
    private void onInput(MoveInputEvent e) {
        if (bypassTicks > 0 && mc.player != null) mc.player.setSprinting(false);
    }

    @Subscribe
    private void onTick(EventPlayerUpdate ignored) {
        if (mc.player == null) return;

        if (swapCooldown > 0) swapCooldown--;
        handleFireworkReturn();
        handlePacketSwap();

        if (bypassTicks > 0) {
            mc.player.setSprinting(false);
            bypassTicks--;
            if (bypassTicks == 1) performSwap();
            if (bypassTicks == 0) restoreSprint();
            return;
        }

        if (swapElytraQueued) {
            if (swapCooldown > 0 || swappingActive) {
                swapElytraQueued = false;
                return;
            }
            if (mc.currentScreen != null) {
                mc.setScreen(null);
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
                bypassTicks = 3;
            } else {
                bypassTicks = 2;
            }
            disableSprint();
            swapCooldown = 1;
            swapElytraQueued = false;
            return;
        }

        if (useFirework) {
            int slotFirework = InventoryUtil.searchItem(Items.FIREWORK_ROCKET);
            if (mc.player.isGliding()) {
                if (slotFirework != -1) {
                    InventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
                } else {
                    ChatUtil.send(NO_FIREWORKS);
                }
            }
            useFirework = false;
        }

        if (autofly.getValue() && bypassTicks == 0) {
            ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestStack.isOf(Items.ELYTRA) && !mc.player.isTouchingWater() && !mc.player.isInLava()
                    && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                mc.player.jump();
            } else if (chestStack.isOf(Items.ELYTRA) && isElytraUsable(chestStack)
                    && !mc.player.isGliding() && !mc.player.isOnGround()) {
                mc.player.startGliding();
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
    }

    private void handlePacketSwap() {
        if (!packetSwapActive || mc.player == null) return;

        if (packetSwapStage == 0) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            int nextSlot = (currentSlot + 1) % 9;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(nextSlot));
            packetSwapStage = 1;
        } else if (packetSwapStage == 1) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(packetSwapSlot));
            packetSwapActive = false;
            packetSwapStage = 0;
        }
    }

    private void performSwap() {
        final int slotElytra = InventoryUtil.findBestElytraSlot();
        final int chestSlot = InventoryUtil.findBestChestplateSlot();

        boolean needChestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        pendingClicks.clear();
        if (needChestplate) {
            if (chestSlot == -1) {
                ChatUtil.send(NO_CHESTPLATE);
                bypassTicks = 0;
                restoreSprint();
                return;
            }
            buildSwapClicks(chestSlot);
        } else {
            if (slotElytra == -1) {
                ChatUtil.send(NO_ELYTRA);
                bypassTicks = 0;
                restoreSprint();
                return;
            }
            buildSwapClicks(slotElytra);
        }
        swappingActive = true;
        flushClicks();
    }

    private void flushClicks() {
        if (mc.player == null || pendingClicks.isEmpty()) return;
        if (mc.currentScreen != null) {
            pendingClicks.clear();
            swappingActive = false;
            return;
        }

        while (!pendingClicks.isEmpty()) {
            ClickTask task = pendingClicks.remove(0);
            mc.interactionManager.clickSlot(0, task.slot, task.button, task.action, mc.player);
        }
        swappingActive = false;
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
    }

    private void buildSwapClicks(int slot) {
        if (swapMode.is("1.17.1")) {
            int screen = screenSlot(slot);
            pendingClicks.add(new ClickTask(screen, 0, SlotActionType.PICKUP));
            pendingClicks.add(new ClickTask(6, 0, SlotActionType.PICKUP));
            pendingClicks.add(new ClickTask(screen, 0, SlotActionType.PICKUP));
        } else if (slot >= 0 && slot < 9) {
            pendingClicks.add(new ClickTask(6, slot, SlotActionType.SWAP));
        } else {
            // Двигаем предмет на хотбар, меняем его с нагрудником, возвращаем всё обратно.
            pendingClicks.add(new ClickTask(slot, 0, SlotActionType.SWAP));
            pendingClicks.add(new ClickTask(6, 0, SlotActionType.SWAP));
            pendingClicks.add(new ClickTask(slot, 0, SlotActionType.SWAP));
        }
    }

    private int screenSlot(int slot) {
        if (slot >= 0 && slot < 9) return slot + 36;
        return slot;
    }

    private static final class ClickTask {
        final int slot;
        final int button;
        final SlotActionType action;

        ClickTask(int slot, int button, SlotActionType action) {
            this.slot = slot;
            this.button = button;
            this.action = action;
        }
    }

    private void handleFireworkReturn() {
        if (fireworkReturnTicks < 0) return;
        if (fireworkReturnTicks > 0) {
            fireworkReturnTicks--;
            return;
        }
        if (fireworkReturnSlot != -1) {
            swapSlotToOffhand(fireworkReturnSlot);
        }
        fireworkReturnSlot = -1;
        fireworkReturnTicks = -1;
    }

    private int findScreenSlot(Item item) {
        for (int slot = 9; slot < 45; slot++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(slot).getStack();
            if (stack.isOf(item)) return slot;
        }
        return -1;
    }

    private void swapSlotToOffhand(int slot) {
        if (slot >= 36 && slot <= 44) {
            mc.interactionManager.clickSlot(0, 45, slot - 36, SlotActionType.SWAP, mc.player);
            return;
        }
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
    }

    private void disableSprint() {
        if (sprintPaused) return;
        Sprint.pushPause(1000);
        sprintPaused = true;
    }

    private void restoreSprint() {
        if (!sprintPaused) return;
        sprintPaused = false;
        Sprint.popPause();
    }

    private boolean isElytraUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    @Subscribe
    private void onKey(EventKeyInput event) {
        if (event.getAction() == 0) return;
        if (event.getKey() == elytraBind.getValue()) swapElytraQueued = true;
        if (event.getKey() == fireworkBind.getValue()) useFirework = true;
    }

    public void swap(ModeSetting mode, boolean chestplate) {
        swapElytraQueued = true;
    }

    @Override
    public void onDisable() {
        bypassTicks = 0;
        swapCooldown = 0;
        fireworkReturnSlot = -1;
        fireworkReturnTicks = -1;
        packetSwapActive = false;
        packetSwapStage = 0;
        swapElytraQueued = false;
        swappingActive = false;
        pendingClicks.clear();
        restoreSprint();
        super.onDisable();
    }
}