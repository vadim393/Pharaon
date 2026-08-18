package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventSlotClick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.util.player.other.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Auto Swap", moduleCategory = ModuleCategory.COMBAT)
public class AutoSwap extends Module {
    private final ModeSetting selection = new ModeSetting("Режим", "Обычный", "Обычный", "Трипл");

    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar", "Triple", "ReallyWorld").setVisible(() -> selection.is("Обычный"));
    private final BindSetting swapKey = new BindSetting("Клавиша свапа", -1).setVisible(() -> selection.is("Обычный"));
    private final BindSetting headSwapKey = new BindSetting("Клавиша хед-свапа", -1).setVisible(() -> selection.is("Обычный"));
    private final ModeSetting firstItem = new ModeSetting("Свапать с", "Шар", "Гепл", "Щит", "Талисман", "Шар").setVisible(() -> selection.is("Обычный"));
    private final ModeSetting secondItem = new ModeSetting("Свапать с", "Шар", "Гепл", "Щит", "Талисман", "Шар").setVisible(() -> selection.is("Обычный"));

    private final ModeSetting tripleMode = new ModeSetting("Тип", "Предметы", "Предметы", "Колесо").setVisible(() -> selection.is("Трипл"));
    private final BindSetting tripleBind = new BindSetting("Кнопка свапа", -1).setVisible(() -> selection.is("Трипл") && tripleMode.is("Предметы"));
    private final ModeSetting tripleFirstItem = new ModeSetting("Первый", "Тотем", "Тотем", "Шар", "Гепл", "Щит").setVisible(() -> selection.is("Трипл") && tripleMode.is("Предметы"));
    private final ModeSetting tripleSecondItem = new ModeSetting("Второй", "Тотем", "Тотем", "Шар", "Гепл", "Щит").setVisible(() -> selection.is("Трипл") && tripleMode.is("Предметы"));
    private final BindSetting wheelBind = new BindSetting("Бинд колеса", -1).setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final SliderSetting wheelSlots = new SliderSetting("Ячейки", 3f, 3f, 8f, 1f).setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot1 = new StringSetting("Слот 1", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot2 = new StringSetting("Слот 2", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot3 = new StringSetting("Слот 3", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot4 = new StringSetting("Слот 4", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot5 = new StringSetting("Слот 5", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot6 = new StringSetting("Слот 6", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot7 = new StringSetting("Слот 7", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));
    private final StringSetting slot8 = new StringSetting("Слот 8", "minecraft:air").setVisible(() -> selection.is("Трипл") && tripleMode.is("Колесо"));

    private boolean swapped;
    private boolean headSwapped;

    private boolean wheelOpen;
    private int pendingPickSlot = -1;
    private long removeFlashUntilMs;
    private int removeFlashIndex = -1;
    private boolean cursorUnlocked;

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (mc.player == null || !selection.is("Обычный")) return;

        if (headSwapped) {
            headSwapped = false;
            handleHeadSwap();
            return;
        }

        if (swapped) {
            swapped = false;
            boolean sameItem = firstItem.getValue().equals(secondItem.getValue());

            int slotFirstItem = findItemByName(firstItem.getValue(), sameItem);
            int slotSecondItem = findItemByName(secondItem.getValue(), sameItem);

            if (slotFirstItem == 40 && slotSecondItem == 40) {
                slotSecondItem = InventoryUtil.searchItemStack(item ->
                        item.getItem() == Items.PLAYER_HEAD &&
                                item != mc.player.getOffHandStack()
                );
            }

            if (slotFirstItem == -1 && slotSecondItem == -1) return;
            if (slotFirstItem == 40 || slotFirstItem == -1 && slotSecondItem != 40) {
                if (slotSecondItem >= 0 && slotSecondItem <= 8) {
                    int finalSlotSecondItem = slotSecondItem;
                    ItemStack notificationStack = getStackBySlot(slotSecondItem);
                    switch (mode.getValue()) {
                        case "Vanilla" -> mc.interactionManager.clickSlot(0, 45, slotSecondItem, SlotActionType.SWAP, mc.player);
                        case "Grim" -> InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, 45, finalSlotSecondItem, SlotActionType.SWAP, mc.player));
                        case "Polar" -> InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, 45, finalSlotSecondItem, SlotActionType.SWAP, mc.player));
                        case "Triple" -> executeTripleSwap(finalSlotSecondItem, 45);
                        case "ReallyWorld" -> {
                            if (mc.player.isOnGround()) InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, 45, finalSlotSecondItem, SlotActionType.SWAP, mc.player));
                            else InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, 45, finalSlotSecondItem, SlotActionType.SWAP, mc.player));
                        }
                    }
                    postSwapNotification(notificationStack);
                } else if (slotSecondItem != -1) {
                    int finalSlotSecondItem = slotSecondItem;
                    ItemStack notificationStack = getStackBySlot(slotSecondItem);
                    switch (mode.getValue()) {
                        case "Vanilla" -> mc.interactionManager.clickSlot(0, slotSecondItem, 40, SlotActionType.SWAP, mc.player);
                        case "Grim" -> InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, finalSlotSecondItem, 40, SlotActionType.SWAP, mc.player));
                        case "Polar" -> InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, finalSlotSecondItem, 40, SlotActionType.SWAP, mc.player));
                        case "Triple" -> executeTripleSwap(finalSlotSecondItem, 45);
                        case "ReallyWorld" -> {
                            if (mc.player.isOnGround()) InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, finalSlotSecondItem, 40, SlotActionType.SWAP, mc.player));
                            else InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, finalSlotSecondItem, 40, SlotActionType.SWAP, mc.player));
                        }
                    }
                    postSwapNotification(notificationStack);
                }
            } else {
                if (slotFirstItem == -1) return;
                if (slotFirstItem >= 0 && slotFirstItem <= 8) {
                    ItemStack notificationStack = getStackBySlot(slotFirstItem);
                    switch (mode.getValue()) {
                        case "Vanilla" -> mc.interactionManager.clickSlot(0, 45, slotFirstItem, SlotActionType.SWAP, mc.player);
                        case "Grim" -> InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, 45, slotFirstItem, SlotActionType.SWAP, mc.player));
                        case "Polar" -> InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, 45, slotFirstItem, SlotActionType.SWAP, mc.player));
                        case "Triple" -> executeTripleSwap(slotFirstItem, 45);
                        case "ReallyWorld" -> {
                            if (mc.player.isOnGround()) InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, 45, slotFirstItem, SlotActionType.SWAP, mc.player));
                            else InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, 45, slotFirstItem, SlotActionType.SWAP, mc.player));
                        }
                    }
                    postSwapNotification(notificationStack);
                } else {
                    ItemStack notificationStack = getStackBySlot(slotFirstItem);
                    switch (mode.getValue()) {
                        case "Vanilla" -> mc.interactionManager.clickSlot(0, slotFirstItem, 40, SlotActionType.SWAP, mc.player);
                        case "Grim" -> InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, slotFirstItem, 40, SlotActionType.SWAP, mc.player));
                        case "Polar" -> InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, slotFirstItem, 40, SlotActionType.SWAP, mc.player));
                        case "Triple" -> executeTripleSwap(slotFirstItem, 45);
                        case "ReallyWorld" -> {
                            if (mc.player.isOnGround()) InventoryUtil.swapWithBypassPolar(() -> mc.interactionManager.clickSlot(0, slotFirstItem, 40, SlotActionType.SWAP, mc.player));
                            else InventoryUtil.swapWithBypassGrim(() -> mc.interactionManager.clickSlot(0, slotFirstItem, 40, SlotActionType.SWAP, mc.player));
                        }
                    }
                    postSwapNotification(notificationStack);
                }
            }
        }
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (mc.player == null) return;
        if (selection.is("Трипл")) {
            onTripleKey(e);
            return;
        }
        if (e.getAction() == 0) return;
        if (e.getKey() == swapKey.getValue()) {
            swapped = true;
        }
        if (e.getKey() == headSwapKey.getValue()) headSwapped = true;
    }

    private void onTripleKey(EventKeyInput e) {
        if (e.getAction() != 1) return;

        if (tripleMode.is("Предметы")) {
            if (e.getKey() == tripleBind.getValue()) {
                tripleSwapItems();
            }
            return;
        }

        if (tripleMode.is("Колесо")) {
            if (e.getKey() == wheelBind.getValue()) {
                wheelOpen = !wheelOpen;
                if (!wheelOpen) {
                    pendingPickSlot = -1;
                    removeFlashIndex = -1;
                    removeFlashUntilMs = 0L;
                    updateWheelCursorState(false);
                }
                if (wheelOpen) {
                    updateWheelCursorState(true);
                }
                return;
            }

            if (wheelOpen && (e.getKey() == 0 || e.getKey() == 1)) {
                handleWheelClick(e.getKey());
            }
        }
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (mc.player == null) return;
        if (!selection.is("Трипл") || !tripleMode.is("Колесо") || !wheelOpen) return;
        if (mc.currentScreen != null) return;

        updateWheelCursorState(true);
        renderWheel(e.getDrawContext());
    }

    @Subscribe
    private void onClickSlot(EventSlotClick e) {
        if (mc.player == null) return;
        if (!selection.is("Трипл") || !tripleMode.is("Колесо") || !wheelOpen) return;
        if (pendingPickSlot == -1) return;
        if (!(mc.currentScreen instanceof InventoryScreen)) return;
        if (e.getActionType() != SlotActionType.PICKUP) return;

        Slot clicked = e.getSlot();
        if (clicked == null) return;
        ItemStack picked = clicked.getStack();
        if (picked.isEmpty() || picked.getItem() == Items.AIR) return;

        Identifier id = Registries.ITEM.getId(picked.getItem());
        if (id == null) return;

        setSlotString(pendingPickSlot, id.toString());
        e.setCancelled(true);
        pendingPickSlot = -1;
        mc.setScreen(null);
        updateWheelCursorState(true);
    }

    private void tripleSwapItems() {
        if (mc.player == null) return;

        Item first = getItemByType(tripleFirstItem.getValue());
        Item second = getItemByType(tripleSecondItem.getValue());

        Slot firstSlot = findSlot(first);
        Slot secondSlot = findSlot(second);

        Slot validSlot = firstSlot != null && mc.player.getOffHandStack().getItem() != first ? firstSlot : secondSlot;
        if (validSlot != null) {
            executeTripleSwap(validSlot.id, 45);
        }
    }

    private void handleWheelClick(int button) {
        if (mc.currentScreen != null) return;

        int count = getWheelSlotCount();
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        float outerR = 92f;
        float innerR = 54f;

        float mouseX = (float) (mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
        float mouseY = (float) (mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());

        int hover = getHoverIndex(mouseX, mouseY, cx, cy, innerR, outerR, count);
        if (hover == -1) return;

        if (button == 1) {
            setSlotString(hover, "minecraft:air");
            removeFlashIndex = hover;
            removeFlashUntilMs = System.currentTimeMillis() + 250L;
            return;
        }

        if (button == 0) {
            ItemStack stack = getStackForIndex(hover);
            if (stack.isEmpty() || stack.getItem() == Items.AIR) {
                pendingPickSlot = hover;
                mc.setScreen(new InventoryScreen(mc.player));
                return;
            }

            Slot slot = findSlot(stack.getItem());
            if (slot != null) {
                executeTripleSwap(slot.id, 45);
            }
        }
    }

    private void renderWheel(DrawContext context) {
        int count = getWheelSlotCount();
        float cx = context.getScaledWindowWidth() / 2f;
        float cy = context.getScaledWindowHeight() / 2f;
        float outerR = 92f;
        float innerR = 54f;

        float mouseX = (float) (mc.mouse.getX() * context.getScaledWindowWidth() / mc.getWindow().getWidth());
        float mouseY = (float) (mc.mouse.getY() * context.getScaledWindowHeight() / mc.getWindow().getHeight());

        int hover = getHoverIndex(mouseX, mouseY, cx, cy, innerR, outerR, count);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < count; i++) {
            boolean isHover = i == hover;
            boolean isRemoveFlash = i == removeFlashIndex && System.currentTimeMillis() <= removeFlashUntilMs;

            int r = 205;
            int g = 205;
            int b = 205;
            int a = 95;

            if (isHover) {
                r = 255;
                g = 209;
                b = 47;
                a = 140;
            }
            if (isRemoveFlash) {
                r = 255;
                g = 70;
                b = 70;
                a = 140;
            }

            float start = (float) (-Math.PI / 2d + (2d * Math.PI) * (i / (double) count));
            float end = (float) (-Math.PI / 2d + (2d * Math.PI) * ((i + 1d) / (double) count));
            drawRingSegment(buffer, matrix, cx, cy, innerR, outerR, start, end, r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        for (int i = 0; i < count; i++) {
            ItemStack stack = getStackForIndex(i);
            if (stack.isEmpty() || stack.getItem() == Items.AIR) continue;

            float start = (float) (-Math.PI / 2d + (2d * Math.PI) * (i / (double) count));
            float end = (float) (-Math.PI / 2d + (2d * Math.PI) * ((i + 1d) / (double) count));
            float mid = (start + end) / 2f;
            float iconR = (innerR + outerR) / 2f;
            float ix = cx + (float) Math.cos(mid) * iconR;
            float iy = cy + (float) Math.sin(mid) * iconR;
            context.drawItem(stack, (int) (ix - 8), (int) (iy - 8));
            context.drawStackOverlay(mc.textRenderer, stack, (int) (ix - 8), (int) (iy - 8));
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void updateWheelCursorState(boolean shouldBeUnlocked) {
        if (mc == null || mc.mouse == null) return;

        if (shouldBeUnlocked) {
            if (!cursorUnlocked) {
                mc.mouse.unlockCursor();
                cursorUnlocked = true;
            }
            return;
        }

        if (cursorUnlocked) {
            if (mc.currentScreen == null) {
                mc.mouse.lockCursor();
            }
            cursorUnlocked = false;
        }
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Шар" -> Items.PLAYER_HEAD;
            case "Гепл" -> Items.GOLDEN_APPLE;
            case "Щит" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }

    private Slot findSlot(Item item) {
        if (mc.player == null || item == null) return null;
        Slot best = null;
        boolean bestEnchanted = false;
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.id == 45 || slot.id == 46) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || stack.getItem() != item) continue;
            boolean enchanted = stack.hasEnchantments();
            if (best == null || (enchanted && !bestEnchanted)) {
                best = slot;
                bestEnchanted = enchanted;
            }
        }
        return best;
    }

    private int getWheelSlotCount() {
        int count = Math.round(wheelSlots.getFloatValue());
        return MathHelper.clamp(count, 3, 8);
    }

    private List<StringSetting> getWheelSettings() {
        List<StringSetting> list = new ArrayList<>();
        list.add(slot1);
        list.add(slot2);
        list.add(slot3);
        list.add(slot4);
        list.add(slot5);
        list.add(slot6);
        list.add(slot7);
        list.add(slot8);
        return list;
    }

    private void setSlotString(int index, String value) {
        List<StringSetting> settings = getWheelSettings();
        if (index < 0 || index >= settings.size()) return;
        settings.get(index).setValue(value);
    }

    private ItemStack getStackForIndex(int index) {
        List<StringSetting> settings = getWheelSettings();
        if (index < 0 || index >= settings.size()) return ItemStack.EMPTY;
        String raw = settings.get(index).getValue();
        if (raw == null || raw.isBlank()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return item.getDefaultStack();
    }

    private int getHoverIndex(float mouseX, float mouseY, float cx, float cy, float innerR, float outerR, int count) {
        float dx = mouseX - cx;
        float dy = mouseY - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < innerR || dist > outerR) return -1;

        double ang = Math.atan2(dy, dx);
        ang = ang + Math.PI / 2d;
        if (ang < 0) ang += Math.PI * 2d;

        int idx = (int) Math.floor((ang / (Math.PI * 2d)) * count);
        if (idx < 0 || idx >= count) return -1;
        return idx;
    }

    private void drawRingSegment(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float innerR, float outerR, float start, float end, int r, int g, int b, int a) {
        int steps = Math.max(10, (int) (48 * (Math.abs(end - start) / (Math.PI * 2f))));
        float step = (end - start) / steps;

        for (int i = 0; i < steps; i++) {
            float a0 = start + step * i;
            float a1 = start + step * (i + 1);

            float x0o = cx + (float) Math.cos(a0) * outerR;
            float y0o = cy + (float) Math.sin(a0) * outerR;
            float x1o = cx + (float) Math.cos(a1) * outerR;
            float y1o = cy + (float) Math.sin(a1) * outerR;

            float x0i = cx + (float) Math.cos(a0) * innerR;
            float y0i = cy + (float) Math.sin(a0) * innerR;
            float x1i = cx + (float) Math.cos(a1) * innerR;
            float y1i = cy + (float) Math.sin(a1) * innerR;

            buffer.vertex(matrix, x0i, y0i, 0).color(r, g, b, a);
            buffer.vertex(matrix, x0o, y0o, 0).color(r, g, b, a);
            buffer.vertex(matrix, x1o, y1o, 0).color(r, g, b, a);

            buffer.vertex(matrix, x0i, y0i, 0).color(r, g, b, a);
            buffer.vertex(matrix, x1o, y1o, 0).color(r, g, b, a);
            buffer.vertex(matrix, x1i, y1i, 0).color(r, g, b, a);
        }
    }

    private void executeTripleSwap(int slot, int targetSlot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        boolean wasSprinting = mc.player.isSprinting();

        if (wasSprinting) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));

        int invIndex = slot >= 36 ? slot - 36 : slot;
        if (invIndex >= 0 && invIndex <= 8) {
            mc.interactionManager.clickSlot(syncId, targetSlot, invIndex, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        }

        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
        if (wasSprinting) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
    }

    private void handleHeadSwap() {
        if (mc.player == null) return;
        ItemStack headStack = mc.player.getEquippedStack(EquipmentSlot.HEAD);
        boolean isWearingBall = headStack.isOf(Items.PLAYER_HEAD);
        int slot = isWearingBall ? findHelmet() : InventoryUtil.searchItem(Items.PLAYER_HEAD);
        if (slot != -1) executeTripleSwap(slot, 5);
    }

    private int findHelmet() {
        int s = InventoryUtil.searchItem(Items.NETHERITE_HELMET);
        if (s == -1) s = InventoryUtil.searchItem(Items.DIAMOND_HELMET);

        if (s == -1) {
            for (int i = 0; i < 36; i++) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if (item instanceof ArmorItem && item.toString().contains("helmet")) return i;
            }
        }
        return s;
    }

    private int findItemByName(String name, boolean ignoreOffhand) {
        switch (name) {
            case "Гепл" -> {
                if (!ignoreOffhand && mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE)
                    return 40;
                return InventoryUtil.searchItem(Items.GOLDEN_APPLE);
            }

            case "Щит" -> {
                if (!ignoreOffhand && mc.player.getOffHandStack().getItem() == Items.SHIELD)
                    return 40;
                return InventoryUtil.searchItem(Items.SHIELD);
            }

            case "Талисман" -> {
                if (!ignoreOffhand &&
                        mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING &&
                        mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                        !mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty())
                    return 40;

                return InventoryUtil.searchItemStack(item ->
                        item.getItem() == Items.TOTEM_OF_UNDYING &&
                                item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                                !item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
                );
            }

            case "Шар" -> {
                if (!ignoreOffhand &&
                        mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD &&
                        mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                        !mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty())
                    return 40;

                return InventoryUtil.searchItemStack(item ->
                        item.getItem() == Items.PLAYER_HEAD &&
                                item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                                !item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
                );
            }
        }
        return -1;
    }

    private ItemStack getStackBySlot(int slot) {
        if (slot < 0) return ItemStack.EMPTY;
        return mc.player.getInventory().getStack(slot).copy();
    }

    private void postSwapNotification(ItemStack stack) {
        if (stack.isEmpty()) return;
        Interface.NotificationManager.postAutoSwapInfo(stack.getName().getString(), stack);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        wheelOpen = false;
        pendingPickSlot = -1;
        removeFlashIndex = -1;
        removeFlashUntilMs = 0L;
        updateWheelCursorState(false);
        if (mc.player != null && mc.currentScreen instanceof InventoryScreen) {
            mc.setScreen(null);
        }
    }
}
