package tech.onetap.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public abstract class ContainerButtonsMixin extends Screen {
    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    protected ContainerButtonsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onetap$initContainerButtons(CallbackInfo ci) {
        if (!onetap$isSupportedContainerScreen()) {
            return;
        }

        int topButtonWidth = 104;
        int sideButtonWidth = 96;
        int buttonHeight = 20;
        int spacing = 4;

        int topButtonX = this.x + (this.backgroundWidth - topButtonWidth) / 2;
        int topButtonY = Math.max(2, this.y - buttonHeight - 4);

        int sideButtonX = this.x + this.backgroundWidth + 6;
        int sideTopY = this.y + 6;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Выкинуть все"), b -> onetap$dropAllFromContainer())
                .dimensions(topButtonX, topButtonY, topButtonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сложить все"), b -> onetap$moveAllFromPlayerToContainer())
                .dimensions(sideButtonX, sideTopY, sideButtonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Взять все"), b -> onetap$takeAllFromContainer())
                .dimensions(sideButtonX, sideTopY + buttonHeight + spacing, sideButtonWidth, buttonHeight)
                .build());
    }

    @Unique
    private boolean onetap$isSupportedContainerScreen() {
        Object self = this;
        return self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen;
    }

    @Unique
    private void onetap$moveAllFromPlayerToContainer() {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return;
        }

        var player = this.client.player;
        var handler = player.currentScreenHandler;
        int syncId = handler.syncId;

        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            Slot slot = handler.slots.get(i);
            if (!onetap$isPlayerInventorySlot(slot, player.getInventory())) {
                continue;
            }
            if (!slot.hasStack()) {
                continue;
            }
            this.client.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
        }
    }

    @Unique
    private void onetap$takeAllFromContainer() {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return;
        }

        var player = this.client.player;
        var handler = player.currentScreenHandler;
        int syncId = handler.syncId;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory == player.getInventory()) {
                continue;
            }
            if (!slot.hasStack()) {
                continue;
            }
            this.client.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
        }
    }

    @Unique
    private void onetap$dropAllFromContainer() {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return;
        }

        var player = this.client.player;
        var handler = player.currentScreenHandler;
        int syncId = handler.syncId;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory == player.getInventory()) {
                continue;
            }
            if (!slot.hasStack()) {
                continue;
            }
            this.client.interactionManager.clickSlot(syncId, slot.id, 1, SlotActionType.THROW, player);
        }
    }

    @Unique
    private boolean onetap$isPlayerInventorySlot(Slot slot, PlayerInventory playerInventory) {
        if (slot.inventory != playerInventory) {
            return false;
        }
        int inventoryIndex = slot.getIndex();
        return inventoryIndex >= 0 && inventoryIndex <= 35;
    }
}
