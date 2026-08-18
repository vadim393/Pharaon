package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

/**
 * Броня в стиле Hud3: плавающий ряд предметов брони без панели и заголовка,
 * с покадровым появлением каждого слота и процентами прочности под предметом.
 */
public class ArmorRenderer {
    private static final ItemStack[] PREVIEW = {
            new ItemStack(Items.NETHERITE_HELMET),
            new ItemStack(Items.NETHERITE_CHESTPLATE),
            new ItemStack(Items.NETHERITE_LEGGINGS),
            new ItemStack(Items.NETHERITE_BOOTS)
    };
    private static final float ITEM_SCALE = 0.75f;
    private static final float ITEM_SPACING = 18f;
    private static final float PANEL_HEIGHT = 22f;

    private final Interface owner;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation[] slotAnim = {
            new Animation(Easing.EXPO_OUT, 350),
            new Animation(Easing.EXPO_OUT, 350),
            new Animation(Easing.EXPO_OUT, 350),
            new Animation(Easing.EXPO_OUT, 350)
    };
    private final boolean[] wasEmpty = {true, true, true, true};
    private final Animation panelAnim = new Animation(Easing.EXPO_OUT, 350);

    public ArmorRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        Draggable drag = owner.getArmorDrag();
        float x = drag.getX();
        float y = drag.getY();

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context, x, y, drag);
            return;
        }

        renderClassic(context, x, y, drag);
    }

    private void renderPouchOld(DrawContext context, float x, float y, Draggable drag) {
        ItemStack[] armor = getArmorSlots();

        for (int i = 0; i < 4; i++) {
            boolean empty = armor[i].isEmpty();
            if (empty != wasEmpty[i]) {
                slotAnim[i].run(empty ? 0f : 1f);
                wasEmpty[i] = empty;
            }
        }

        boolean anySlotVisible = false;
        for (Animation a : slotAnim) {
            if (a.getValue() > 0.01f) {
                anySlotVisible = true;
                break;
            }
        }

        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        boolean preview = !anySlotVisible && chatOpen;

        panelAnim.run(preview ? 1f : 0f);
        alpha.run((!anySlotVisible && !preview) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        float previewPanel = (float) panelAnim.getValue();
        int visibleCount = preview ? PREVIEW.length : 0;
        if (!preview) {
            for (Animation a : slotAnim) {
                if (a.getValue() > 0.01f) visibleCount++;
            }
        }

        float width = Math.max(24f, visibleCount * ITEM_SPACING + 7f);

        if (preview) {
            float pa = Math.min(1f, previewPanel * globalAlpha);
            if (pa > 0.01f) {
                float itemX = x + 4f;
                for (ItemStack stack : PREVIEW) {
                    drawArmorItem(context, stack, itemX + 1.7f, y + 5f, pa, false);
                    itemX += ITEM_SPACING;
                }
            }
        } else {
            float itemX = x + 4f;
            for (int i = 0; i < 4; i++) {
                float a = Math.min(1f, (float) slotAnim[i].getValue() * globalAlpha);
                if (a < 0.01f) continue;
                drawArmorItem(context, armor[i], itemX + 1.7f, y + 5f, a, true);
                itemX += ITEM_SPACING;
            }
        }

        drag.setWidth(width);
        drag.setHeight(PANEL_HEIGHT);
    }

    private void drawArmorItem(DrawContext context, ItemStack stack, float x, float y, float alpha, boolean durability) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
        context.getMatrices().pop();

        if (durability && stack.isDamageable() && stack.getMaxDamage() > 0) {
            int percent = Math.round((1f - stack.getDamage() / (float) stack.getMaxDamage()) * 100f);
            String text = String.valueOf(Math.max(0, percent));
            float tw = Fonts.SFMEDIUM.get().getWidth(text, 4.5f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x + 6f - tw * 0.5f, y + 10f,
                    ColorProvider.rgba(255, 255, 255, (int) (255 * alpha)), 6f);
        }
    }

    private void renderClassic(DrawContext context, float x, float y, Draggable drag) {
        float width = 70f;
        float height = 30f;

        DrawUtil.drawRound(x, y, width, height, 3f, ColorProvider.rgba(20, 22, 28, 145));

        ItemStack[] armor = getArmorSlots();
        float slotX = x + 3f;
        float slotY = y + 4f;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;
            context.getMatrices().push();
            context.getMatrices().translate(slotX + 1f, slotY + 1f, 0);
            context.getMatrices().scale(0.65f, 0.65f, 0.65f);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
            slotX += 16.5f;
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private ItemStack[] getArmorSlots() {
        ItemStack[] armor = new ItemStack[4];
        armor[0] = owner.mc.player.getInventory().armor.get(3);
        armor[1] = owner.mc.player.getInventory().armor.get(2);
        armor[2] = owner.mc.player.getInventory().armor.get(1);
        armor[3] = owner.mc.player.getInventory().armor.get(0);
        return armor;
    }
}
