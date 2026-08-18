package tech.onetap.module.list.render.hud.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PotionsRenderer {
    private final Interface owner;
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation xLine = new Animation(Easing.EXPO_OUT, 170);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation potionsEmptyAnim = new Animation(Easing.EXPO_OUT, 233);
    private final List<PotionItem> potionItems = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> potionMaxDurations = new HashMap<>();

    public PotionsRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context);
        } else if (owner.isAxiomStyle()) {
            renderAxiom(context);
        } else if (owner.getHudStyleSetting().is("Celestial")) {
            renderCelestial(context);
        } else if (owner.getHudStyleSetting().is("Pharaon")) {
            renderMoonward(context);
        } else if (owner.getHudStyleSetting().is("Exp4.0")) {
            renderExp4_0(context);
        } else {
            renderClassic(context);
        }
    }

    private void renderPouchOld(DrawContext context) {
        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) visible.add(item);
        }

        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        boolean showPlaceholder = chatOpen && visible.isEmpty();
        potionsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((visible.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        float emptyAnimVal = MathHelper.clamp((float) potionsEmptyAnim.getValue(), 0f, 1f);
        float previewRowCount = showPlaceholder ? emptyAnimVal * 2f : 0f;
        float headerHeight = 15f;
        float itemSpacing = 11f;
        float minWidth = 60f;

        float targetWidth = minWidth;
        for (PotionItem item : visible) {
            String name = pouchEffectName(item);
            String time = pouchEffectTime(item.durationTicks);
            targetWidth = Math.max(targetWidth,
                    Fonts.SFMEDIUM.get().getWidth(name, 6f) + Fonts.SFMEDIUM.get().getWidth(time, 6f) + 30f);
        }
        if (previewRowCount > 0.001f) {
            targetWidth = Math.max(targetWidth,
                    Fonts.SFMEDIUM.get().getWidth("Speed", 6f) + Fonts.SFMEDIUM.get().getWidth("**:**", 6f) + 30f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());

        float rowCount = (float) visible.stream()
                .mapToDouble(item -> MathHelper.clamp((float) item.animation.getValue(), 0f, 1f))
                .sum();
        rowCount += previewRowCount;
        rowCount = Math.max(1f, rowCount);
        float totalHeight = Math.max(20f, headerHeight + rowCount * itemSpacing);

        Draggable drag = owner.getPotionsDrag();
        float x = drag.getX();
        float y = drag.getY();

        Hud3Style.drawPanel(x, y, currentWidth, totalHeight, true, globalAlpha);
        Hud3Style.drawHeader(x, y, currentWidth, "Potions", "E", globalAlpha);

        float currentY = y + headerHeight;
        for (PotionItem item : visible) {
            float rowAnim = MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;
            drawPouchPotionRow(context, x, currentY + (1f - rowAnim) * 4f, currentWidth, item, globalAlpha * rowAnim);
            currentY += itemSpacing * rowAnim;
        }

        if (previewRowCount > 0.001f) {
            drawPouchPotionRow(context, x, currentY, currentWidth, null, globalAlpha * emptyAnimVal);
            currentY += itemSpacing * emptyAnimVal;
            drawPouchPotionRow(context, x, currentY, currentWidth, new PotionItem("Poison", 0, -1, StatusEffects.POISON), globalAlpha * emptyAnimVal);
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawPouchPotionRow(DrawContext context, float x, float rowY, float w, PotionItem item, float rowAlpha) {
        if (rowAlpha <= 0.01f) return;
        float iconSize = 9f;
        float iconX = x + 2.5f;
        float iconY = rowY + 1f;

        Sprite sprite = item == null || item.effect == null
                ? owner.mc.getStatusEffectSpriteManager().getSprite(StatusEffects.SPEED)
                : owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
        if (sprite != null) {
            RenderSystem.setShaderColor(1f, 1f, 1f, rowAlpha);
            context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite,
                    (int) iconX, (int) iconY, (int) iconSize, (int) iconSize,
                    (int) (245 * rowAlpha) << 24 | 0xFFFFFF);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        boolean harmful = item != null && item.effect != null && item.effect.value() != null
                && item.effect.value().getCategory() == StatusEffectCategory.HARMFUL;
        float pulseVal = 1f;
        if (item != null && item.durationTicks > 0 && item.durationTicks <= 120) {
            pulseVal = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 150.0);
        }
        int baseCol = harmful ? ColorProvider.rgba(255, 85, 85, 255) : ColorProvider.rgba(255, 255, 255, 255);
        int col = ColorProvider.setAlpha(baseCol, MathHelper.clamp((int) (255 * rowAlpha * pulseVal), 0, 255));

        String name = item == null ? "Speed" : pouchEffectName(item);
        String time = item == null ? "**:**" : pouchEffectTime(item.durationTicks);
        float nameW = Fonts.SFMEDIUM.get().getWidth(name, 6f);
        float timeW = Fonts.SFMEDIUM.get().getWidth(time, 6f);
        float gap = 12f;
        float blockW = nameW + gap + timeW;
        float iconRight = x + 2.5f + iconSize + 3f;
        float blockX = Math.max(iconRight, x + (w - blockW) / 2f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, blockX, rowY + 2f, col, 6f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), time, blockX + nameW + gap, rowY + 2f, col, 6f);
    }

    private String pouchEffectName(PotionItem item) {
        return item.amplifier >= 1 ? item.name + " " + (item.amplifier + 1) : item.name;
    }

    private String pouchEffectTime(int ticks) {
        if (ticks < 0 || ticks > 72000) return "**:**";
        int totalSec = Math.max(0, ticks / 20);
        int minutes = Math.min(99, totalSec / 60);
        int secs = totalSec % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void renderAxiom(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) {
                visible.add(item);
            }
        }

        boolean showPlaceholder = chatOpen && visible.isEmpty();
        potionsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((visible.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) potionsEmptyAnim.getValue(), 0f, 1f);
        float headerHeight = 18.0f;
        float rowHeight = 18.0f;
        float rowInset = 4.0f;
        float minWidth = 108.0f;
        String titleText = "STATUS FX";
        String subtitleText = visible.isEmpty() ? "no active effects" : visible.size() + " effects";
        String placeholderText = "No active effects";
        float nameSize = 6.95f;
        float metaSize = 5.7f;

        float targetWidth = minWidth;
        for (PotionItem item : visible) {
            String timeText = formatPotionTime(item.durationTicks);
            String levelText = getPotionLevelLabel(item.amplifier);
            float rowWidth = 8.0f + 12.0f + 6.0f
                    + Math.max(Fonts.RUBIK.get().getWidth(item.name, nameSize), Fonts.SFMEDIUM.get().getWidth(timeText, metaSize))
                    + Fonts.RUBIK.get().getWidth(levelText, 5.8f) + 34.0f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.RUBIK.get().getWidth(placeholderText, nameSize) + 24.0f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) visible.stream().mapToDouble(item -> rowHeight * (float) item.animation.getValue()).sum();
        rowsHeight += rowHeight * emptyAnimVal;
        float totalHeight = headerHeight + rowsHeight + 6.0f;

        Draggable drag = owner.getPotionsDrag();
        float x = drag.getX();
        float y = drag.getY();

        owner.drawBackground(x, y, currentWidth, totalHeight, 6.0f, aInt);
        owner.drawAxiomAccent(x + 4.0f, y + 4.0f, 16.5f, headerHeight - 8.0f, 4.0f, aInt);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "E", x + 8.2f, y + 5.25f, owner.getAxiomPrimaryTextColor(aInt), 7.8f);
        DrawUtil.drawText(Fonts.RUBIK.get(), titleText, x + 25.0f, y + 4.0f, owner.getAxiomPrimaryTextColor(aInt), 7.2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), subtitleText, x + 25.0f, y + 10.0f, owner.getAxiomSecondaryTextColor(aInt), 5.7f);

        float currentY = y + headerHeight + 2.0f;
        for (PotionItem item : visible) {
            float rowAnim = (float) item.animation.getValue();
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                float rowY = currentY;
                float innerWidth = currentWidth - (rowInset * 2.0f);
                float iconBoxX = x + rowInset + 2.0f;
                float iconBoxY = rowY + 3.0f;
                float iconBoxSize = Math.max(9.0f, itemHeight - 6.0f);
                String timeText = formatPotionTime(item.durationTicks);
                String levelText = getPotionLevelLabel(item.amplifier);
                float chipWidth = Fonts.RUBIK.get().getWidth(levelText, 5.8f) + 10.0f;
                float chipX = x + currentWidth - rowInset - chipWidth - 3.0f;
                float chipY = rowY + 3.4f;
                float chipHeight = Math.max(7.0f, itemHeight - 6.8f);
                float barWidth = Math.max(22.0f, Math.min(42.0f, currentWidth * 0.23f));
                float barX = chipX - barWidth - 6.0f;
                float barY = rowY + itemHeight - 4.9f;
                float progress = MathHelper.clamp(item.durationTicks / 1200.0f, 0.04f, 1.0f);

                owner.drawBackground(x + rowInset, rowY, innerWidth, itemHeight - 1.1f, 4.2f, itemAlpha);
                owner.drawAxiomAccent(iconBoxX, iconBoxY, 10.5f, iconBoxSize, 3.0f, itemAlpha);

                Sprite sprite = owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
                if (sprite != null) {
                    RenderSystem.setShaderColor(1f, 1f, 1f, itemAlpha / 255f);
                    context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, (int) iconBoxX + 1, (int) iconBoxY + 1, 8, 8, (itemAlpha << 24) | 0xFFFFFF);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }

                float nameClipWidth = Math.max(12.0f, barX - (x + rowInset + 15.0f) - 6.0f);
                Scissor.push();
                Scissor.setFromComponentCoordinates(x + rowInset + 15.0f, rowY, nameClipWidth, itemHeight);
                DrawUtil.drawText(Fonts.RUBIK.get(), item.name, x + rowInset + 15.0f, rowY + 3.15f, owner.getAxiomPrimaryTextColor(itemAlpha), nameSize);
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), timeText, x + rowInset + 15.0f, rowY + 10.1f, owner.getAxiomSecondaryTextColor(itemAlpha), metaSize);
                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawRound(barX, barY, barWidth, 1.55f, 0.75f, ColorProvider.rgba(255, 255, 255, Math.max(10, itemAlpha / 10)));
                DrawUtil.drawRound(
                        barX,
                        barY,
                        barWidth * progress,
                        1.55f,
                        0.75f,
                        ColorProvider.setAlpha(ColorProvider.getThemeColor(), Math.max(24, itemAlpha)),
                        ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), Math.max(18, itemAlpha))
                );

                DrawUtil.drawRound(
                        chipX,
                        chipY,
                        chipWidth,
                        chipHeight,
                        3.0f,
                        ColorProvider.rgba(255, 255, 255, Math.max(10, itemAlpha / 8)),
                        ColorProvider.rgba(255, 255, 255, Math.max(6, itemAlpha / 12))
                );
                DrawUtil.drawText(Fonts.RUBIK.get(), levelText, chipX + 5.0f, rowY + 4.05f, owner.getAxiomPrimaryTextColor(itemAlpha), 5.8f);
            }
            currentY += itemHeight;
        }

        if (emptyAnimVal > 0.001f) {
            float itemHeight = rowHeight * emptyAnimVal;
            int itemAlpha = MathHelper.clamp((int) (aInt * emptyAnimVal), 0, 255);
            if (itemAlpha >= 4) {
                float textX = x + (currentWidth - Fonts.RUBIK.get().getWidth(placeholderText, nameSize)) / 2.0f;
                DrawUtil.drawText(Fonts.RUBIK.get(), placeholderText, textX, currentY + 4.2f, owner.getAxiomSecondaryTextColor(itemAlpha), nameSize);
            }
            currentY += itemHeight;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void renderMoonward(DrawContext context) {
        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) {
                visible.add(item);
            }
        }

        alpha.run(1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int alphaInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float titleSize = 9f;
        float rowFontSize = 7.1f;
        float timeFontSize = 7.1f;
        float headerHeight = 15.5f;
        float rowHeight = 11.5f;
        String headerText = "POTIONS";

        float headerTextWidth = Fonts.SFSEMIBOLD.get().getWidth(headerText, titleSize);
        float iconWidth = 13f;
        float minWidth = 2.5f + iconWidth + 2.5f + headerTextWidth + 8f;
        float targetWidth = minWidth;

        for (PotionItem item : visible) {
            int totalSec = Math.max(0, item.durationTicks / 20);
            String timeText = String.format("%d:%02d", totalSec / 60, totalSec % 60);
            String nameText = item.amplifier >= 1 ? item.name + " " + (item.amplifier + 1) : item.name;
            float nameWidth = Fonts.SFBOLD.get().getWidth(nameText, rowFontSize);
            float timeWidth = Fonts.SFBOLD.get().getWidth(timeText, timeFontSize);
            float rowWidth = 18f + nameWidth + timeWidth + 16f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) visible.stream().mapToDouble(item -> rowHeight * (float) item.animation.getValue()).sum();
        float totalHeight = headerHeight + rowsHeight + 4f;

        Draggable drag = owner.getPotionsDrag();
        float x = drag.getX();
        float y = drag.getY();

        DrawUtil.drawRoundBlur(x - 2f, y + 2, currentWidth + 4f, totalHeight, 4f, ColorProvider.rgba(55, 55, 55, 255), 45);
        DrawUtil.drawRoundBlur(x, y + headerHeight, currentWidth, totalHeight - headerHeight, 2f, ColorProvider.rgba(135, 135, 135, 255), 0);

        float iconX = x + 2.5f;
        float centeredX = x + (currentWidth - headerTextWidth) / 2f;
        float headerTextX = Math.max(centeredX, iconX + iconWidth + 2.5f);

        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue901", iconX, y + 4f, ColorProvider.rgba(222, 222, 222, alphaInt), 13f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), headerText, headerTextX, y + 4.35f, ColorProvider.rgba(222, 222, 222, alphaInt), titleSize);

        float currentY = y + headerHeight;
        for (PotionItem item : visible) {
            float rowAnim = (float) item.animation.getValue();
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (alphaInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                int totalSec = Math.max(0, item.durationTicks / 20);
                String timeText = String.format("%d:%02d", totalSec / 60, totalSec % 60);
                String nameText = item.amplifier >= 1 ? item.name + " " + (item.amplifier + 1) : item.name;

                float textY = currentY + 3.85f;
                float timeWidth = Fonts.SFSEMIBOLD.get().getWidth(timeText, timeFontSize);
                float timeX = x + currentWidth - timeWidth - 4f;
                float iconX_row = x + 2.75f;
                float iconYBox = currentY + 2.3f;
                float iconSize = 10.5f;

                DrawUtil.drawRound(iconX_row, iconYBox, iconSize, iconSize, 2.2f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), itemAlpha));

                Sprite sprite = owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
                if (sprite != null) {
                    RenderSystem.setShaderColor(1f, 1f, 1f, itemAlpha / 255f);
                    context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, (int) iconX_row + 1, (int) iconYBox + 1, (int) iconSize , (int) iconSize, (itemAlpha << 24) | 0xFFFFFF);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }

                float clipWidth = timeX - (x + 16f) - 4f;
                Scissor.push();
                Scissor.setFromComponentCoordinates(x + 16f, currentY + 2, clipWidth, itemHeight);
                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), nameText, x + 16f, textY, ColorProvider.rgba(226, 226, 226, itemAlpha), rowFontSize);
                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), timeText, timeX, textY, ColorProvider.setAlpha(ColorProvider.getThemeColor(), itemAlpha), timeFontSize);
            }
            currentY += itemHeight;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }
    public void updatePotions() {
        Map<String, StatusEffectInstance> currentEffects = owner.mc.player.getStatusEffects().stream()
                .collect(Collectors.toMap(
                        e -> net.minecraft.text.Text.translatable(e.getTranslationKey()).getString() + ":" + e.getAmplifier(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        potionItems.forEach(item -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = currentEffects.get(key);
            if (effect != null) {
                item.durationTicks = effect.getDuration();
                if (!item.active) item.animation.setValue(1.0f);
                item.active = true;
                currentEffects.remove(key);
            } else {
                item.active = false;
            }
        });

        currentEffects.forEach((key, effect) -> potionItems.add(new PotionItem(
                net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString(),
                effect.getAmplifier(),
                effect.getDuration(),
                effect.getEffectType()
        )));

        potionItems.removeIf(item -> !item.active && item.animation.getValue() == 0);
    }

    private void renderCelestial(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) visible.add(item);
        }

        boolean showPlaceholder = chatOpen && visible.isEmpty();
        potionsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((visible.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) potionsEmptyAnim.getValue(), 0f, 1f);

        final String headerText = "Potions";
        final String placeholderText = "No active effects";
        final float fontSize = 7.5f;
        final float headerH = 14f;
        final float rowH = 9.5f;
        final float padL = 5f;
        final float padR = 5f;

        float targetWidth = 70f;
        for (PotionItem item : visible) {
            int totalSec = Math.max(0, item.durationTicks / 20);
            int minutes = totalSec / 60;
            int sec = totalSec % 60;
            String time = String.format("%d:%02d", minutes, sec);
            int lvl = item.amplifier + 1;
            String lvlText = "  " + lvl;

            float nameW = Fonts.CELESTIAL.get().getWidth(item.name, fontSize);
            float lvlW = Fonts.CELESTIAL.get().getWidth(lvlText, fontSize);
            float timeW = Fonts.CELESTIAL.get().getWidth(time, fontSize);
            float rowW = padL + nameW + lvlW + 10f + timeW + padR;
            targetWidth = Math.max(targetWidth, rowW);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize) + 14f);
        }

        widthAnim.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim.getValue());
        float rowsHeight = 0f;
        for (PotionItem item : visible) {
            rowsHeight += rowH * MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
        }
        rowsHeight += rowH * emptyAnimVal;
        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        Draggable drag = owner.getPotionsDrag();
        float x = drag.getX();
        float y = drag.getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();
        int[] orbital = ColorProvider.getOrbitalRect(t1, t2, 300.0, aInt);
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 300.0, (int) (110 * globalAlpha));
        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        drawCelestialGlow(m, x, y, curW, totalH, 4f, globalAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, curW + 1f, totalH + 1f, 4f, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, curW, totalH, 4f, ColorProvider.rgba(14, 10, 6, aInt));

        Builder.rectangle()
                .size(new SizeState(curW + 0.5f, headerH))
                .radius(new QuadRadiusState(4, 0, 0, 4))
                .color(new QuadColorState(orbital[0], orbital[1], orbital[2], orbital[3]))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);

        float headerTextX = x + (curW - Fonts.CELESTIAL.get().getWidth(headerText, 10f)) / 2f;
        DrawUtil.drawText(Fonts.CELESTIAL.get(), headerText, headerTextX, y + 1f, ColorProvider.rgba(255, 255, 255, aInt), 10f);

        float curY = y + headerH + 1f;
        for (PotionItem item : visible) {
            float rowAnim = MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;
            float itemH = rowH * rowAnim;
            int itemA = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);

            if (itemA >= 4) {
                int totalSec = Math.max(0, item.durationTicks / 20);
                int minutes = totalSec / 60;
                int sec = totalSec % 60;
                String time = String.format("%d:%02d", minutes, sec);
                int lvl = item.amplifier + 1;
                String lvlText = "  " + lvl;

                float timeW = Fonts.SFBOLD.get().getWidth(time, fontSize);
                float timeX = x + curW - timeW - padR;
                float leftX = x + padL;
                float textY = curY + (itemH / 2f) - (fontSize / 2f) - 1f;

                float clipW = Math.max(0f, (timeX - 6f) - leftX);
                Scissor.push();
                Scissor.setFromComponentCoordinates(leftX, curY, clipW, itemH);
                DrawUtil.drawText(Fonts.CELESTIAL.get(), item.name, leftX, textY, ColorProvider.rgba(233, 233, 233, itemA), fontSize);
                float nameW = Fonts.CELESTIAL.get().getWidth(item.name, fontSize);
                int lvlColor = (lvl >= 2) ? ColorProvider.rgba(192, 100, 106, itemA) : ColorProvider.rgba(200, 200, 200, itemA);
                if (lvl > 1) DrawUtil.drawText(Fonts.CELESTIAL.get(), lvlText, leftX + nameW, textY, lvlColor, fontSize);
                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawText(Fonts.CELESTIAL.get(), time, timeX, textY, ColorProvider.rgba(200, 200, 200, itemA), fontSize);
            }
            curY += itemH;
        }

        if (emptyAnimVal > 0.001f) {
            float itemH = rowH * emptyAnimVal;
            int itemA = MathHelper.clamp((int) (aInt * emptyAnimVal), 0, 255);
            if (itemA >= 4) {
                float textY = curY + (itemH / 2f) - (fontSize / 2f);
                float textX = x + (curW - Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize)) / 2f;
                DrawUtil.drawText(Fonts.CELESTIAL.get(), placeholderText, textX, textY, ColorProvider.rgba(255, 205, 70, itemA), fontSize);
            }
            curY += itemH;
        }

        drag.setWidth(curW);
        drag.setHeight(totalH);
    }

    private void renderExp4_0(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) visible.add(item);
        }

        boolean showPlaceholder = chatOpen && visible.isEmpty();
        potionsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((visible.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) potionsEmptyAnim.getValue(), 0f, 1f);

        final float headerHeight = 20f;
        final float rowHeight = 14f;
        final String headerText = "Potions";
        final float nameSize = 6f;
        final float timerSize = 6f;

        float targetWidth = 110f;
        for (PotionItem item : visible) {
            String key = item.name + ":" + item.amplifier;
            Integer currentMax = potionMaxDurations.get(key);
            if (currentMax == null || item.durationTicks > currentMax) {
                potionMaxDurations.put(key, item.durationTicks);
            }
            String displayName = item.amplifier >= 1 ? item.name + " " + (item.amplifier + 1) : item.name;
            String timer = formatExp4Timer(item.durationTicks);
            float nameWidth = Fonts.SFBOLD.get().getWidth(displayName, nameSize);
            float timerWidth = Fonts.SFBOLD.get().getWidth(timer, timerSize);
            targetWidth = Math.max(nameWidth + timerWidth + 40f, targetWidth);
        }
        if (emptyAnimVal > 0.001f) {
            float exampleWidth = Fonts.SFBOLD.get().getWidth("Example Effect 10", nameSize)
                    + Fonts.SFBOLD.get().getWidth("0:00", timerSize) + 40f;
            targetWidth = Math.max(exampleWidth, targetWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(110f, (float) widthAnim.getValue());
        float contentRows = (float) visible.stream().mapToDouble(item -> (float) item.animation.getValue()).sum();
        contentRows += emptyAnimVal;
        float totalHeight = headerHeight + contentRows * rowHeight + 4f;

        Draggable drag = owner.getPotionsDrag();
        float x = drag.getX();
        float y = drag.getY();

        int bgAlpha = MathHelper.clamp((int) (230 * globalAlpha), 0, 255);
        DrawUtil.drawRound(x, y, currentWidth, totalHeight, 5f,
                ColorProvider.rgba(20, 20, 20, bgAlpha),
                ColorProvider.rgba(15, 15, 15, bgAlpha),
                ColorProvider.rgba(20, 20, 20, bgAlpha),
                ColorProvider.rgba(15, 15, 15, bgAlpha));
        owner.drawExp4Border(x, y, currentWidth, totalHeight, 5f, bgAlpha);

        Scissor.push();
        Scissor.setFromComponentCoordinates((int) x, (int) y, (int) currentWidth, (int) totalHeight);

        DrawUtil.drawText(Fonts.SFBOLD.get(), headerText, x + 8f, y + 6.5f, ColorProvider.rgba(255, 255, 255, aInt), 8f);

        float iconBoxSize = 12f;
        float iconBoxX = x + currentWidth - iconBoxSize - 5f;
        float iconBoxY = y + 4f;
        DrawUtil.drawRound(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 3f,
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha),
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha));
        DrawUtil.drawText(Fonts.ICONS2.get(), "I", iconBoxX + 1.5f, iconBoxY + 1f, ColorProvider.rgba(130, 140, 255, aInt), 9f);

        float rowY = y + headerHeight;
        for (PotionItem item : visible) {
            float anim = MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
            if (anim <= 0.001f) continue;
            int rowAlpha = MathHelper.clamp((int) (255 * anim * globalAlpha), 0, 255);
            if (rowAlpha < 4) {
                rowY += rowHeight * anim;
                continue;
            }
            String key = item.name + ":" + item.amplifier;
            int maxDuration = potionMaxDurations.getOrDefault(key, Math.max(1, item.durationTicks));
            float progress = 1.0f;
            if (item.durationTicks != -1 && maxDuration > 0) {
                progress = MathHelper.clamp((float) item.durationTicks / maxDuration, 0.02f, 1.0f);
            }
            drawExp4EffectRow(context, x, rowY, currentWidth, item, progress, anim, rowAlpha);
            rowY += rowHeight * anim;
        }

        if (emptyAnimVal > 0.001f) {
            int rowAlpha = MathHelper.clamp((int) (255 * emptyAnimVal * globalAlpha), 0, 255);
            drawExp4EffectRow(context, x, rowY, currentWidth, null, 1.0f, emptyAnimVal, rowAlpha);
            rowY += rowHeight * emptyAnimVal;
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawExp4EffectRow(DrawContext context, float x, float rowY, float w, PotionItem item, float progress, float animation, int alpha) {
        if (alpha < 4) return;
        float rowHeight = 14f;
        float nameSize = 6f;

        String displayName;
        String timer;
        if (item == null) {
            displayName = "Example Effect 10";
            timer = "0:00";
        } else {
            displayName = item.amplifier >= 1 ? item.name + " " + (item.amplifier + 1) : item.name;
            timer = formatExp4Timer(item.durationTicks);
        }

        float timerTextWidth = Fonts.SFBOLD.get().getWidth(timer, 6f);
        float arcSize = 5.0f;
        float spacing = 2.5f;
        float boxWidth = arcSize + spacing + timerTextWidth + 8f;
        float boxHeight = 11f;
        float boxX = x + w - boxWidth - 6f;
        float boxY = rowY + (rowHeight - boxHeight) / 2f;

        DrawUtil.drawRound(boxX, boxY, boxWidth, boxHeight, 3f,
                ColorProvider.rgba(25, 25, 25, (int) (alpha * 0.6f)),
                ColorProvider.rgba(20, 20, 20, (int) (alpha * 0.6f)),
                ColorProvider.rgba(25, 25, 25, (int) (alpha * 0.6f)),
                ColorProvider.rgba(20, 20, 20, (int) (alpha * 0.6f)));
        Builder.border()
                .size(new SizeState(boxWidth + 0.5f, boxHeight + 0.25f))
                .radius(new QuadRadiusState(3f))
                .color(new QuadColorState(ColorProvider.rgba(45, 45, 45, (int) (alpha * 0.6f))))
                .thickness(0.2f)
                .smoothness(0.5f, 1f)
                .build()
                .render(boxX, boxY);

        int timerColor = getExp4TimerColor(item, alpha);
        float arcX = boxX + 4f;
        float arcY = boxY + (boxHeight - arcSize) / 2f;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        drawExp4Arc(matrix, arcX + arcSize / 2f, arcY + arcSize / 2f, arcSize / 2f, 1.2f, 360f, ColorProvider.rgba(40, 40, 40, alpha));
        drawExp4Arc(matrix, arcX + arcSize / 2f, arcY + arcSize / 2f, arcSize / 2f, 1.2f, Math.max(0f, progress) * 360f, timerColor);

        float textHeight = 5f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), timer, arcX + arcSize + spacing, boxY + (boxHeight - textHeight) / 2f - 1.0f, timerColor, 6f);

        float iconSize = 9f;
        float iconX = x + 8f;
        float iconY = rowY + (rowHeight - iconSize) / 2f;
        if (item != null && item.effect != null) {
            Sprite sprite = owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
            if (sprite != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, alpha / 255f);
                context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize, (alpha << 24) | 0xFFFFFF);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        }

        DrawUtil.drawText(Fonts.SFBOLD.get(), displayName, x + 20f, rowY + (rowHeight - 6f) / 2f + 0.5f, ColorProvider.rgba(220, 220, 220, alpha), nameSize);
    }

    private int getExp4TimerColor(PotionItem item, int alpha) {
        if (item == null || item.effect == null || item.effect.value() == null) {
            return ColorProvider.rgba(130, 140, 255, alpha);
        }
        boolean harmful = item.effect.value().getCategory() == StatusEffectCategory.HARMFUL;
        if (harmful) {
            return ColorProvider.rgba(255, 75, 75, alpha);
        }
        if (item.durationTicks != -1 && item.durationTicks <= 300) {
            return ColorProvider.rgba(255, 170, 0, alpha);
        }
        return ColorProvider.rgba(130, 140, 255, alpha);
    }

    private String formatExp4Timer(int ticks) {
        if (ticks == -1) return "∞";
        int totalSec = Math.max(0, ticks / 20);
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
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

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getPotionsDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        float headerIconW = Fonts.ICONS_NURIK.get().getWidth("E", 8);
        float headerTextW = Fonts.SFMEDIUM.get().getWidth("Active Potions", 7.5f);
        float defaultWidth = headerIconW + headerTextW + 30;
        float height = 14.5f;

        potionItems.sort(Comparator.comparing(pi -> pi.name));
        boolean isFound = false;

        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1 : 0);
            if (item.animation.getValue() > 0.001f) isFound = true;
            int seconds = item.durationTicks / 20;
            int minutes = seconds / 60;
            int sec = seconds % 60;
            String duration = String.format("%d:%02d", minutes, sec);
            float nameW = Fonts.SFREGULAR.get().getWidth(item.name, 6.5f);
            float ampW = (item.amplifier >= 1 ? Fonts.SFREGULAR.get().getWidth(" " + (item.amplifier + 1), 6.5f) : 0);
            float timeW = Fonts.SFREGULAR.get().getWidth(duration, 6.5f);
            float moduleWidth = nameW + ampW + timeW + 45;
            if (moduleWidth > defaultWidth) defaultWidth = moduleWidth;
        }

        if (!isFound && !(owner.mc.currentScreen instanceof ChatScreen)) alpha.run(0);
        else alpha.run(1);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;
        int headerAlpha = MathHelper.clamp((int) (255 * globalAlpha), 0, 255);

        widthAnim.run(defaultWidth);
        float currentWidth = Math.max(20, (float) widthAnim.getValue());
        owner.drawBackground(posX, posY, currentWidth - 3, height, 3, headerAlpha);

        DrawUtil.drawRound(posX + 13.75f, posY + 2, 0.5f, 10.5f, 0, ColorProvider.rgba(88, 88, 88, headerAlpha));
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "E", posX + 4, posY + 3.75f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Active Potions", posX + 18f, posY + 3.25f, ColorProvider.rgba(255, 255, 255, headerAlpha), 7.5f);

        posY += 14.5f;
        xLine.run(12);

        for (PotionItem item : potionItems) {
            float animVal = (float) item.animation.getValue();
            if (animVal <= 0.001f) continue;

            float itemHeight = 12 * Math.min(1.0f, animVal);
            height += itemHeight;
            int itemAlpha = MathHelper.clamp((int) (255 * Math.min(1.0f, Math.max(0.0f, animVal)) * globalAlpha), 0, 255);
            if (itemAlpha < 5) {
                posY += itemHeight;
                continue;
            }

            String moduleName = item.name;
            int seconds = item.durationTicks / 20;
            int minutes = seconds / 60;
            int sec = seconds % 60;
            String bind = String.format("%d:%02d", minutes, sec);
            float textYOffset = (itemHeight / 2f) - 3f;

            owner.drawBackground(posX, posY, currentWidth - 3, itemHeight, 3, itemAlpha);
            float separatorX = (float) (posX + currentWidth - 6.5f - xLine.getValue());
            DrawUtil.drawRound(separatorX, posY + 2, 0.5f, itemHeight - 4, 0, ColorProvider.rgba(88, 88, 88, itemAlpha));

            DrawUtil.drawText(Fonts.SFREGULAR.get(), moduleName, posX + 4, posY + textYOffset - 1f, ColorProvider.rgba(255, 255, 255, itemAlpha), 6.5f);
            if (item.amplifier >= 1) {
                DrawUtil.drawText(Fonts.SFREGULAR.get(), String.valueOf(item.amplifier + 1),
                        posX + 6 + Fonts.SFREGULAR.get().getWidth(moduleName, 6.75f),
                        posY + textYOffset - 1f,
                        ColorProvider.setAlpha(ColorProvider.rgba(66, 205, 255, 255), itemAlpha), 6.5f);
            }
            float timeWidth = Fonts.SFREGULAR.get().getWidth(bind, 6.75f);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), bind, separatorX - timeWidth - 3f, posY + textYOffset - 0.5f, ColorProvider.rgba(255, 255, 255, itemAlpha), 6.5f);

            Sprite sprite = owner.mc.getStatusEffectSpriteManager().getSprite(item.effect);
            if (sprite != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, (itemAlpha / 255f));
                float iconSize = 9;
                float iconX = separatorX + 3.5f;
                float iconY = posY + (itemHeight - iconSize) / 2f;
                int color = (itemAlpha << 24) | 0xFFFFFF;
                context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize, color);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            posY += itemHeight;
        }

        widthAnim.run(defaultWidth);
        drag.setWidth((float) widthAnim.getValue());
        drag.setHeight(height);
    }

    private void drawCelestialGlow(Matrix4f matrix, float x, float y, float w, float h, float radius, float anim) {
        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();
        float glow = 7.0f;
        int a = (int) (110 * anim);
        int[] c = ColorProvider.getOrbitalRect(t1, t2, 300.0, a);

        Builder.glow()
                .size(new SizeState(w + glow * 2f - 6, h + glow * 2f - 6))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(c[0], c[1], c[2], c[3]))
                .glowRadius(glow)
                .softness(0f)
                .intensity(2.0f)
                .additive(true)
                .build()
                .render(matrix, x - glow + 3, y - glow + 3, 0);
    }

    private String formatPotionTime(int durationTicks) {
        int totalSec = Math.max(0, durationTicks / 20);
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
    }

    private String getPotionLevelLabel(int amplifier) {
        return amplifier >= 1 ? "LV " + (amplifier + 1) : "LV 1";
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
        Animation animation = new Animation(Easing.EXPO_OUT, 233);

        PotionItem(String name, int amplifier, int durationTicks, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.effect = effect;
            this.active = true;
        }
    }
}
