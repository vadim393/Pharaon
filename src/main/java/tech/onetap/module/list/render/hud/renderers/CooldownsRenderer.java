package tech.onetap.module.list.render.hud.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class CooldownsRenderer {
    private static final float ANIMATION_SPEED = 8.0f;
    private static final float ITEM_SCALE = 0.5f;
    private static final String TIMER_TEMPLATE = "00:00";
    private static final float ARC_SIZE = 9f;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    private final Interface owner;
    private final Map<Item, CoolDownInfo> cooldownMap = new LinkedHashMap<>();
    private final Map<Item, Animation> cooldownAnimations = new LinkedHashMap<>();
    private final Set<Item> activeCooldowns = new HashSet<>();
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation heightAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private long lastItemChange = 0;
    private int currentItemIndex = 0;

    public CooldownsRenderer(Interface owner) {
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
        updateActiveCooldowns();
        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        alpha.run((activeCooldowns.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        float headerHeight = 15f;
        float itemSpacing = 11f;
        float minWidth = 60f;

        float targetWidth = minWidth;
        boolean preview = chatOpen && activeCooldowns.isEmpty();
        if (preview) {
            String name = "Sugar";
            String time = "**:**";
            targetWidth = Math.max(targetWidth,
                    Fonts.SFMEDIUM.get().getWidth(name, 6f) + Fonts.SFMEDIUM.get().getWidth(time, 6f) + 30f);
        } else {
            for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
                float animation = (float) entry.getValue().getValue();
                if (animation <= 0.001f) continue;
                Item item = entry.getKey();
                String name = item.getDefaultStack().getName().getString();
                String time = cooldownTime(item);
                targetWidth = Math.max(targetWidth,
                        Fonts.SFMEDIUM.get().getWidth(name, 6f) + Fonts.SFMEDIUM.get().getWidth(time, 6f) + 30f);
            }
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());

        float rows = 0f;
        if (preview) {
            rows = 1f;
        } else {
            for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
                rows += (float) entry.getValue().getValue();
            }
        }
        rows = Math.max(1f, rows);
        float targetHeight = Math.max(20f, headerHeight + rows * itemSpacing);
        heightAnim.run(targetHeight);
        float totalHeight = (float) heightAnim.getValue();

        Draggable drag = owner.getCooldownsDrag();
        float x = drag.getX();
        float y = drag.getY();

        Hud3Style.drawPanel(x, y, currentWidth, totalHeight, true, globalAlpha);
        Hud3Style.drawHeader(x, y, currentWidth, "Cooldowns", "T", globalAlpha);

        float rowY = y + headerHeight;
        if (preview) {
            drawPouchCooldownRow(context, x, rowY, currentWidth, Items.SUGAR.getDefaultStack(),
                    "Sugar", "**:**", globalAlpha);
        } else {
            for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
                float anim = (float) entry.getValue().getValue();
                if (anim <= 0.001f) continue;
                Item item = entry.getKey();
                CoolDownInfo info = cooldownMap.get(item);
                if (info == null) continue;
                ItemStack stack = item.getDefaultStack();
                String name = stack.getName().getString();
                String duration = cooldownTime(item);
                drawPouchCooldownRow(context, x, rowY + (1f - anim) * 4f, currentWidth, stack,
                        name, duration, globalAlpha * anim);
                rowY += itemSpacing * anim;
            }
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private String cooldownTime(Item item) {
        CoolDownInfo info = cooldownMap.get(item);
        if (info == null) return "**:**";
        float progress = owner.mc.player.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0.0f);
        int seconds = info.getDisplaySeconds(progress);
        if (seconds < 0) return "**:**";
        int minutes = Math.min(99, seconds / 60);
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void drawPouchCooldownRow(DrawContext context, float x, float rowY, float currentWidth,
                                      ItemStack stack, String name, String duration, float rowAlpha) {
        if (rowAlpha <= 0.01f) return;
        int alpha = (int) (255 * rowAlpha);
        float itemSize = 8f;
        float itemX = x + 2.5f;
        float itemY = rowY + 1f;
        float nameX = itemX + itemSize + 3f;
        float nameW = Fonts.SFMEDIUM.get().getWidth(name, 6f);
        float timeW = Fonts.SFMEDIUM.get().getWidth(duration, 6f);
        float gap = 12f;
        float blockW = nameW + gap + timeW;
        float blockX = Math.max(nameX, x + (currentWidth - blockW) / 2f);
        int col = ColorProvider.rgba(255, 255, 255, alpha);

        if (!stack.isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 100);
            context.getMatrices().push();
            context.getMatrices().translate(itemX, itemY, 0);
            context.getMatrices().scale(0.5f, 0.5f, 1f);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
            context.getMatrices().pop();
        }
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, blockX, rowY + 4.5f, col, 6f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), duration, blockX + nameW + gap, rowY + 4.5f, col, 6f);
    }

    private void drawExp4Arc(Matrix4f matrix, float cx, float cy, float radius, float thickness, float degrees, int color) {
        if (degrees <= 0f) return;
        float start = -90f;
        float end = start + Math.min(360f, degrees);
        int segments = Math.max(4, (int) (degrees / 4f));
        float inner = Math.max(0.01f, radius - thickness / 2f);
        float outer = radius + thickness / 2f;

        DrawUtil.drawSetup();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(start + (end - start) * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.vertex(matrix, cx + cos * inner, cy + sin * inner, 0).color(color);
            builder.vertex(matrix, cx + cos * outer, cy + sin * outer, 0).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
        DrawUtil.drawEnd();
    }

    private void renderExp4_0(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        updateActiveCooldowns();

        boolean showPlaceholder = chatOpen && activeCooldowns.isEmpty();
        alpha.run((activeCooldowns.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        int bgAlpha = (int) (255 * globalAlpha);

        float fixedTimerWidth = Fonts.SFBOLD.get().getWidth(TIMER_TEMPLATE, 6f);
        float timerBoxWidth = fixedTimerWidth + 4f;

        float offset = 23f;
        float targetWidth = 80f;

        if (!activeCooldowns.isEmpty()) {
            for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
                float animation = (float) entry.getValue().getValue();
                if (animation <= 0.001f) continue;
                offset += animation * 11f;
                String name = entry.getKey().getDefaultStack().getName().getString();
                targetWidth = Math.max(Fonts.SFBOLD.get().getWidth(name, 6f) + fixedTimerWidth + 55f, targetWidth);
            }
        } else {
            offset += 11f;
            targetWidth = Math.max(Fonts.SFBOLD.get().getWidth("Example CoolDown", 6f) + fixedTimerWidth + 55f, targetWidth);
        }

        float targetHeight = offset + 2f;

        widthAnim.run(targetWidth);
        heightAnim.run(targetHeight);
        float currentWidth = (float) widthAnim.getValue();
        float currentHeight = (float) heightAnim.getValue();

        Draggable drag = owner.getCooldownsDrag();
        float x = drag.getX();
        float y = drag.getY();

        DrawUtil.drawRound(x, y, currentWidth, currentHeight, 5f,
                ColorProvider.rgba(52, 52, 52, bgAlpha),
                ColorProvider.rgba(32, 32, 32, bgAlpha),
                ColorProvider.rgba(52, 52, 52, bgAlpha),
                ColorProvider.rgba(32, 32, 32, bgAlpha));
        Builder.border()
                .size(new SizeState(currentWidth + 0.5f, currentHeight + 0.25f))
                .radius(new QuadRadiusState(5f))
                .color(new QuadColorState(ColorProvider.rgba(90, 90, 90, bgAlpha)))
                .thickness(0.35f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);

        Scissor.push();
        Scissor.setFromComponentCoordinates((int) x, (int) y, (int) currentWidth, (int) currentHeight);

        DrawUtil.drawRound(x + currentWidth - 22.5f, y + 5f, 14f, 12f, 3f, ColorProvider.rgba(52, 52, 52, bgAlpha));
        DrawUtil.drawText(Fonts.ICONS2.get(), "D", x + currentWidth - 20f, y + 6.5f, ColorProvider.rgba(165, 165, 165, bgAlpha), 9f);
        DrawUtil.drawText(Fonts.SFBOLD.get(), "CoolDowns", x + 8f, y + 6.5f, ColorProvider.rgba(255, 255, 255, bgAlpha), 6f);

        float moduleOffset = 23f;
        float fixedTimerBoxX = x + currentWidth - timerBoxWidth - 9.5f;

        if (activeCooldowns.isEmpty()) {
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            if (chatOpen) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastItemChange >= 1000) {
                    currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                    lastItemChange = currentTime;
                }
            }
            drawExp4TimerBox(context, fixedTimerBoxX, y + moduleOffset - 1f, timerBoxWidth, bgAlpha);
            drawExp4Item(context, item.getDefaultStack(), x + 8f, y + moduleOffset - 1f, bgAlpha);
            DrawUtil.drawText(Fonts.SFBOLD.get(), "Example CoolDown", x + 20f, y + moduleOffset - 1f, ColorProvider.rgba(255, 255, 255, bgAlpha), 6f);
            drawExp4TimerText(fixedTimerBoxX + (timerBoxWidth - Fonts.SFBOLD.get().getWidth("0:00", 6f)) / 2f, y + moduleOffset, "0:00", bgAlpha);
        } else {
            for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
                float animation = (float) entry.getValue().getValue();
                if (animation <= 0.001f) continue;
                Item item = entry.getKey();
                CoolDownInfo info = cooldownMap.get(item);
                if (info == null) continue;

                float currentProgress = owner.mc.player.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0.0f);
                String name = item.getDefaultStack().getName().getString();
                int remainingSeconds = info.getDisplaySeconds(currentProgress);
                String duration = formatDuration(remainingSeconds);

                int textAlpha = (int) (255 * animation * globalAlpha);
                int rowAlpha = MathHelper.clamp((int) (255 * animation * globalAlpha), 0, 255);

                drawExp4TimerBox(context, fixedTimerBoxX, y + moduleOffset - 1f, timerBoxWidth, rowAlpha);
                drawExp4Item(context, item.getDefaultStack(), x + 8f, y + moduleOffset - 1f, (int) (animation * 255));
                DrawUtil.drawText(Fonts.SFBOLD.get(), name, x + 20f, y + moduleOffset - 0.5f, ColorProvider.rgba(255, 255, 255, textAlpha), 6f);
                drawExp4TimerText(fixedTimerBoxX + (timerBoxWidth - Fonts.SFBOLD.get().getWidth(duration, 6f)) / 2f, y + moduleOffset, duration, textAlpha);

                moduleOffset += animation * 11f;
            }
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(currentHeight);
    }

    private void drawExp4TimerBox(DrawContext context, float x, float y, float width, int alpha) {
        DrawUtil.drawRound(x, y, width, 9f, 3f, ColorProvider.rgba(52, 52, 52, Math.max(8, alpha)));
        Builder.border()
                .size(new SizeState(width + 0.5f, 9f + 0.25f))
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(ColorProvider.rgba(132, 132, 132, Math.max(8, alpha))))
                .thickness(0.05f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
    }

    private void drawExp4TimerText(float x, float y, String duration, int alpha) {
        DrawUtil.drawText(Fonts.SFBOLD.get(), duration, x + 1f, y, ColorProvider.rgba(165, 165, 165, Math.max(8, alpha)), 6f);
    }

    private void drawExp4Item(DrawContext context, ItemStack stack, float x, float y, int alpha) {
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

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getCooldownsDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        boolean isFound = !activeCooldowns.isEmpty();
        if (isFound) alpha.run(1);
        if (!isFound && !(owner.mc.currentScreen instanceof ChatScreen)) alpha.run(0);
        if (owner.mc.currentScreen instanceof ChatScreen) alpha.run(1);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;
        int headerAlpha = MathHelper.clamp((int) (255 * globalAlpha), 0, 255);

        DrawUtil.drawRound(posX, posY, (float) widthAnim.getValue(), 14.5f, 3f, ColorProvider.rgba(15, 15, 15, headerAlpha));
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Cooldowns", posX + 4f, posY + 4f, ColorProvider.rgba(255, 255, 255, headerAlpha), 6.5f);
        drag.setWidth((float) widthAnim.getValue());
        drag.setHeight(14.5f);
    }

    private void updateActiveCooldowns() {
        if (owner.mc.player == null) {
            activeCooldowns.clear();
            return;
        }

        activeCooldowns.clear();
        Set<Item> checkedItems = new HashSet<>();

        for (int i = 0; i < owner.mc.player.getInventory().size(); i++) {
            ItemStack stack = owner.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && !checkedItems.contains(stack.getItem())) {
                checkedItems.add(stack.getItem());
                checkAndUpdateCooldown(stack.getItem());
            }
        }

        ItemStack mainHand = owner.mc.player.getMainHandStack();
        if (!mainHand.isEmpty() && !checkedItems.contains(mainHand.getItem())) {
            checkAndUpdateCooldown(mainHand.getItem());
        }
        ItemStack offHand = owner.mc.player.getOffHandStack();
        if (!offHand.isEmpty() && !checkedItems.contains(offHand.getItem())) {
            checkAndUpdateCooldown(offHand.getItem());
        }

        List<Item> toRemove = new ArrayList<>();
        for (Map.Entry<Item, Animation> entry : cooldownAnimations.entrySet()) {
            Item item = entry.getKey();
            float targetAnim = activeCooldowns.contains(item) ? 1f : 0f;
            entry.getValue().run(targetAnim);
            if ((float) entry.getValue().getValue() <= 0.01f && targetAnim == 0f) {
                toRemove.add(item);
            }
        }
        for (Item item : toRemove) {
            cooldownAnimations.remove(item);
            cooldownMap.remove(item);
        }
    }

    private void checkAndUpdateCooldown(Item item) {
        if (owner.mc.player == null) return;

        var cooldownManager = owner.mc.player.getItemCooldownManager();
        ItemStack stack = item.getDefaultStack();

        if (cooldownManager.isCoolingDown(stack)) {
            float progress = cooldownManager.getCooldownProgress(stack, 0.0f);
            activeCooldowns.add(item);

            CoolDownInfo info = cooldownMap.get(item);
            if (info == null) {
                info = new CoolDownInfo(item, progress);
                cooldownMap.put(item, info);
            } else {
                info.updateEstimate(progress);
            }

            if (!cooldownAnimations.containsKey(item)) {
                cooldownAnimations.put(item, new Animation(Easing.EXPO_OUT, 125));
            }
        }
    }

    private String formatDuration(int seconds) {
        if (seconds < 0) return "...";
        if (seconds == 0) return "0:00";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private static class CoolDownInfo {
        Item item;
        long startTime;
        float startProgress;
        long estimatedTotalMs;
        int displaySeconds = -1;
        long nextTickTime = 0;
        boolean estimateReady = false;

        CoolDownInfo(Item item, float progress) {
            this.item = item;
            this.startTime = System.currentTimeMillis();
            this.startProgress = progress;
        }

        void updateEstimate(float currentProgress) {
            if (estimateReady) return;

            long now = System.currentTimeMillis();
            long elapsed = now - startTime;
            if (elapsed < 200) return;

            if (startProgress > currentProgress && startProgress > 0.01f) {
                float progressConsumed = startProgress - currentProgress;
                if (progressConsumed > 0.01f) {
                    estimatedTotalMs = (long) (elapsed / progressConsumed);
                    long remainingMs = (long) (currentProgress * estimatedTotalMs);
                    displaySeconds = (int) Math.ceil(remainingMs / 1000.0);
                    nextTickTime = now + 1000;
                    estimateReady = true;
                }
            }
        }

        int getDisplaySeconds(float currentProgress) {
            if (currentProgress <= 0) {
                displaySeconds = 0;
                return 0;
            }
            if (!estimateReady) return -1;

            long now = System.currentTimeMillis();
            if (now >= nextTickTime && nextTickTime > 0) {
                displaySeconds = Math.max(0, displaySeconds - 1);
                nextTickTime = now + 1000;

                int calculatedSeconds;
                if (estimatedTotalMs > 0) {
                    long remainingMs = (long) (currentProgress * estimatedTotalMs);
                    calculatedSeconds = (int) Math.ceil(remainingMs / 1000.0);
                } else {
                    calculatedSeconds = displaySeconds;
                }
                if (Math.abs(displaySeconds - calculatedSeconds) > 2) {
                    displaySeconds = calculatedSeconds;
                }
            }
            return Math.max(0, displaySeconds);
        }
    }
}
