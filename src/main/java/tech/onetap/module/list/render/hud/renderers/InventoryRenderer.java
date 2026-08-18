package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.FontModeController;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class InventoryRenderer {
    private static final int SLOT_SIZE = 12;
    private static final int SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final float ITEM_SCALE = 0.5f;

    private final Interface owner;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private int filledSlots = 0;

    public InventoryRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context);
        } else if (owner.getHudStyleSetting().is("Exp4.0")) {
            renderExp4_0(context);
        } else {
            renderClassic(context);
        }
    }

    private void renderPouchOld(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        updateFilledSlots();

        alpha.run((filledSlots == 0 && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        int aInt = MathHelper.clamp((int) (255 * globalAlpha), 0, 255);
        if (globalAlpha <= 0.05f || aInt < 4) return;

        float headerHeight = 15f;
        float slotHeight = 16f;
        float panelWidth = 144f;
        float slotsHeight = 3 * slotHeight;
        float totalHeight = headerHeight + 1.5f + slotsHeight;

        Draggable drag = owner.getInventoryDrag();
        float x = drag.getX();
        float y = drag.getY();

        int themeCol = ColorProvider.getThemeColor();
        int bgColor = ColorProvider.rgba(0, 0, 0, aInt);

        DrawUtil.drawRoundBlur(x, y, panelWidth, totalHeight, 5f, bgColor, 15f);
        DrawUtil.drawRound(x, y, panelWidth, headerHeight, new Vector4f(5f, 5f, 0f, 0f), bgColor);

        String title = "Inventory";
        float charX = x + 4.5f;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), ch, charX, y + (headerHeight - 7f) * 0.5f - 0.5f,
                    ColorProvider.setAlpha(themeCol, aInt), 7f);
            charX += Fonts.SFMEDIUM.get().getWidth(ch, 7f);
        }

        float iconWidth = Fonts.ICONS_NURIK.get().getWidth("A", 10f);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "A",
                x + panelWidth - iconWidth - 4.5f, y + (headerHeight - 10f) * 0.5f + 0.1f,
                ColorProvider.setAlpha(themeCol, aInt), 10f);

        float slotY = y + headerHeight + 1.5f;
        int separatorColor = ColorProvider.rgba(255, 255, 255, (int) (245 * globalAlpha));
        for (int col = 1; col < 9; col++) {
            float lineX = x + col * 16f - 0.5f;
            DrawUtil.drawRound(lineX, slotY + 2f, 0.5f, slotHeight - 4f, 0f, separatorColor);
        }

        if (chatOpen && filledSlots == 0) {
            drawPouchItem(context, new ItemStack(Items.ENDER_PEARL), x + 4f, slotY + 4f);
            drawPouchItem(context, new ItemStack(Items.GOLDEN_APPLE), x + 20f, slotY + 4f);
            drawPouchItem(context, new ItemStack(Items.TOTEM_OF_UNDYING), x + 36f, slotY + 4f);
            drawPouchItem(context, new ItemStack(Items.SUGAR), x + 52f, slotY + 4f);
        } else {
            for (int slotIndex = 9; slotIndex < 36; slotIndex++) {
                ItemStack stack = owner.mc.player.getInventory().getStack(slotIndex);
                if (stack.isEmpty()) continue;
                int localIndex = slotIndex - 9;
                float itemX = x + localIndex % 9 * 16f + 8f - 4f;
                float itemY = slotY + localIndex / 9 * slotHeight + 8f - 4f;
                drawPouchItem(context, stack, itemX, itemY);
            }
        }

        drag.setWidth(panelWidth);
        drag.setHeight(totalHeight);
    }

    private void drawPouchItem(DrawContext context, ItemStack stack, float x, float y) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
        context.drawItem(stack, 0, 0);
        context.drawStackOverlay(owner.mc.textRenderer, stack, 0, 0);
        context.getMatrices().pop();
        context.getMatrices().pop();
    }

    private void renderExp4_0(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        updateFilledSlots();

        alpha.run((filledSlots == 0 && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        int bgAlpha = (int) (255 * globalAlpha);

        float padding = 6f;
        float slotGap = 1f;

        float slotsWidth = SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * slotGap;
        float slotsHeight = INVENTORY_ROWS * SLOT_SIZE + (INVENTORY_ROWS - 1) * slotGap;

        float contentWidth = slotsWidth + padding * 2f;
        float contentHeight = slotsHeight + padding * 2f;

        Draggable drag = owner.getInventoryDrag();
        float x = drag.getX();
        float y = drag.getY();

        float contentY = y;

        DrawUtil.drawRound(x + 2f, contentY + 2f, contentWidth - 4f, contentHeight - 4f, 5f,
                ColorProvider.rgba(52, 52, 52, bgAlpha),
                ColorProvider.rgba(32, 32, 32, bgAlpha),
                ColorProvider.rgba(52, 52, 52, bgAlpha),
                ColorProvider.rgba(32, 32, 32, bgAlpha));
        Builder.border()
                .size(new SizeState(contentWidth - 4f + 0.5f, contentHeight - 4f + 0.25f))
                .radius(new QuadRadiusState(5f))
                .color(new QuadColorState(ColorProvider.rgba(90, 90, 90, bgAlpha)))
                .thickness(0.35f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x + 2f, contentY + 2f);

        float slotsStartX = x + padding;
        float slotsStartY = contentY + padding;

        List<CountLabel> countLabels = new ArrayList<>();

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = 9 + row * SLOTS_PER_ROW + col;

                float slotX = slotsStartX + col * (SLOT_SIZE + slotGap);
                float slotY = slotsStartY + row * (SLOT_SIZE + slotGap);

                ItemStack stack = owner.mc.player.getInventory().getStack(slotIndex);

                DrawUtil.drawRound(slotX, slotY, SLOT_SIZE, SLOT_SIZE, 2f, ColorProvider.rgba(28, 28, 28, bgAlpha));

                if (!stack.isEmpty()) {
                    float itemSize = 16 * ITEM_SCALE;
                    float itemX = slotX + (SLOT_SIZE - itemSize) / 2f;
                    float itemY = slotY + (SLOT_SIZE - itemSize) / 2f;

                    drawInventoryItem(context, stack, itemX, itemY, aInt);

                    int count = stack.getCount();
                    if (count > 1) {
                        countLabels.add(new CountLabel(slotX, slotY, count));
                    }
                }
            }
        }

        int textAlpha = aInt;
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        MsdfFont countFont = Fonts.SFMEDIUM.get();
        float countSize = 6.0f;
        for (CountLabel label : countLabels) {
            String countText = String.valueOf(label.count);
            float textWidth = countFont.getWidth(countText, countSize);
            float textX = label.slotX + SLOT_SIZE - textWidth - 1.0f;
            float textY = label.slotY + SLOT_SIZE - 5.0f;
            DrawUtil.drawText(countFont, countText, textX, textY, textColor, countSize);
        }

        drag.setWidth(contentWidth);
        drag.setHeight(contentHeight + 4f);
    }

    private void drawInventoryItem(DrawContext context, ItemStack stack, float x, float y, int alpha) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();

        if (FontModeController.isCustom()) {
            int count = stack.getCount();
            if (count > 1) {
                MsdfFont font = Fonts.SFMEDIUM.get();
                float size = 5.0f;
                String countText = String.valueOf(count);
                float countW = font.getWidth(countText, size);
                DrawUtil.drawText(font, countText, x + 16.0f * ITEM_SCALE - countW - 1.0f, y + 16.0f * ITEM_SCALE - 6.0f, ColorProvider.rgba(255, 255, 255, alpha), size);
            }
        } else {
            context.drawStackOverlay(owner.mc.textRenderer, stack, 0, 0);
        }

        context.getMatrices().pop();
    }

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getInventoryDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        DrawUtil.drawRound(posX, posY, 14.5f, 14.5f, 3f, ColorProvider.rgba(15, 15, 15, 200));
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Inv", posX + 4f, posY + 4f, ColorProvider.rgba(255, 255, 255, 255), 6.5f);
        drag.setWidth(14.5f);
        drag.setHeight(14.5f);
    }

    private void updateFilledSlots() {
        if (owner.mc.player == null) {
            filledSlots = 0;
            return;
        }
        filledSlots = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = owner.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                filledSlots++;
            }
        }
    }

    private record CountLabel(float slotX, float slotY, int count) {
    }
}
