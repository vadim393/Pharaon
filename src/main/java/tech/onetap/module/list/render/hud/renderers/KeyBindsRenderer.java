package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.keyboard.KeyStorage;
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
import java.util.List;

public class KeyBindsRenderer {
    private final Interface owner;
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation xLine = new Animation(Easing.EXPO_OUT, 170);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation keybindsEmptyAnim = new Animation(Easing.EXPO_OUT, 233);

    public KeyBindsRenderer(Interface owner) {
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
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();
        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        boolean showPlaceholder = chatOpen && activeBinds.isEmpty();
        keybindsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeBinds.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        float emptyAnimVal = MathHelper.clamp((float) keybindsEmptyAnim.getValue(), 0f, 1f);
        float headerHeight = 15f;
        float itemSpacing = 11f;
        float minWidth = 60f;

        float targetWidth = minWidth;
        for (KeyBindEntry entry : activeBinds) {
            String keyText = KeyStorage.getKey(entry.key).toUpperCase();
            targetWidth = Math.max(targetWidth,
                    Fonts.SFMEDIUM.get().getWidth(entry.name, 6f) + Fonts.SFMEDIUM.get().getWidth(keyText, 6f) + 26.5f);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth,
                    Fonts.SFMEDIUM.get().getWidth("Example", 6f) + Fonts.SFMEDIUM.get().getWidth("R", 6f) + 26.5f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());

        float rowCount = (float) activeBinds.stream()
                .mapToDouble(bind -> MathHelper.clamp(bind.animation, 0f, 1f))
                .sum();
        rowCount += showPlaceholder ? emptyAnimVal : 0f;
        rowCount = Math.max(1f, rowCount);
        float totalHeight = Math.max(20f, headerHeight + rowCount * itemSpacing);

        Draggable drag = owner.getKeyBindsDrag();
        float x = drag.getX();
        float y = drag.getY();

        Hud3Style.drawPanel(x, y, currentWidth, totalHeight, true, globalAlpha);
        Hud3Style.drawHeader(x, y, currentWidth, "Hotkeys", "C", globalAlpha);

        float rowY = y + headerHeight;
        Scissor.push();
        Scissor.setFromComponentCoordinates((int) x, (int) rowY, (int) currentWidth, (int) (totalHeight - headerHeight + 2f));

        if (showPlaceholder) {
            drawPouchHotkeyRow(x, rowY, currentWidth, "Example", "R", globalAlpha * emptyAnimVal);
            rowY += itemSpacing * emptyAnimVal;
        } else {
            for (KeyBindEntry entry : activeBinds) {
                float rowAnim = MathHelper.clamp(entry.animation, 0f, 1f);
                if (rowAnim <= 0.001f) continue;
                drawPouchHotkeyRow(x, rowY + (1f - rowAnim) * 4f, currentWidth, entry.name,
                        KeyStorage.getKey(entry.key).toUpperCase(), globalAlpha * rowAnim);
                rowY += itemSpacing * rowAnim;
            }
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawPouchHotkeyRow(float x, float rowY, float w, String name, String bind, float rowAlpha) {
        if (rowAlpha <= 0.01f) return;
        int nameCol = ColorProvider.rgba(255, 255, 255, (int) (255 * rowAlpha));
        int bindCol = ColorProvider.rgba(255, 255, 255, (int) (245 * rowAlpha));
        float bindW = Fonts.SFMEDIUM.get().getWidth(bind, 6f);
        float nameW = Fonts.SFMEDIUM.get().getWidth(name, 6f);
        float gap = 9f;
        float blockW = nameW + gap + bindW;
        float blockX = x + Math.max(4.5f, (w - blockW) / 2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, blockX, rowY + 3f, nameCol, 6f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), bind, blockX + nameW + gap, rowY + 3f, bindCol, 6f);
    }

    private void renderAxiom(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();

        boolean showPlaceholder = chatOpen && activeBinds.isEmpty();
        keybindsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeBinds.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) keybindsEmptyAnim.getValue(), 0f, 1f);
        float headerHeight = 18.0f;
        float rowHeight = 13.5f;
        float rowInset = 4.0f;
        float minWidth = 92.0f;
        float nameSize = 7.0f;
        float keySize = 6.15f;
        String titleText = "KEY RACK";
        String subtitleText = activeBinds.isEmpty() ? "waiting" : activeBinds.size() + " active";
        String placeholderText = "No binds active";

        float targetWidth = minWidth;
        for (KeyBindEntry entry : activeBinds) {
            String keyText = KeyStorage.getKey(entry.key).toUpperCase();
            float rowWidth = 8.0f + 10.0f + 6.0f
                    + Fonts.RUBIK.get().getWidth(entry.name, nameSize)
                    + Fonts.RUBIK.get().getWidth(keyText, keySize) + 18.0f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.RUBIK.get().getWidth(placeholderText, nameSize) + 24.0f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) activeBinds.stream().mapToDouble(bind -> rowHeight * bind.animation).sum();
        rowsHeight += rowHeight * emptyAnimVal;
        float totalHeight = headerHeight + rowsHeight + 6.0f;

        Draggable drag = owner.getKeyBindsDrag();
        float x = drag.getX();
        float y = drag.getY();

        owner.drawBackground(x, y, currentWidth, totalHeight, 6.0f, aInt);
        owner.drawAxiomAccent(x + 4.0f, y + 4.0f, 16.5f, headerHeight - 8.0f, 4.0f, aInt);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "C", x + 8.1f, y + 5.25f, owner.getAxiomPrimaryTextColor(aInt), 7.9f);
        DrawUtil.drawText(Fonts.RUBIK.get(), titleText, x + 25.0f, y + 4.0f, owner.getAxiomPrimaryTextColor(aInt), 7.3f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), subtitleText, x + 25.0f, y + 10.0f, owner.getAxiomSecondaryTextColor(aInt), 5.7f);

        float currentY = y + headerHeight + 2.0f;
        for (KeyBindEntry entry : activeBinds) {
            float rowAnim = entry.animation;
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                String keyText = KeyStorage.getKey(entry.key).toUpperCase();
                float rowY = currentY;
                float innerWidth = currentWidth - (rowInset * 2.0f);
                float keyChipWidth = Fonts.RUBIK.get().getWidth(keyText, keySize) + 10.0f;
                float keyChipHeight = Math.max(7.0f, itemHeight - 4.5f);
                float keyChipX = x + currentWidth - rowInset - keyChipWidth - 2.5f;
                float iconBoxX = x + rowInset + 2.0f;
                float iconBoxY = rowY + 2.2f;
                float iconBoxSize = Math.max(7.0f, itemHeight - 4.4f);

                owner.drawBackground(x + rowInset, rowY, innerWidth, itemHeight - 1.1f, 4.2f, itemAlpha);
                owner.drawAxiomAccent(iconBoxX, iconBoxY, 9.5f, iconBoxSize, 3.0f, itemAlpha);
                DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), entry.icon, iconBoxX + 1.15f, rowY + 3.3f, owner.getAxiomPrimaryTextColor(itemAlpha), 7.8f);

                DrawUtil.drawText(Fonts.RUBIK.get(), entry.name, x + rowInset + 15.0f, rowY + 3.45f, owner.getAxiomPrimaryTextColor(itemAlpha), nameSize);

                DrawUtil.drawRound(
                        keyChipX,
                        rowY + 2.3f,
                        keyChipWidth,
                        keyChipHeight,
                        3.0f,
                        ColorProvider.rgba(255, 255, 255, Math.max(12, itemAlpha / 8)),
                        ColorProvider.rgba(255, 255, 255, Math.max(6, itemAlpha / 12))
                );
                DrawUtil.drawText(Fonts.RUBIK.get(), keyText, keyChipX + 5.0f, rowY + 4.0f, owner.getAxiomPrimaryTextColor(itemAlpha), keySize);
            }
            currentY += itemHeight;
        }

        if (emptyAnimVal > 0.001f) {
            float itemHeight = rowHeight * emptyAnimVal;
            int itemAlpha = MathHelper.clamp((int) (aInt * emptyAnimVal), 0, 255);
            if (itemAlpha >= 4) {
                float textX = x + (currentWidth - Fonts.RUBIK.get().getWidth(placeholderText, nameSize)) / 2.0f;
                DrawUtil.drawText(Fonts.RUBIK.get(), placeholderText, textX, currentY + 3.45f, owner.getAxiomSecondaryTextColor(itemAlpha), nameSize);
            }
            currentY += itemHeight;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void renderMoonward(DrawContext context) {
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();

        alpha.run(1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int alphaInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float titleSize = 9f;
        float rowFontSize = 7.1f;
        float keyFontSize = 7.1f;
        float headerHeight = 15.5f;
        float rowHeight = 11.5f;
        String headerText = "HOTKEYS";

        float headerTextWidth = Fonts.SFSEMIBOLD.get().getWidth(headerText, titleSize);
        float iconWidth = 13f;
        float minWidth = 2.5f + iconWidth + 2.5f + headerTextWidth + 8f;
        float targetWidth = minWidth;

        for (KeyBindEntry entry : activeBinds) {
            String keyText = "[" + KeyStorage.getKey(entry.key) + "]";
            float nameWidth = Fonts.SFBOLD.get().getWidth(entry.name, rowFontSize);
            float keyWidth = Fonts.SFBOLD.get().getWidth(keyText, keyFontSize);
            float rowWidth = 18f + nameWidth + keyWidth + 16f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) activeBinds.stream().mapToDouble(bind -> rowHeight * bind.animation).sum();
        float totalHeight = headerHeight + rowsHeight + 4f;

        Draggable drag = owner.getKeyBindsDrag();
        float x = drag.getX();
        float y = drag.getY();

        DrawUtil.drawRoundBlur(x - 2f, y + 2, currentWidth + 4f, totalHeight, 4f, ColorProvider.rgba(55, 55, 55, 255), 45);
        DrawUtil.drawRoundBlur(x, y + headerHeight, currentWidth, totalHeight - headerHeight, 2f, ColorProvider.rgba(135, 135, 135, 255), 0);

        float iconX = x + 2.5f;
        float iconEnd = iconX + iconWidth;


        float centeredX = x + (currentWidth - headerTextWidth) / 2f;
        float minAllowedX = iconEnd + 2.5f;
        float headerTextX = Math.max(centeredX, minAllowedX);

        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue913", iconX, y + 4f, ColorProvider.rgba(222, 222, 222, alphaInt), 13f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), headerText, headerTextX, y + 4.35f, ColorProvider.rgba(222, 222, 222, alphaInt), titleSize);

        float currentY = y + headerHeight;
        for (KeyBindEntry entry : activeBinds) {
            float rowAnim = entry.animation;
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (alphaInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                String keyText = "[" + KeyStorage.getKey(entry.key) + "]";
                float textY = currentY + 3.85f;
                float keyX = x + currentWidth - Fonts.SFSEMIBOLD.get().getWidth(keyText, keyFontSize) - 4f;
                float iconX_row = x + 2.75f;
                float iconYBox = currentY + 2.3f;
                float iconSize = 10.5f;
                float iconTextSize = 9f;

                DrawUtil.drawRound(iconX_row, iconYBox, iconSize, iconSize, 2.2f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), itemAlpha));
                float iconTextX = iconX_row + 1.75f + ("\ue910".equals(entry.icon) ? 0.75f : 0f);
                float iconTextY = iconYBox + 1.2f;

                DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), entry.icon, iconTextX, iconTextY, ColorProvider.rgba(222, 222, 222, itemAlpha), iconTextSize);
                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), entry.name, x + 16f, textY, ColorProvider.rgba(226, 226, 226, itemAlpha), rowFontSize);
                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), keyText, keyX, textY, ColorProvider.setAlpha(ColorProvider.getThemeColor(), itemAlpha), keyFontSize);
            }
            currentY += itemHeight;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void renderCelestial(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();

        boolean showPlaceholder = chatOpen && activeBinds.isEmpty();
        keybindsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeBinds.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) keybindsEmptyAnim.getValue(), 0f, 1f);

        final String placeholderText = "No active binds";
        final float fontSize = 7.5f;
        final float headerH = 14f;
        final float rowH = 9.5f;

        float targetWidth = 70f;
        for (KeyBindEntry entry : activeBinds) {
            String keyText = "[" + KeyStorage.getKey(entry.key) + "]";
            float rowWidth = Fonts.CELESTIAL.get().getWidth(entry.name, fontSize) + Fonts.CELESTIAL.get().getWidth(keyText, fontSize) + 20f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize) + 14f);
        }
        widthAnim.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim.getValue());

        float rowsHeight = (float) activeBinds.stream().mapToDouble(bind -> rowH * bind.animation).sum();
        rowsHeight += rowH * emptyAnimVal;
        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        Draggable drag = owner.getKeyBindsDrag();
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

        float headerTextX = x + (curW - Fonts.CELESTIAL.get().getWidth("Keybinds", 10f)) / 2f;
        DrawUtil.drawText(Fonts.CELESTIAL.get(), "Keybinds", headerTextX, y + 1f, ColorProvider.rgba(255, 255, 255, aInt), 10f);

        float curY = y + headerH + 1f;
        for (KeyBindEntry entry : activeBinds) {
            float rowAnim = entry.animation;
            if (rowAnim <= 0.001f) continue;

            float itemHeight = 9 * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                float textY = curY + (itemHeight / 2f) - (fontSize / 2f) - 1;
                String key = "[" + KeyStorage.getKey(entry.key) + "]";

                DrawUtil.drawText(Fonts.CELESTIAL.get(), entry.name, x + 5f, textY, ColorProvider.rgba(233, 233, 233, itemAlpha), fontSize);
                float keyX = x + curW - Fonts.CELESTIAL.get().getWidth(key, fontSize) - 5f;
                DrawUtil.drawText(Fonts.CELESTIAL.get(), key, keyX, textY, ColorProvider.rgba(200, 200, 200, itemAlpha), fontSize);
            }
            curY += itemHeight;
        }

        if (emptyAnimVal > 0.001f) {
            float itemHeight = rowH * emptyAnimVal;
            int itemAlpha = MathHelper.clamp((int) (aInt * emptyAnimVal), 0, 255);
            if (itemAlpha >= 4) {
                float textY = curY + (itemHeight / 2f) - (fontSize / 2f);
                float textX = x + (curW - Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize)) / 2f;
                DrawUtil.drawText(Fonts.CELESTIAL.get(), placeholderText, textX, textY, ColorProvider.rgba(255, 205, 70, itemAlpha), fontSize);
            }
            curY += itemHeight;
        }

        drag.setWidth(curW);
        drag.setHeight(totalH);
    }

    private void renderExp4_0(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();

        boolean showPlaceholder = chatOpen && activeBinds.isEmpty();
        keybindsEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeBinds.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) keybindsEmptyAnim.getValue(), 0f, 1f);

        final float headerHeight = 22f;
        final float rowHeight = 16f;
        final String headerText = "Hotkeys";

        float targetWidth = 85f;
        for (KeyBindEntry entry : activeBinds) {
            String keyText = KeyStorage.getKey(entry.key).toUpperCase();
            float nameWidth = Fonts.SFBOLD.get().getWidth(entry.name, 7f);
            float bindWidth = Fonts.SFBOLD.get().getWidth(keyText, 7f);
            float rowWidth = nameWidth + bindWidth + 35f;
            targetWidth = Math.max(rowWidth, targetWidth);
        }
        if (emptyAnimVal > 0.001f) {
            float exampleWidth = Fonts.SFBOLD.get().getWidth("Example Module", 7f)
                    + Fonts.SFBOLD.get().getWidth("E", 7f) + 35f;
            targetWidth = Math.max(exampleWidth, targetWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(85f, (float) widthAnim.getValue());
        float rowsHeight = (float) activeBinds.stream().mapToDouble(bind -> rowHeight * bind.animation).sum();
        rowsHeight += rowHeight * emptyAnimVal;
        float totalHeight = headerHeight + rowsHeight + 4f;

        Draggable drag = owner.getKeyBindsDrag();
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

        DrawUtil.drawText(Fonts.SFBOLD.get(), headerText, x + 8f, y + 7f, ColorProvider.rgba(255, 255, 255, aInt), 8f);

        float iconBoxSize = 12f;
        float iconBoxX = x + currentWidth - iconBoxSize - 5f;
        float iconBoxY = y + 5f;
        DrawUtil.drawRound(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 3f,
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha),
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha));
        DrawUtil.drawText(Fonts.HUD_ICONS.get(), "e", iconBoxX + 1.5f, iconBoxY + 2f, ColorProvider.rgba(130, 140, 255, aInt), 9f);

        float rowY = y + headerHeight;
        if (showPlaceholder && emptyAnimVal > 0.001f) {
            int rowAlpha = MathHelper.clamp((int) (255 * emptyAnimVal * globalAlpha), 0, 255);
            if (rowAlpha >= 4) {
                drawExp4KeyRow(x, rowY, currentWidth, "Example Module", "E", rowAlpha);
            }
            rowY += rowHeight * emptyAnimVal;
        } else {
            for (KeyBindEntry entry : activeBinds) {
                float rowAnim = entry.animation;
                if (rowAnim <= 0.001f) continue;
                int rowAlpha = MathHelper.clamp((int) (255 * rowAnim * globalAlpha), 0, 255);
                if (rowAlpha >= 4) {
                    drawExp4KeyRow(x, rowY, currentWidth, entry.name, KeyStorage.getKey(entry.key).toUpperCase(), rowAlpha);
                }
                rowY += rowHeight * rowAnim;
            }
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawExp4KeyRow(float x, float rowY, float w, String name, String bind, int alpha) {
        int textColor = ColorProvider.rgba(220, 220, 220, alpha);
        int accentColor = ColorProvider.rgba(130, 130, 140, alpha);

        DrawUtil.drawText(Fonts.SFBOLD.get(), name, x + 8f, rowY + (16f - 7f) / 2f + 0.5f, textColor, 7f);

        float bindTextSize = 7f;
        float bindWidth = Fonts.SFBOLD.get().getWidth(bind, bindTextSize);
        float boxSize = 12f;
        float boxWidth = Math.max(boxSize, bindWidth + 6f);
        float boxHeight = boxSize;
        float boxX = x + w - boxWidth - 5f;
        float boxY = rowY + (16f - boxHeight) / 2f;

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

        DrawUtil.drawText(Fonts.SFBOLD.get(), bind, boxX + (boxWidth - bindWidth) / 2f, boxY + (boxHeight - 6f) / 2f - 0.5f, accentColor, bindTextSize);
    }

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getKeyBindsDrag();
        float posX = drag.getX();
        float posY = drag.getY();
        float defaultWidth = 55;
        float height = 14.5f;

        List<KeyBindEntry> activeBinds = collectActiveKeyBindEntries();
        boolean isFound = !activeBinds.isEmpty();
        if (isFound) alpha.run(1);
        if (!isFound && !(owner.mc.currentScreen instanceof ChatScreen)) alpha.run(0);
        if (owner.mc.currentScreen instanceof ChatScreen) alpha.run(1);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int headerAlpha = MathHelper.clamp((int) (255 * globalAlpha), 0, 255);
        owner.drawBackground(posX, posY, (float) widthAnim.getValue(), height, 3, headerAlpha);

        DrawUtil.drawRound(posX + 15.25f, posY + 2, 0.5f, 10.5f, 0, ColorProvider.rgba(125, 125, 125, headerAlpha));
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "C", posX + 4f, posY + 4f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Hotkeys", posX + 19.5f, posY + 3.25f, ColorProvider.rgba(255, 255, 255, headerAlpha), 7.5f);

        posY += 14.5f;
        float bindWidth = 0;
        for (KeyBindEntry bindEntry : activeBinds) {
            float localBindWidth = Fonts.SFREGULAR.get().getWidth(KeyStorage.getKey(bindEntry.key), 6.75f);
            if (localBindWidth > bindWidth) bindWidth = localBindWidth;
        }

        xLine.run(bindWidth);

        for (KeyBindEntry bindEntry : activeBinds) {
            float animVal = bindEntry.animation;
            if (animVal <= 0.001f) continue;

            float itemHeight = 12 * Math.min(1.0f, animVal);
            height += itemHeight;
            int itemAlpha = MathHelper.clamp((int) (255 * Math.min(1.0f, Math.max(0.0f, animVal)) * globalAlpha), 0, 255);
            if (itemAlpha < 5) {
                posY += itemHeight;
                continue;
            }

            String bind = KeyStorage.getKey(bindEntry.key);
            String moduleName = bindEntry.name;
            float elementsWidth = Fonts.SFREGULAR.get().getWidth(moduleName, 6.75f) + Fonts.SFREGULAR.get().getWidth(bind, 6.75f) + 30;
            float textYOffset = (itemHeight / 2f) - 4f;

            owner.drawBackground(posX, posY, (float) widthAnim.getValue(), itemHeight, 3, itemAlpha);

            float separatorX = (float) (posX + widthAnim.getValue() - 6.5f - xLine.getValue());
            DrawUtil.drawRound(separatorX, posY + 2, 0.5f, itemHeight - 4, 0, ColorProvider.rgba(125, 125, 125, itemAlpha));
            DrawUtil.drawText(Fonts.SFREGULAR.get(), moduleName, posX + 4.25f, posY + textYOffset, ColorProvider.rgba(255, 255, 255, itemAlpha), 6.5f);

            float bindX = (float) (posX + widthAnim.getValue() - 2.5f - xLine.getValue()
                    - Fonts.SFREGULAR.get().getWidth(bind, 6.75f) / 2 + xLine.getValue() / 2 - 0.25f);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), bind, bindX, posY + textYOffset, ColorProvider.rgba(255, 255, 255, itemAlpha), 6.5f);

            if (elementsWidth > defaultWidth) defaultWidth = elementsWidth;
            posY += itemHeight;
        }

        widthAnim.run(defaultWidth);
        drag.setWidth((float) widthAnim.getValue());
        drag.setHeight(height);
    }

    private List<KeyBindEntry> collectActiveKeyBindEntries() {
        List<KeyBindEntry> entries = new ArrayList<>();
        for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
            float moduleAnim = MathHelper.clamp((float) module.getAnimation().getValue(), 0f, 1f);
            if (module.getKey() != -1 && moduleAnim > 0.01f) {
                entries.add(new KeyBindEntry(module.getName(), module.getKey(), moduleAnim, getCategoryIcon(module.getCategory())));
            }

            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof BooleanSetting boolSetting)) continue;
                float settingAnim = MathHelper.clamp((float) boolSetting.getAnimation().getValue(), 0f, 1f);
                if (boolSetting.getKey() == -1 || settingAnim <= 0.01f) continue;
                entries.add(new KeyBindEntry(boolSetting.getName(), boolSetting.getKey(), settingAnim, getCategoryIcon(module.getCategory())));
            }
        }
        return entries;
    }

    private String getCategoryIcon(ModuleCategory category) {
        if (category == null) {
            return "\ue90a";
        }

        return switch (category) {
            case COMBAT -> "\ue902";
            case MOVEMENT -> "\ue910";
            case RENDER -> "\ue90b";
            case PLAYER -> "\ue904";
            case MISC -> "\ue90a";
        };
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

    private static class KeyBindEntry {
        private final String name;
        private final int key;
        private final float animation;
        private final String icon;

        private KeyBindEntry(String name, int key, float animation, String icon) {
            this.name = name;
            this.key = key;
            this.animation = animation;
            this.icon = icon;
        }
    }
}
