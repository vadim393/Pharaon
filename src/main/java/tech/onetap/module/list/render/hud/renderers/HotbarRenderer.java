package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class HotbarRenderer {
    private final Interface owner;
    private float animatedHotbarSlot;
    private boolean hotbarAnimationInitialized;

    public HotbarRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null || owner.mc.player.isSpectator()) {
            hotbarAnimationInitialized = false;
            return;
        }

        var inventory = owner.mc.player.getInventory();
        int selectedSlot = inventory.selectedSlot;
        if (!hotbarAnimationInitialized) {
            animatedHotbarSlot = selectedSlot;
            hotbarAnimationInitialized = true;
        }

        animatedHotbarSlot = MathHelper.lerp(0.24f, animatedHotbarSlot, selectedSlot);
        animatedHotbarSlot = MathHelper.clamp(animatedHotbarSlot, 0f, 8f);

        if (owner.isAxiomStyle()) {
            renderAxiom(context, inventory);
            return;
        }

        float slotStride = 20f;
        float slotSize = 16f;
        float selectedSlotSize = 20f;
        float barWidth = 182f;
        float barHeight = 22f;
        float radius = 6f;
        float barX = owner.mc.getWindow().getScaledWidth() / 2f - 91f;
        float barY = owner.mc.getWindow().getScaledHeight() - 23f;
        float itemInsetX = 3f;
        float itemInsetY = 3f;

        owner.drawBackground(barX, barY, barWidth, barHeight, radius, 230);

        float selectedX = barX + 1f + animatedHotbarSlot * slotStride;
        float selectedY = barY + 1f;

        DrawUtil.drawRoundBlur(
                selectedX - 0.5f,
                selectedY - 0.5f,
                selectedSlotSize + 1f,
                selectedSlotSize + 1f,
                6f,
                ColorProvider.setAlpha(ColorProvider.getThemeColor(), 120),
                ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), 120),
                16f
        );
        DrawUtil.drawRound(
                selectedX,
                selectedY,
                selectedSlotSize,
                selectedSlotSize,
                5.5f,
                ColorProvider.rgba(255, 255, 255, 34),
                ColorProvider.rgba(255, 255, 255, 16)
        );
        for (int slot = 0; slot < 9; slot++) {
            float itemXBase = barX + itemInsetX + slot * slotStride;
            float itemYBase = barY + itemInsetY;

            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int itemX = Math.round(itemXBase);
            int itemY = Math.round(itemYBase);
            context.drawItem(stack, itemX, itemY);
            context.drawStackOverlay(owner.mc.textRenderer, stack, itemX, itemY);
        }

        ItemStack offHandStack = owner.mc.player.getOffHandStack();
        if (!offHandStack.isEmpty()) {
            float offhandWidth = 22f;
            float offhandX = barX - offhandWidth - 4f;
            owner.drawBackground(offhandX, barY, offhandWidth, barHeight, radius, 200);

            int itemX = Math.round(offhandX + 3f);
            int itemY = Math.round(barY + itemInsetY);
            context.drawItem(offHandStack, itemX, itemY);
            context.drawStackOverlay(owner.mc.textRenderer, offHandStack, itemX, itemY);
        }
    }

    private void renderAxiom(DrawContext context, net.minecraft.entity.player.PlayerInventory inventory) {
        float slotSize = 18.0f;
        float slotGap = 3.0f;
        float totalWidth = slotSize * 9.0f + slotGap * 8.0f;
        float startX = owner.mc.getWindow().getScaledWidth() / 2f - totalWidth / 2f;
        float barY = owner.mc.getWindow().getScaledHeight() - 23.0f;
        float itemInset = 1.0f;

        float highlightX = startX + animatedHotbarSlot * (slotSize + slotGap) - 1.0f;
        owner.drawBackground(highlightX, barY - 1.0f, slotSize + 2.0f, slotSize + 2.0f, 5.5f, 245);
        owner.drawAxiomAccent(highlightX + 2.5f, barY + 1.5f, slotSize - 3.0f, 2.3f, 1.2f, 255);

        for (int slot = 0; slot < 9; slot++) {
            float slotX = startX + slot * (slotSize + slotGap);
            owner.drawBackground(slotX, barY, slotSize, slotSize, 5.0f, 220);

            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int itemX = Math.round(slotX + itemInset);
            int itemY = Math.round(barY + itemInset);
            context.drawItem(stack, itemX, itemY);
            context.drawStackOverlay(owner.mc.textRenderer, stack, itemX, itemY);
        }

        ItemStack offHandStack = owner.mc.player.getOffHandStack();
        if (!offHandStack.isEmpty()) {
            float offhandX = startX + totalWidth + 7.0f;
            owner.drawBackground(offhandX, barY, slotSize, slotSize, 5.0f, 210);
            owner.drawAxiomAccent(offhandX + 2.5f, barY + 1.5f, slotSize - 5.0f, 2.1f, 1.1f, 210);

            int itemX = Math.round(offhandX + itemInset);
            int itemY = Math.round(barY + itemInset);
            context.drawItem(offHandStack, itemX, itemY);
            context.drawStackOverlay(owner.mc.textRenderer, offHandStack, itemX, itemY);
        }
    }
}
