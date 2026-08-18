package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Стрелы в стиле Hud3: панель с заголовком и строками (иконка + название +
 * количество) для каждого типа стрел в инвентаре.
 */
public class ArrowsRenderer {
    private static final float ITEM_SCALE = 0.5f;
    private static final float ICON_SIZE = 11f;
    private static final float ROW_HEIGHT = Hud3Style.ITEM_SPACING;

    private final Interface owner;

    public ArrowsRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        Draggable drag = owner.getArrowsDrag();
        float x = drag.getX();
        float y = drag.getY();

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context, x, y, drag);
            return;
        }

        renderClassic(context, x, y, drag);
    }

    private void renderPouchOld(DrawContext context, float x, float y, Draggable drag) {
        Map<Item, Integer> counts = collectArrows();
        if (counts.isEmpty()) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float headerHeight = Hud3Style.HEADER_HEIGHT;
        float targetWidth = 90f;
        for (Item item : counts.keySet()) {
            String name = item.getDefaultStack().getName().getString();
            targetWidth = Math.max(targetWidth, 16f + Fonts.SFMEDIUM.get().getWidth(name, 6.75f) + Fonts.SFMEDIUM.get().getWidth("999", 6.75f) + 14f);
        }
        float currentWidth = targetWidth;
        float totalHeight = headerHeight + counts.size() * ROW_HEIGHT + 2f;

        Hud3Style.drawPanel(x, y, currentWidth, totalHeight, true, 1f);
        Hud3Style.drawHeader(x, y, currentWidth, "Arrows", "F", 1f);

        int theme = ColorProvider.getThemeColor();
        float rowY = y + headerHeight + 1.5f;
        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            ItemStack stack = entry.getKey().getDefaultStack();
            String name = stack.getName().getString();
            String countText = String.valueOf(entry.getValue());

            DrawUtil.drawRound(x + 3.75f, rowY + 1.25f, ICON_SIZE, ICON_SIZE, 2.5f, ColorProvider.setAlpha(Hud3Style.BG, 160));
            Builder.border()
                    .size(new SizeState(ICON_SIZE + 0.5f, ICON_SIZE + 0.25f))
                    .radius(new QuadRadiusState(2.5f))
                    .color(new QuadColorState(ColorProvider.setAlpha(Hud3Style.BORDER, 150)))
                    .thickness(0.5f)
                    .smoothness(0.5f, 1f)
                    .build()
                    .render(x + 3.75f, rowY + 1.25f);

            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 100);
            context.getMatrices().push();
            context.getMatrices().translate(x + 3.75f + 1.5f, rowY + 1.25f + 1.5f, 0);
            context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
            context.getMatrices().pop();

            float nameWidth = Fonts.SFMEDIUM.get().getWidth(name, 6.75f);
            float countWidth = Fonts.SFMEDIUM.get().getWidth(countText, 6.75f);
            float gap = 10f;
            float blockW = nameWidth + gap + countWidth;
            float textBlockX = Math.max(x + 18.5f, x + (currentWidth - blockW) / 2f);

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textBlockX, rowY + 2.5f, Hud3Style.TEXT, 6.75f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), countText, textBlockX + nameWidth + gap, rowY + 2.5f,
                    ColorProvider.setAlpha(theme, 255), 6.75f);

            DrawUtil.drawRound(x + 6f, rowY + ROW_HEIGHT - 0.75f, currentWidth - 12f, 0.5f, 0.25f,
                    ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (55f)));

            rowY += ROW_HEIGHT;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void renderClassic(DrawContext context, float x, float y, Draggable drag) {
        Map<Item, Integer> counts = collectArrows();
        if (counts.isEmpty()) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float width = 60f;
        float height = counts.size() * 10f + 2f;

        float rowY = y;
        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            String text = entry.getKey().getDefaultStack().getName().getString() + " x" + entry.getValue();
            DrawUtil.drawText(Fonts.SFREGULAR.get(), text, x + 3f, rowY, -1, 6.5f);
            rowY += 10f;
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private Map<Item, Integer> collectArrows() {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < owner.mc.player.getInventory().size(); i++) {
            ItemStack stack = owner.mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.ARROW || stack.getItem() == Items.SPECTRAL_ARROW || stack.getItem() == Items.TIPPED_ARROW) {
                result.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return result;
    }
}