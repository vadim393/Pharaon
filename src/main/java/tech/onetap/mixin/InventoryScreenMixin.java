package tech.onetap.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    protected InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onetap$initDropAllButton(CallbackInfo ci) {
        int buttonWidth = 96;
        int buttonHeight = 20;
        int buttonX = this.x + (this.backgroundWidth - buttonWidth) / 2;
        int buttonY = Math.max(2, this.y - buttonHeight - 4);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Выкинуть все"), b -> onetap$dropAllInventory())
                .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Unique
    private void onetap$dropAllInventory() {
        if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
            return;
        }

        var player = this.client.player;
        var handler = player.currentScreenHandler;
        int syncId = handler.syncId;

        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }

            if (!onetap$isDroppablePlayerSlot(slot, player.getInventory())) {
                continue;
            }

            this.client.interactionManager.clickSlot(syncId, slot.id, 1, SlotActionType.THROW, player);
        }
    }

    @Unique
    private boolean onetap$isDroppablePlayerSlot(Slot slot, PlayerInventory playerInventory) {
        if (slot.inventory != playerInventory) {
            return false;
        }

        int inventoryIndex = slot.getIndex();
        return inventoryIndex >= 0 && inventoryIndex <= 40;
    }
}
