package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
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
import tech.onetap.util.replace.ReplaceUtil;
import tech.onetap.util.staff.StaffManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class StaffListRenderer {
    private final Interface owner;
    private final List<Staff> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(ꔷ|ꔳ|ꔩ|ꔥ|ꔡ|ꔗ|ꔓ|\\bmod\\b|\\badm\\b|\\bhelp\\b|\\bwne\\b|модер|хелп|помощ|админ|владел|отриц|\\btaf\\b|\\bcurat\\b|куратор|\\bdev\\b|разраб|\\bsupp\\b|саппорт|\\byt\\b|\\[yt\\]|ютуб|стажер|сотрудник).*");
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation staffListEmptyAnim = new Animation(Easing.EXPO_OUT, 233);

    public StaffListRenderer(Interface owner) {
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
        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1f : 0f);
        }
        List<Staff> activeStaff = staffPlayers.stream()
                .filter(staff -> staff.animation.getValue() > 0.01f)
                .toList();

        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        boolean showPlaceholder = chatOpen && activeStaff.isEmpty();
        staffListEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeStaff.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        float emptyAnimVal = MathHelper.clamp((float) staffListEmptyAnim.getValue(), 0f, 1f);
        float headerHeight = 15f;
        float itemSpacing = 11f;
        float minWidth = 60f;

        float targetWidth = minWidth;
        for (Staff staff : activeStaff) {
            float nameW = Fonts.SFMEDIUM.get().getWidth(staff.name, 6f);
            targetWidth = Math.max(targetWidth, nameW + 46f);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.SFMEDIUM.get().getWidth("Moderator", 6f) + 46f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());

        float rowCount = Math.max(1f, (float) activeStaff.stream()
                .mapToDouble(staff -> MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f))
                .sum() + (showPlaceholder ? emptyAnimVal : 0f));
        float totalHeight = Math.max(20f, headerHeight + rowCount * itemSpacing);

        Draggable drag = owner.getStaffListDrag();
        float x = drag.getX();
        float y = drag.getY();

        int themeCol = ColorProvider.getThemeColor();
        int bgA = (int) (255 * globalAlpha);
        int blurBg = ColorProvider.rgba(0, 0, 0, (int) (bgA * 0.45f));
        int blurHeader = ColorProvider.rgba(0, 0, 0, bgA);

        DrawUtil.drawRoundBlur(x, y, currentWidth, totalHeight + 2.3f, new org.joml.Vector4f(6f, 6f, 6f, 6f), blurBg, 15f);
        DrawUtil.drawRoundBlur(x, y, currentWidth, headerHeight, new org.joml.Vector4f(6f, 6f, 0f, 0f), blurHeader, 15f);

        float titleW = Fonts.SFMEDIUM.get().getWidth("Staff", 7f);
        float iconWidth = Fonts.ICONS_NURIK.get().getWidth("O", 8f);
        float headBlockW = titleW + 5f + iconWidth;
        float headBlockX = x + Math.max(4.5f, (currentWidth - headBlockW) / 2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Staff", headBlockX, y + (headerHeight - 7f) * 0.5f - 0.5f,
                ColorProvider.setAlpha(themeCol, bgA), 7f);

        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "O", headBlockX + titleW + 5f,
                y + (headerHeight - 8f) * 0.5f + 0.1f, ColorProvider.setAlpha(themeCol, bgA), 8f);

        float rowY = y + headerHeight;
        if (showPlaceholder && emptyAnimVal > 0.001f) {
            drawPouchStaffRow(context, x, rowY, currentWidth,
                    owner.mc.player.getName().getString(),
                    ColorProvider.rgba(85, 255, 140, 235), globalAlpha * emptyAnimVal);
        } else {
            for (Staff staff : activeStaff) {
                float rowAnim = MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f);
                if (rowAnim <= 0.001f) continue;
                int dotColor = getPouchStatusColor(staff, 235);
                drawPouchStaffRow(context, x, rowY + (1f - rowAnim) * 4f, currentWidth, staff.name, dotColor, globalAlpha * rowAnim);
                rowY += itemSpacing * rowAnim;
            }
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawPouchStaffRow(DrawContext context, float x, float rowY, float width, String name, int dotColor, float rowAlpha) {
        if (rowAlpha <= 0.01f) return;
        int aInt = (int) (255 * rowAlpha);

        float headSize = 7f;
        float headX = x + 2.5f;
        float headY = rowY + 2f;
        float textX = x + 12f;
        float statusSize = 5f;
        float statusX = x + width - statusSize - 2.5f;
        float statusY = rowY + 3f;
        float separatorX = statusX - 2f;
        float separatorY = rowY + 3f;

        drawPouchStaffFace(context, name, headX, headY, headSize, aInt);

        DrawUtil.drawRound(separatorX, separatorY, 0.5f, 5f, 0f, ColorProvider.rgba(128, 128, 128, (int) (128 * rowAlpha)));

        float nameW = Fonts.SFMEDIUM.get().getWidth(name, 6f);
        float textAvail = (statusX - 2f) - (x + 12f);
        float nameX = x + 12f + Math.max(0f, (textAvail - nameW) / 2f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, nameX, rowY + 2.5f,
                ColorProvider.rgba(255, 255, 255, aInt), 6f);

        DrawUtil.drawRound(statusX, statusY, statusSize, statusSize, 1.5f,
                ColorProvider.setAlpha(dotColor, aInt));
    }

    private void drawPouchStaffFace(DrawContext context, String name, float x, float y, float size, int alpha) {
        DrawUtil.drawRound(x, y, size, size, 2f, ColorProvider.setAlpha(Hud3Style.BG, (int) (alpha * 0.6f)));
        Builder.border()
                .size(new SizeState(size + 0.5f, size + 0.25f))
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(ColorProvider.setAlpha(Hud3Style.BORDER, (int) (alpha * 0.7f))))
                .thickness(0.5f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
        net.minecraft.util.Identifier skinTexture;
        PlayerListEntry playerEntry = owner.mc.getNetworkHandler().getPlayerListEntry(name);
        if (playerEntry != null) skinTexture = playerEntry.getSkinTextures().texture();
        else skinTexture = DefaultSkinHelper.getTexture();
        int textureId = owner.mc.getTextureManager().getTexture(skinTexture).getGlId();
        Builder.texture()
                .size(new SizeState(size - 2f, size - 2f))
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(ColorProvider.setAlpha(-1, alpha)))
                .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x + 1f, y + 1f);
    }

    private String getPouchStatus(Staff staff) {
        return (staff.status == Status.VANISHED || staff.isSpec) ? "SPEC" : "PLAYING";
    }

    private int getPouchStatusColor(Staff staff, int alpha) {
        return (staff.status == Status.VANISHED || staff.isSpec)
                ? ColorProvider.rgba(255, 75, 75, alpha)
                : ColorProvider.rgba(90, 255, 90, alpha);
    }

    private void renderAxiom(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1f : 0f);
        }

        List<Staff> activeStaff = staffPlayers.stream()
                .filter(staff -> staff.animation.getValue() > 0.01f)
                .toList();
        Set<String> nearPlayerNames = collectNearPlayerNames();

        boolean showPlaceholder = chatOpen && activeStaff.isEmpty();
        staffListEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeStaff.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) staffListEmptyAnim.getValue(), 0f, 1f);
        float headerHeight = 18.0f;
        float rowHeight = 18.0f;
        float rowInset = 4.0f;
        float minWidth = 120.0f;
        float nameSize = 7.0f;
        float metaSize = 5.65f;
        String titleText = "WATCHLIST";
        String subtitleText = activeStaff.isEmpty() ? "no tracked staff" : activeStaff.size() + " online";
        String placeholderText = "No active staff";

        float targetWidth = minWidth;
        for (Staff staff : activeStaff) {
            String tagText = getAxiomStatusLabel(staff, nearPlayerNames);
            String metaText = getAxiomMetaText(staff);
            float rowWidth = 8.0f + 12.0f + 6.0f
                    + Math.max(Fonts.RUBIK.get().getWidth(staff.name, nameSize), Fonts.SFMEDIUM.get().getWidth(metaText, metaSize))
                    + Fonts.RUBIK.get().getWidth(tagText, 5.8f) + 34.0f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.RUBIK.get().getWidth(placeholderText, nameSize) + 24.0f);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) activeStaff.stream().mapToDouble(staff -> rowHeight * (float) staff.animation.getValue()).sum();
        rowsHeight += rowHeight * emptyAnimVal;
        float totalHeight = headerHeight + rowsHeight + 6.0f;

        Draggable drag = owner.getStaffListDrag();
        float x = drag.getX();
        float y = drag.getY();

        owner.drawBackground(x, y, currentWidth, totalHeight, 6.0f, aInt);
        owner.drawAxiomAccent(x + 4.0f, y + 4.0f, 16.5f, headerHeight - 8.0f, 4.0f, aInt);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "O", x + 8.15f, y + 5.25f, owner.getAxiomPrimaryTextColor(aInt), 7.8f);
        DrawUtil.drawText(Fonts.RUBIK.get(), titleText, x + 25.0f, y + 4.0f, owner.getAxiomPrimaryTextColor(aInt), 7.2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), subtitleText, x + 25.0f, y + 10.0f, owner.getAxiomSecondaryTextColor(aInt), 5.7f);

        float currentY = y + headerHeight + 2.0f;
        for (Staff staff : activeStaff) {
            float rowAnim = MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                String tagText = getAxiomStatusLabel(staff, nearPlayerNames);
                int tagColor = getAxiomStatusColor(staff, itemAlpha, nearPlayerNames);
                String metaText = getAxiomMetaText(staff);
                float rowY = currentY;
                float innerWidth = currentWidth - (rowInset * 2.0f);
                float headBoxX = x + rowInset + 2.0f;
                float headBoxY = rowY + 3.0f;
                float headBoxSize = Math.max(9.0f, itemHeight - 6.0f);
                float chipWidth = Fonts.RUBIK.get().getWidth(tagText, 5.8f) + 10.0f;
                float chipX = x + currentWidth - rowInset - chipWidth - 3.0f;
                float chipY = rowY + 3.4f;
                float chipHeight = Math.max(7.0f, itemHeight - 6.8f);
                float textX = x + rowInset + 15.0f;
                float clipWidth = Math.max(20.0f, chipX - textX - 6.0f);

                owner.drawBackground(x + rowInset, rowY, innerWidth, itemHeight - 1.1f, 4.2f, itemAlpha);
                owner.drawAxiomAccent(headBoxX, headBoxY, 10.5f, headBoxSize, 3.0f, itemAlpha);
                renderAxiomHead(context, staff.name, headBoxX + 1.0f, headBoxY + 1.0f, 8.5f, itemAlpha);

                Scissor.push();
                Scissor.setFromComponentCoordinates(textX, rowY, clipWidth, itemHeight);
                DrawUtil.drawText(Fonts.RUBIK.get(), staff.name, textX, rowY + 3.15f, owner.getAxiomPrimaryTextColor(itemAlpha), nameSize);
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), metaText, textX, rowY + 10.1f, owner.getAxiomSecondaryTextColor(itemAlpha), metaSize);
                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawRound(
                        chipX,
                        chipY,
                        chipWidth,
                        chipHeight,
                        3.0f,
                        ColorProvider.setAlpha(tagColor, Math.max(22, itemAlpha / 3)),
                        ColorProvider.setAlpha(tagColor, Math.max(12, itemAlpha / 5))
                );
                DrawUtil.drawText(Fonts.RUBIK.get(), tagText, chipX + 5.0f, rowY + 4.05f, owner.getAxiomPrimaryTextColor(itemAlpha), 5.8f);
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
        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1f : 0f);
        }

        List<Staff> activeStaff = staffPlayers.stream()
                .filter(staff -> staff.animation.getValue() > 0.01f)
                .toList();
        Set<String> nearPlayerNames = collectNearPlayerNames();

        alpha.run(1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int alphaInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float titleSize = 9f;
        float rowFontSize = 7.1f;
        float statusFontSize = 7.1f;
        float headerHeight = 15.5f;
        float rowHeight = 11.5f;
        String headerText = "STAFFLIST";

        float headerTextWidth = Fonts.SFSEMIBOLD.get().getWidth(headerText, titleSize);
        float iconWidth = 13f;
        float minWidth = 2.5f + iconWidth + 2.5f + headerTextWidth + 8f;
        float targetWidth = minWidth;

        for (Staff staff : activeStaff) {
            String statusText = getMoonwardStatus(staff, nearPlayerNames);
            float nameWidth = Fonts.SFBOLD.get().getWidth(staff.name, rowFontSize);
            float statusWidth = Fonts.SFBOLD.get().getWidth(statusText, statusFontSize);
            float rowWidth = 18f + nameWidth + statusWidth + 16f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());
        float rowsHeight = (float) activeStaff.stream().mapToDouble(staff -> rowHeight * (float) staff.animation.getValue()).sum();
        float totalHeight = headerHeight + rowsHeight + 4f;

        Draggable drag = owner.getStaffListDrag();
        float x = drag.getX();
        float y = drag.getY();

        DrawUtil.drawRoundBlur(x - 2f, y + 2, currentWidth + 4f, totalHeight, 4f, ColorProvider.rgba(55, 55, 55, 255), 45);
        DrawUtil.drawRoundBlur(x, y + headerHeight, currentWidth, totalHeight - headerHeight, 2f, ColorProvider.rgba(135, 135, 135, 255), 0);

        float iconX = x + 2.5f;
        float centeredX = x + (currentWidth - headerTextWidth) / 2f;
        float headerTextX = Math.max(centeredX, iconX + iconWidth + 2.5f);

        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue903", iconX, y + 4f, ColorProvider.rgba(222, 222, 222, alphaInt), 13f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), headerText, headerTextX, y + 4.35f, ColorProvider.rgba(222, 222, 222, alphaInt), titleSize);

        float currentY = y + headerHeight;
        for (Staff staff : activeStaff) {
            float rowAnim = (float) staff.animation.getValue();
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowHeight * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (alphaInt * rowAnim), 0, 255);
            if (itemAlpha >= 4) {
                String statusText = getMoonwardStatus(staff, nearPlayerNames);
                int statusColor = getMoonwardStatusColor(staff, itemAlpha, nearPlayerNames);
                float textY = currentY + 3.85f;
                float statusWidth = Fonts.SFSEMIBOLD.get().getWidth(statusText, statusFontSize);
                float statusX = x + currentWidth - statusWidth - 4f;

                float iconX_row = x + 2.75f;
                float iconYBox = currentY + 2.3f;
                float iconSize = 10.5f;

                DrawUtil.drawRound(iconX_row, iconYBox, iconSize, iconSize, 2.2f, ColorProvider.setAlpha(statusColor, itemAlpha));

                PlayerListEntry playerEntry = owner.mc.getNetworkHandler().getPlayerListEntry(staff.name);
                net.minecraft.util.Identifier skinTexture = (playerEntry != null) ? playerEntry.getSkinTextures().texture() : DefaultSkinHelper.getTexture();
                int textureId = owner.mc.getTextureManager().getTexture(skinTexture).getGlId();

                Builder.texture()
                        .size(new SizeState(iconSize - 2, iconSize - 2))
                        .radius(new QuadRadiusState(1.5f))
                        .color(new QuadColorState(ColorProvider.setAlpha(-1, itemAlpha)))
                        .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                        .smoothness(1f)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), iconX_row + 1.45f, iconYBox + 1);

                float clipWidth = statusX - (x + 16f) - 4f;
                Scissor.push();
                Scissor.setFromComponentCoordinates(x + 16f, currentY, clipWidth, itemHeight);
                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), staff.name, x + 16f, textY, ColorProvider.rgba(226, 226, 226, itemAlpha), rowFontSize);
                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), statusText, statusX, textY, statusColor, statusFontSize);
            }
            currentY += itemHeight;
        }

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void renderExp4_0(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1f : 0f);
        }

        List<Staff> activeStaff = staffPlayers.stream()
                .filter(staff -> staff.animation.getValue() > 0.01f)
                .toList();

        boolean showPlaceholder = chatOpen && activeStaff.isEmpty();
        staffListEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeStaff.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) staffListEmptyAnim.getValue(), 0f, 1f);

        final float headerHeight = 20f;
        final float rowHeight = 16f;
        final float nameSize = 7f;
        final float roleSize = 5.5f;
        final float statusSize = 5.5f;
        final float faceSize = 10f;
        final String headerText = "Staff List";

        float targetWidth = 120f;
        for (Staff staff : activeStaff) {
            String role = getExp4Role(staff);
            float bw = 26f;
            if (role != null) bw = Math.max(bw, Fonts.SFBOLD.get().getWidth(role, roleSize) + 6f);
            float nameWidth = Fonts.SFBOLD.get().getWidth(staff.name, nameSize);
            String status = getExp4Status(staff);
            float statusWidth = Fonts.SFBOLD.get().getWidth(status, statusSize) + 14f;
            float rowWidth = 8f + bw + 6f + faceSize + 5f + nameWidth + 8f + statusWidth + 8f;
            targetWidth = Math.max(rowWidth, targetWidth);
        }
        if (emptyAnimVal > 0.001f) {
            String role = "Helper";
            float bw = Math.max(26f, Fonts.SFBOLD.get().getWidth(role, roleSize) + 6f);
            float rowWidth = 8f + bw + 6f + faceSize + 5f + Fonts.SFBOLD.get().getWidth("DeadInside ", nameSize) + 8f + 14f + 8f;
            targetWidth = Math.max(rowWidth, targetWidth);
        }

        widthAnim.run(targetWidth);
        float currentWidth = Math.max(120f, (float) widthAnim.getValue());
        float rowsHeight = (float) activeStaff.stream().mapToDouble(staff -> rowHeight * (float) staff.animation.getValue()).sum();
        rowsHeight += rowHeight * emptyAnimVal;
        float totalHeight = headerHeight + rowsHeight + 4f;

        Draggable drag = owner.getStaffListDrag();
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
        DrawUtil.drawText(Fonts.ICONS2.get(), "J", iconBoxX + 2f, iconBoxY + 1.5f, ColorProvider.rgba(130, 140, 255, aInt), 8f);

        float rowY = y + headerHeight;
        if (showPlaceholder && emptyAnimVal > 0.001f) {
            int rowAlpha = MathHelper.clamp((int) (255 * emptyAnimVal * globalAlpha), 0, 255);
            if (rowAlpha >= 4) {
                drawExp4StaffRow(context, x, rowY, currentWidth, "Helper", ColorProvider.rgba(100, 100, 255, rowAlpha), owner.mc.player.getName().getString(), "PLAYING", ColorProvider.rgba(50, 200, 50, rowAlpha), emptyAnimVal, rowAlpha);
            }
            rowY += rowHeight * emptyAnimVal;
        } else {
            for (Staff staff : activeStaff) {
                float rowAnim = MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f);
                if (rowAnim <= 0.001f) continue;
                int rowAlpha = MathHelper.clamp((int) (255 * rowAnim * globalAlpha), 0, 255);
                if (rowAlpha >= 4) {
                    String role = getExp4Role(staff);
                    int roleColor = getExp4RoleColor(staff, rowAlpha);
                    String status = getExp4Status(staff);
                    int statusColor = getExp4StatusColor(staff, rowAlpha);
                    drawExp4StaffRow(context, x, rowY, currentWidth, role, roleColor, staff.name, status, statusColor, rowAnim, rowAlpha);
                }
                rowY += rowHeight * rowAnim;
            }
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private void drawExp4StaffRow(DrawContext context, float x, float rowY, float width, String role, int roleColor, String name, String status, int statusColor, float animation, int alpha) {
        float rowHeight = 16f;
        float centerY = rowY + (rowHeight / 2f);

        float bw = 26f;
        if (role != null) bw = Math.max(bw, Fonts.SFBOLD.get().getWidth(role, 5.5f) + 6f);

        float faceSize = 10f;
        float fx = x + 8f + bw + 6f;
        float nx = fx + faceSize + 5f;
        String cleanName = (name == null || name.isEmpty()) ? "Staff" : name;
        DrawUtil.drawText(Fonts.SFBOLD.get(), cleanName, nx, centerY - 3f, ColorProvider.rgba(255, 255, 255, alpha), 7f);

        int roleBgAlpha = (int) (alpha * 0.2f);
        DrawUtil.drawRound(x + 8f, centerY - 4f, bw, 8f, 2f,
                role != null ? ColorProvider.setAlpha(roleColor, Math.max(8, roleBgAlpha))
                        : ColorProvider.rgba(30, 30, 35, Math.max(8, (int) (alpha * 0.4f))));
        if (role != null) {
            float roleX = x + 8f + (bw - Fonts.SFBOLD.get().getWidth(role, 5.5f)) / 2f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), role, roleX, centerY - 3f, roleColor, 5.5f);
        }

        float sw = Fonts.SFBOLD.get().getWidth(status, 5.5f) + 14f;
        float sx = x + width - sw - 8f;
        int sbAlpha = Math.max(8, (int) (alpha * 0.15f));
        DrawUtil.drawRound(sx, centerY - 4.25f, sw, 8.5f, 3f, ColorProvider.setAlpha(statusColor, sbAlpha));
        DrawUtil.drawText(Fonts.ICONS2.get(), "F", sx + 3f, centerY - 4f, statusColor, 6.5f);
        DrawUtil.drawText(Fonts.SFBOLD.get(), status, sx + 11f, centerY - 3f, statusColor, 5.5f);

        PlayerListEntry playerEntry = owner.mc.getNetworkHandler().getPlayerListEntry(cleanName);
        net.minecraft.util.Identifier skinTexture = (playerEntry != null) ? playerEntry.getSkinTextures().texture() : DefaultSkinHelper.getTexture();
        int textureId = owner.mc.getTextureManager().getTexture(skinTexture).getGlId();
        Builder.texture()
                .size(new SizeState(faceSize, faceSize))
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(ColorProvider.setAlpha(-1, alpha)))
                .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), fx, centerY - faceSize / 2f);
    }

    private String getExp4Role(Staff staff) {
        if (staff.prefix == null) return null;
        String prefix = staff.prefix.getString()
                .replace(staff.name, "")
                .replaceAll("§[0-9A-FK-OR]", "")
                .trim();
        if (prefix.isEmpty()) return null;
        return prefix.length() > 12 ? prefix.substring(0, 11) + "…" : prefix;
    }

    private int getExp4RoleColor(Staff staff, int alpha) {
        String prefix = staff.prefix == null ? "" : staff.prefix.getString().toLowerCase(Locale.ROOT);
        if (prefix.contains("owner") || prefix.contains("владел")) return ColorProvider.rgba(255, 50, 50, alpha);
        if (prefix.contains("admin") || prefix.contains("админ")) return ColorProvider.rgba(255, 75, 75, alpha);
        if (prefix.contains("mod") || prefix.contains("модер")) return ColorProvider.rgba(75, 150, 255, alpha);
        if (prefix.contains("helper") || prefix.contains("хелп")) return ColorProvider.rgba(100, 100, 255, alpha);
        if (prefix.contains("trainee") || prefix.contains("стажер")) return ColorProvider.rgba(150, 255, 150, alpha);
        if (prefix.contains("curat") || prefix.contains("куратор")) return ColorProvider.rgba(255, 150, 50, alpha);
        if (prefix.contains("dev") || prefix.contains("разраб")) return ColorProvider.rgba(150, 100, 255, alpha);
        if (prefix.contains("tester")) return ColorProvider.rgba(255, 150, 255, alpha);
        if (prefix.contains("support") || prefix.contains("саппорт")) return ColorProvider.rgba(100, 255, 200, alpha);
        if (prefix.contains("yt") || prefix.contains("youtube") || prefix.contains("ютуб")) return ColorProvider.rgba(255, 50, 50, alpha);
        return ColorProvider.rgba(150, 150, 150, alpha);
    }

    private String getExp4Status(Staff staff) {
        return (staff.status == Status.VANISHED || staff.isSpec) ? "SPEC" : "PLAYING";
    }

    private int getExp4StatusColor(Staff staff, int alpha) {
        return (staff.status == Status.VANISHED || staff.isSpec)
                ? ColorProvider.rgba(255, 75, 75, alpha)
                : ColorProvider.rgba(50, 200, 50, alpha);
    }

    public void update() {
        for (Staff staff : staffPlayers) {
            staff.isOnServer = false;
        }

        for (PlayerListEntry playerListEntry : owner.mc.getNetworkHandler().getPlayerList()) {
            String name = playerListEntry.getProfile().getName().replaceAll("[\\[\\]]", "");
            PlayerListEntry info = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(name);
            boolean vanish = info == null;
            boolean isGM3 = info != null && info.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;

            boolean matchesPrefix = prefixMatches.matcher(playerListEntry.getDisplayName() != null ? playerListEntry.getDisplayName().getString().toLowerCase(Locale.ROOT) : "").matches();
            boolean isValidName = namePattern.matcher(name).matches();
            boolean notSelf = !name.equals(MinecraftClient.getInstance().player.getName().getString());

            if ((isValidName && notSelf && matchesPrefix) || (isValidName && notSelf && vanish) || StaffManager.isStaff(name)) {
                if (StaffManager.isStaff(name)) {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null
                            && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw")
                            || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (String n : names) {
                            if (name.contains(n)) {
                                contains = true;
                                break;
                            }
                        }
                    }
                    if (contains) continue;
                }

                Optional<Staff> existingStaff = staffPlayers.stream().filter(s -> s.name.equals(name)).findFirst();
                Status status = vanish ? Status.VANISHED : (isGM3 ? Status.VANISHED : Status.NONE);

                if (existingStaff.isPresent()) {
                    Staff s = existingStaff.get();
                    s.isOnServer = true;
                    s.status = status;
                } else {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null
                            && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw")
                            || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (String n : names) {
                            if (name.contains(n)) {
                                contains = true;
                            }
                        }
                    }
                    if (!contains) {
                        net.minecraft.text.Text originalPrefix = playerListEntry.getDisplayName();
                        net.minecraft.text.Text prefix = originalPrefix;
                        if (prefix != null) {
                            prefix = ReplaceUtil.replaceSymbols(prefix);
                            String fullString = prefix.getString();
                            int nickIndex = fullString.indexOf(name);
                            if (nickIndex != -1) {
                                int endIndex = nickIndex + name.length();
                                if (endIndex < fullString.length()) {
                                    net.minecraft.text.MutableText newText = net.minecraft.text.Text.empty();
                                    int currentLength = 0;
                                    net.minecraft.text.MutableText baseCopy = prefix.copy();
                                    baseCopy.getSiblings().clear();
                                    String mainContent = baseCopy.getString();
                                    if (!mainContent.isEmpty() && currentLength < endIndex) {
                                        int takeLength = Math.min(mainContent.length(), endIndex - currentLength);
                                        newText.append(net.minecraft.text.Text.literal(mainContent.substring(0, takeLength)).setStyle(prefix.getStyle()));
                                        currentLength += takeLength;
                                    }
                                    for (net.minecraft.text.Text sibling : prefix.getSiblings()) {
                                        if (currentLength >= endIndex) break;
                                        net.minecraft.text.MutableText siblingCopy = sibling.copy();
                                        siblingCopy.getSiblings().clear();
                                        String siblingContent = siblingCopy.getString();
                                        int takeLength = Math.min(siblingContent.length(), endIndex - currentLength);
                                        if (takeLength > 0) {
                                            newText.append(net.minecraft.text.Text.literal(siblingContent.substring(0, takeLength)).setStyle(sibling.getStyle()));
                                            currentLength += takeLength;
                                        }
                                    }
                                    prefix = newText;
                                }
                            }
                        }
                        Staff staff = new Staff(prefix == null ? net.minecraft.text.Text.of(playerListEntry.getProfile().getName()) : prefix, name, vanish || isGM3, status);
                        staff.isOnServer = true;
                        staffPlayers.add(staff);
                    }
                }
            }
        }
        staffPlayers.removeIf(staff -> !staff.isOnServer && staff.animation.getValue() == 0);
    }

    private void renderCelestial(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1 : 0);
        }
        List<Staff> activeStaff = staffPlayers.stream().filter(s -> s.animation.getValue() > 0.01f).toList();
        Set<String> nearPlayerNames = collectNearPlayerNames();

        boolean showPlaceholder = chatOpen && activeStaff.isEmpty();
        staffListEmptyAnim.run(showPlaceholder ? 1f : 0f);
        alpha.run((activeStaff.isEmpty() && !chatOpen) ? 0f : 1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);
        float emptyAnimVal = MathHelper.clamp((float) staffListEmptyAnim.getValue(), 0f, 1f);

        final String headerText = "Staff Online";
        final String placeholderText = "No active staff";
        final float fontSize = 7.5f;
        final float headerH = 14f;
        final float rowH = 9.5f;
        final float padL = 5f;
        final float padR = 5f;

        float targetWidth = 70f;
        for (Staff staff : activeStaff) {
            float prefixW = Fonts.CELESTIAL.get().getWidth(staff.prefix, fontSize);
            float nameW = Fonts.CELESTIAL.get().getWidth(" " + staff.name, fontSize);
            float rowWidth = padL + prefixW + nameW + 14f + padR;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (emptyAnimVal > 0.001f) {
            targetWidth = Math.max(targetWidth, Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize) + 14f);
        }

        widthAnim.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim.getValue());
        float rowsHeight = (float) activeStaff.stream().mapToDouble(s -> rowH * MathHelper.clamp((float) s.animation.getValue(), 0f, 1f)).sum();
        rowsHeight += rowH * emptyAnimVal;
        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        Draggable drag = owner.getStaffListDrag();
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
        for (Staff staff : activeStaff) {
            float rowAnim = MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;
            float itemH = rowH * rowAnim;
            int itemA = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);
            if (itemA >= 4) {
                float textY = curY + (itemH / 2f) - (fontSize / 2f) - 1f;
                DrawUtil.drawText(Fonts.CELESTIAL.get(), staff.prefix, x + padL, textY, fontSize, itemA);
                float prefixW = Fonts.CELESTIAL.get().getWidth(staff.prefix, fontSize);
                DrawUtil.drawText(Fonts.CELESTIAL.get(), " " + staff.name, x + padL + prefixW, textY, ColorProvider.rgba(220, 220, 220, itemA), fontSize);

                boolean inNear = nearPlayerNames.contains(staff.name);
                int statusColor;
                if (staff.status == Status.VANISHED || staff.isSpec) statusColor = ColorProvider.rgba(255, 50, 50, itemA);
                else if (inNear) statusColor = ColorProvider.rgba(255, 215, 0, itemA);
                else statusColor = ColorProvider.rgba(50, 255, 50, itemA);

                float r = 2.5f;
                float cx = x + curW - padR - (r * 2f);
                float cy = curY + (itemH / 2f) - r;
                DrawUtil.drawRound(cx, cy, r * 2f, r * 2f, r, statusColor);
            }
            curY += itemH;
        }

        if (emptyAnimVal > 0.001f) {
            float itemH = rowH * emptyAnimVal;
            int itemA = MathHelper.clamp((int) (aInt * emptyAnimVal), 0, 255);
            if (itemA >= 4) {
                float textY = curY + (itemH / 2f) - (fontSize / 2f) - 1;
                float textX = x + (curW - Fonts.CELESTIAL.get().getWidth(placeholderText, fontSize)) / 2f;
                DrawUtil.drawText(Fonts.CELESTIAL.get(), placeholderText, textX, textY, ColorProvider.rgba(255, 205, 70, itemA), fontSize);
            }
            curY += itemH;
        }

        drag.setWidth(curW);
        drag.setHeight(totalH);
    }

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getStaffListDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        float defaultWidth = 64;
        float height = 14.5f;

        boolean isFound = false;
        if (!staffPlayers.isEmpty()) {
            alpha.run(1);
            isFound = true;
        }
        if (!isFound && !(owner.mc.currentScreen instanceof ChatScreen)) alpha.run(0);
        if (owner.mc.currentScreen instanceof ChatScreen) alpha.run(1);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;
        int headerAlpha = MathHelper.clamp((int) (255 * globalAlpha), 0, 255);

        owner.drawBackground(posX, posY, (float) widthAnim.getValue(), 14.5f, 3, headerAlpha);
        DrawUtil.drawRound(posX + 15.25f, posY + 2, 0.5f, 10.5f, 0, ColorProvider.rgba(88, 88, 88, headerAlpha));
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "O", posX + 4.25f, posY + 4.5f, ColorProvider.setAlpha(-1, headerAlpha), 8.5f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Staff Online", posX + 19.5f, posY + 3.25f, ColorProvider.rgba(255, 255, 255, headerAlpha), 7.5f);

        posY += 14.5f;
        float headOffset = 12f;

        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1 : 0);
            float localBindWidth = headOffset + Fonts.SFREGULAR.get().getWidth(staff.prefix, 6.75f) + Fonts.SFREGULAR.get().getWidth(staff.status.string, 6.75f);
            if (localBindWidth > defaultWidth) defaultWidth = localBindWidth + 15;
        }

        for (Staff staff : staffPlayers) {
            float animVal = (float) staff.animation.getValue();
            if (animVal <= 0.001f) continue;

            float itemHeight = 11 * Math.min(1.0f, animVal);
            height += itemHeight;
            int itemAlpha = MathHelper.clamp((int) (255 * Math.min(1.0f, Math.max(0.0f, animVal)) * globalAlpha), 0, 255);
            if (itemAlpha < 5) {
                posY += itemHeight;
                continue;
            }

            String name = staff.name;
            net.minecraft.text.Text prefix = staff.prefix;
            float textYOffset = (itemHeight / 2f) - 3f;

            owner.drawBackground(posX, posY, (float) widthAnim.getValue(), itemHeight, 3, itemAlpha);
            DrawUtil.drawRound((float) (posX + widthAnim.getValue() - 11.25f), posY + 2, 0.5f, itemHeight - 4, 0, ColorProvider.rgba(125, 125, 125, itemAlpha));

            float headSize = 8f;
            float headX = posX + 3f;
            float headY = posY + textYOffset - 1f;

            net.minecraft.util.Identifier skinTexture;
            PlayerListEntry playerEntry = owner.mc.getNetworkHandler().getPlayerListEntry(name);
            if (playerEntry != null) skinTexture = playerEntry.getSkinTextures().texture();
            else skinTexture = DefaultSkinHelper.getTexture();
            int textureId = owner.mc.getTextureManager().getTexture(skinTexture).getGlId();

            Builder.texture()
                    .size(new SizeState(headSize, headSize))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(ColorProvider.setAlpha(-1, itemAlpha)))
                    .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                    .smoothness(1f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), headX, headY);

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), prefix, posX + 2f + headOffset, posY + textYOffset - 0.5f, 6.5f, itemAlpha);
            DrawUtil.drawRound((float) (posX + widthAnim.getValue() - 8), posY + textYOffset + 1f, 5, 5, 2,
                    staff.status == Status.NONE ? ColorProvider.rgba(32, 255, 32, itemAlpha) : ColorProvider.rgba(255, 32, 32, itemAlpha));

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

    private String getMoonwardStatus(Staff staff, Set<String> nearPlayerNames) {
        if (staff.status == Status.VANISHED || staff.isSpec) {
            return "SPEC";
        }
        if (nearPlayerNames.contains(staff.name)) {
            return "NEAR";
        }
        return "ACTIVE";
    }

    private int getMoonwardStatusColor(Staff staff, int alpha, Set<String> nearPlayerNames) {
        if (staff.status == Status.VANISHED || staff.isSpec) {
            return ColorProvider.rgba(229, 57, 53, alpha);
        }
        if (nearPlayerNames.contains(staff.name)) {
            return ColorProvider.rgba(226, 214, 117, alpha);
        }
        return ColorProvider.rgba(134, 255, 96, alpha);
    }

    private String getAxiomStatusLabel(Staff staff, Set<String> nearPlayerNames) {
        if (staff.status == Status.VANISHED || staff.isSpec) {
            return "SPEC";
        }
        if (nearPlayerNames.contains(staff.name)) {
            return "NEAR";
        }
        return "LIVE";
    }

    private int getAxiomStatusColor(Staff staff, int alpha, Set<String> nearPlayerNames) {
        if (staff.status == Status.VANISHED || staff.isSpec) {
            return ColorProvider.rgba(255, 105, 105, alpha);
        }
        if (nearPlayerNames.contains(staff.name)) {
            return ColorProvider.rgba(255, 214, 102, alpha);
        }
        return ColorProvider.rgba(124, 255, 170, alpha);
    }

    private String getAxiomMetaText(Staff staff) {
        String prefixText = staff.prefix == null ? "" : staff.prefix.getString().replace(staff.name, "").trim();
        if (prefixText.isEmpty()) {
            return staff.status == Status.VANISHED || staff.isSpec ? "spectator / hidden" : "online on server";
        }
        return prefixText.length() > 30 ? prefixText.substring(0, 27) + "..." : prefixText;
    }

    private void renderAxiomHead(DrawContext context, String name, float x, float y, float size, int alpha) {
        net.minecraft.util.Identifier skinTexture;
        PlayerListEntry playerEntry = owner.mc.getNetworkHandler().getPlayerListEntry(name);
        if (playerEntry != null) {
            skinTexture = playerEntry.getSkinTextures().texture();
        } else {
            skinTexture = DefaultSkinHelper.getTexture();
        }
        int textureId = owner.mc.getTextureManager().getTexture(skinTexture).getGlId();

        Builder.texture()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(2f))
                .color(new QuadColorState(ColorProvider.setAlpha(-1, alpha)))
                .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);
    }

    private Set<String> collectNearPlayerNames() {
        if (owner.mc.world == null) {
            return Set.of();
        }
        Set<String> nearPlayerNames = new HashSet<>();
        owner.mc.world.getPlayers().forEach(player -> nearPlayerNames.add(player.getName().getString()));
        return nearPlayerNames;
    }

    private enum Status {
        NONE("", -1),
        VANISHED("SPEC", ColorProvider.rgba(229, 0, 63, 255));

        private final String string;
        private final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }

    private static class Staff {
        net.minecraft.text.Text prefix;
        String name;
        boolean isSpec;
        Status status;
        boolean isOnServer;
        Animation animation = new Animation(Easing.EXPO_OUT, 233);

        Staff(net.minecraft.text.Text prefix, String name, boolean isSpec, Status status) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
            this.status = status;
        }
    }
}
