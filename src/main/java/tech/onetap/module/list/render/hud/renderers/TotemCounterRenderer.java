package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class TotemCounterRenderer {
    private final Interface owner;

    public TotemCounterRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        Draggable drag = owner.getTotemCounterDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        int totemCount = countItem(Items.TOTEM_OF_UNDYING);
        int fireworkCount = countItem(Items.FIREWORK_ROCKET);

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context, posX, posY, totemCount, fireworkCount, drag);
            return;
        }

        if (owner.isAxiomStyle()) {
            renderAxiom(context, posX, posY, totemCount, fireworkCount, drag);
            return;
        }

        renderDefault(context, posX, posY, totemCount, fireworkCount, drag);
    }

    private int countItem(Item item) {
        int count = 0;
        for (int i = 0; i < owner.mc.player.getInventory().size(); i++) {
            ItemStack stack = owner.mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void drawItemIcon(DrawContext context, Item item, float x, float y, float itemSize) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(itemSize / 16f, itemSize / 16f, 1f);
        context.drawItem(new ItemStack(item), 0, 0);
        context.getMatrices().pop();
    }

    private void renderDefault(DrawContext context, float posX, float posY, int totemCount, int fireworkCount, Draggable drag) {
        boolean showTotem = totemCount > 0;
        boolean showFirework = fireworkCount > 0;
        if (!showTotem && !showFirework) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float itemSize = 11f;
        float padX = 7f;
        float padY = 5f;
        float textGap = 5f;
        float lineHeight = 13f;

        String totemText = showTotem ? "x" + totemCount : "";
        String fireworkText = showFirework ? "x" + fireworkCount : "";
        float totemTextW = showTotem ? Fonts.SFREGULAR.get().getWidth(totemText, 9) : 0;
        float fireworkTextW = showFirework ? Fonts.SFREGULAR.get().getWidth(fireworkText, 9) : 0;
        float contentW = itemSize + textGap + Math.max(totemTextW, fireworkTextW);
        float panelW = padX * 2 + contentW;
        int rows = (showTotem ? 1 : 0) + (showFirework ? 1 : 0);
        float panelH = padY * 2 + rows * lineHeight - (rows > 1 ? 2 : 0);

        owner.drawBackground(posX, posY, panelW, panelH, 4f, 235);

        float y = posY + padY;
        if (showTotem) {
            drawItemIcon(context, Items.TOTEM_OF_UNDYING, posX + padX, y, itemSize);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), totemText, posX + padX + itemSize + textGap, y + (itemSize - 9f) / 2f, -1, 9);
            y += lineHeight;
        }
        if (showFirework) {
            drawItemIcon(context, Items.FIREWORK_ROCKET, posX + padX, y, itemSize);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), fireworkText, posX + padX + itemSize + textGap, y + (itemSize - 9f) / 2f, -1, 9);
        }

        drag.setWidth(panelW);
        drag.setHeight(panelH);
    }

    private void renderPouchOld(DrawContext context, float posX, float posY, int totemCount, int fireworkCount, Draggable drag) {
        boolean showTotem = totemCount > 0;
        boolean showFirework = fireworkCount > 0;
        if (!showTotem && !showFirework) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float padX = 5f;
        float itemSize = 10f;
        float panelRadius = 4f;
        float padY = 4f;
        float lineHeight = 12f;

        String totemText = "x" + totemCount;
        String fireworkText = "x" + fireworkCount;
        float totemTextW = showTotem ? Fonts.SFMEDIUM.get().getWidth(totemText, 6f) : 0;
        float fireworkTextW = showFirework ? Fonts.SFMEDIUM.get().getWidth(fireworkText, 6f) : 0;
        float contentW = itemSize + 4f + Math.max(totemTextW, fireworkTextW);
        float panelWidth = Math.max(40f, padX + contentW + padX);
        int rows = (showTotem ? 1 : 0) + (showFirework ? 1 : 0);
        float panelHeight = padY * 2 + rows * lineHeight;

        int bgColor = ColorProvider.rgba(30, 25, 40, 255);
        int textColor = ColorProvider.rgba(255, 255, 255, 220);

        DrawUtil.drawRoundBlur(posX, posY, panelWidth, panelHeight, panelRadius, bgColor, 15f);
        DrawUtil.drawRound(posX, posY, panelWidth, panelHeight, panelRadius, bgColor);

        float y = posY + padY;
        if (showTotem) {
            drawPouchRow(context, posX, y, Items.TOTEM_OF_UNDYING, totemText, itemSize, textColor);
            y += lineHeight;
        }
        if (showFirework) {
            drawPouchRow(context, posX, y, Items.FIREWORK_ROCKET, fireworkText, itemSize, textColor);
        }

        drag.setWidth(panelWidth);
        drag.setHeight(panelHeight);
    }

    private void drawPouchRow(DrawContext context, float posX, float y, Item item, String countText, float itemSize, int textColor) {
        float padX = 5f;
        drawItemIcon(context, item, posX + padX, y, itemSize);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), countText, posX + padX + itemSize + 4f, y + (itemSize - 6f) / 2f + 0.5f, textColor, 6f);
    }

    private void renderAxiom(DrawContext context, float posX, float posY, int totemCount, int fireworkCount, Draggable drag) {
        boolean showTotem = totemCount > 0;
        boolean showFirework = fireworkCount > 0;
        if (!showTotem && !showFirework) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        String totemText = String.valueOf(totemCount);
        String fireworkText = String.valueOf(fireworkCount);
        float countSize = 8.6f;
        float labelSize = 5.8f;
        float rowH = 13f;
        float panelHeight = 5.5f + ((showTotem ? 1 : 0) + (showFirework ? 1 : 0)) * rowH;
        float maxTextW = Math.max(
                Fonts.RUBIK.get().getWidth(totemText, countSize) + Fonts.SFMEDIUM.get().getWidth("TOTEMS", labelSize),
                Fonts.RUBIK.get().getWidth(fireworkText, countSize) + Fonts.SFMEDIUM.get().getWidth("FIREWORKS", labelSize)
        );
        float panelWidth = Math.max(50.0f, 28.0f + maxTextW);

        owner.drawBackground(posX, posY, panelWidth, panelHeight, 5.0f, 235);
        owner.drawAxiomAccent(posX + 4.0f, posY + 3.0f, 13.5f, panelHeight - 6.0f, 3.2f, 255);

        float y = posY + 3.0f;
        if (showTotem) {
            drawAxiomRow(context, posX, y, Items.TOTEM_OF_UNDYING, totemText, "TOTEMS", countSize, labelSize);
            y += rowH;
        }
        if (showFirework) {
            drawAxiomRow(context, posX, y, Items.FIREWORK_ROCKET, fireworkText, "FIREWORKS", countSize, labelSize);
        }

        drag.setWidth(panelWidth);
        drag.setHeight(panelHeight);
    }

    private void drawAxiomRow(DrawContext context, float posX, float y, Item item, String countText, String label, float countSize, float labelSize) {
        drawItemIcon(context, item, posX + 4.25f, y + 0.2f, 11.5f);
        DrawUtil.drawText(Fonts.RUBIK.get(), countText, posX + 21.0f, y - 0.05f, owner.getAxiomPrimaryTextColor(255), countSize);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), label, posX + 21.0f, y + 6.5f, owner.getAxiomSecondaryTextColor(255), labelSize);
    }
}