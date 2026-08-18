package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.math.Counter;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.server.Server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkRenderer {
    private static final Identifier ICON_TEXTURE = Identifier.of("mre", "icon.png");
    private static final float NURSULTAN_HEIGHT = 15f;
    private static final float NURSULTAN_GAP = 3f;
    private static final float NURSULTAN_MENU_WIDTH = 104f;
    private static final float NURSULTAN_MENU_ROW_HEIGHT = 11.5f;
    private static final float NURSULTAN_MENU_PADDING = 5f;
    private static final float NURSULTAN_MENU_TOGGLE_WIDTH = 24f;
    private static final float NURSULTAN_MENU_TOGGLE_HEIGHT = 10.5f;
    private static final float NURSULTAN_MENU_TOGGLE_RADIUS = 3.2f;
    private static final float NURSULTAN_MENU_TOGGLE_KNOB_PADDING = 0.55f;
    private static final float NURSULTAN_MENU_TOGGLE_KNOB_REDUCTION = 0.5f;
    private static final int NURSULTAN_MENU_TOGGLE_COUNT = 8;
    private static final int NURSULTAN_TRANSPARENT_TOGGLE_INDEX = NURSULTAN_MENU_TOGGLE_COUNT;
    private static final int NURSULTAN_WATERMARK_BLUR_INDEX = NURSULTAN_MENU_TOGGLE_COUNT + 1;
    private static final int NURSULTAN_WATERMARK_THEME_INDEX = NURSULTAN_MENU_TOGGLE_COUNT + 2;

    private final Interface owner;
    private final AnimatedStatText pingAnimatedText = new AnimatedStatText(1550L);
    private final AnimatedStatText fpsAnimatedText = new AnimatedStatText(1550L);
    private final AnimatedStatText timeAnimatedText = new AnimatedStatText(1550L);
    private final AnimatedStatText speedAnimatedText = new AnimatedStatText(900L);
    private final Animation pouchWidthAnimation = new Animation(Easing.CUBIC_OUT, 350);
    private final Animation pouchAlphaAnimation = new Animation(Easing.CUBIC_OUT, 300);
    private String exp4LastFps = "";
    private String exp4OldFps = "";
    private String exp4LastTime = "";
    private String exp4OldTime = "";
    private long exp4FpsStart;
    private long exp4TimeStart;
    private boolean nursultanMenuOpen;
    private float nursultanMenuX;
    private float nursultanMenuY;
    private float nursultanMenuWidth;
    private float nursultanMenuHeight;
    private float nursultanMenuAnchorX = Float.NaN;
    private float nursultanMenuAnchorY = Float.NaN;
    private final float[] nursultanToggleProgress = new float[NURSULTAN_MENU_TOGGLE_COUNT + 3];
    private float nursultanModeProgress;

    public WatermarkRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        if (owner.getHudStyleSetting().is("Funtime")) {
            renderFuntime(context);
        } else if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context);
        } else if (owner.getHudStyleSetting().is("Celestial")) {
            renderCelestial(context);
        } else if (owner.getHudStyleSetting().is("Pharaon")) {
            renderMoonward(context);
        } else if (owner.isAxiomStyle()) {
            renderAxiom(context);
        } else if (owner.getHudStyleSetting().is("Exp4.0")) {
            renderExp4_0(context);
        } else {
            renderNursultan(context);
        }
    }

    private void renderFuntime(DrawContext context) {
        Counter.updateFPS();

        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();

        int accent = ColorProvider.getThemeColor();
        String ping = Server.getPing(owner.mc.player) + " ms";
        String fps = Counter.getCurrentFPS() + " FPS";

        float sz = 8f;
        float icoSz = 9f;
        float pad = 10f;
        float gap = 7f;

        float icoW = icoSz;
        float sepW = Fonts.SFMEDIUM.get().getWidth("/", sz);
        float pingW = Fonts.SFMEDIUM.get().getWidth(ping, sz);
        float fpsW = Fonts.SFMEDIUM.get().getWidth(fps, sz);

        float w = pad + icoW + gap + pingW + gap + sepW + gap + fpsW + pad;
        float h = 7 * 2 + sz;
        drag.setWidth(w);
        drag.setHeight(h);

        FuntimeHud.drawPanel(x, y, w, h, 1f);

        float icoX = x + pad;
        float icoY = y + (h - icoSz) / 2f;
        FuntimeHud.drawGlowAccent(icoX, icoY, icoSz, accent, 1f);
        FuntimeHud.drawIconCell(icoX, icoY, icoSz, accent, 1f);

        float cx = icoX;
        float textY = y + (h - sz) / 2f;

        DrawUtil.drawImage(context.getMatrices(), ICON_TEXTURE, icoX, icoY, icoSz, icoSz, 0xFFFFFFFF);
        cx += icoW + gap;

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), ping, cx, textY, FuntimeHud.TEXT, sz);
        cx += pingW + gap;

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "/", cx, textY, FuntimeHud.SEP, sz);
        cx += sepW + gap;

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fps, cx, textY, FuntimeHud.TEXT, sz);
    }

    private void renderPouchOld(DrawContext context) {
        Counter.updateFPS();

        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();
        float textSize = 7f;
        float padding = 6f;
        float iconSize = 12f;
        float textGap = 6f;
        float textY = y + 6f;

        String username = Onetap.getInstance().getAuthManager().getDiscordUsername();
        if (username == null || username.isEmpty()) username = owner.mc.getSession().getUsername();
        String stats = username + "  |  " + Counter.getCurrentFPS() + " FPS  |  "
                + Server.getPing(owner.mc.player) + " Ping  |  "
                + String.format("%.0f TPS", Onetap.getInstance().getTpsGetter().getTPS());

        float statsWidth = Fonts.SFMEDIUM.get().getWidth(stats, textSize);
        float blockW = iconSize + textGap + statsWidth;

        float width = Math.max(96f, padding + blockW + padding + 4f);
        float height = 20f;

        pouchWidthAnimation.run(width);
        pouchAlphaAnimation.run(1f);
        float alpha = (float) pouchAlphaAnimation.getValue();
        if (alpha <= 0.01f) {
            drag.setHeight(0);
            return;
        }
        float animatedWidth = Math.max(width, (float) pouchWidthAnimation.getValue());
        int alphaInt = (int) (255 * alpha);
        float panelX = x;
        float panelWidth = animatedWidth;
        float blockX = panelX + Math.max(padding, (panelWidth - blockW) * 0.5f - 14f);
        float glowExt = 6f;
        int glowColor = ColorProvider.rgba(63, 135, 245, Math.min(80, alphaInt));
        DrawUtil.drawRoundBlur(panelX - glowExt, y - glowExt, panelWidth + glowExt * 2f, height + glowExt * 2f,
                6f, glowColor, 20f);
        Hud3Style.drawPanel(panelX, y, panelWidth, height, false, alpha);

        float iconX = blockX;
        DrawUtil.drawImage(context.getMatrices(), ICON_TEXTURE, iconX, y + 4f, iconSize, iconSize,
                ColorProvider.setAlpha(0xFFFFFFFF, alphaInt));
        float statsX = iconX + iconSize + textGap;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), stats, statsX, textY,
                ColorProvider.rgba(235, 238, 245, alphaInt), textSize);

        drag.setWidth(panelWidth);
        drag.setHeight(height);
    }

    private void drawPouchBlock(float x, float y, float w, float h, int bg, int border) {
        DrawUtil.drawRound(x, y, w, h, 3f, bg);
        Builder.border()
                .size(new SizeState(w + 0.5f, h + 0.25f))
                .radius(new QuadRadiusState(3f))
                .color(new QuadColorState(border))
                .thickness(0.5f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
    }

    private String getBlockCoords() {
        if (owner.mc.player == null) return "X 0 Y 0 Z 0";
        int x = (int) Math.floor(owner.mc.player.getX());
        int y = (int) Math.floor(owner.mc.player.getY());
        int z = (int) Math.floor(owner.mc.player.getZ());
        return "X " + x + " Y " + y + " Z " + z;
    }

    

    private void renderExp4_0(DrawContext context) {
        Counter.updateFPS();

        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();

        String username = owner.mc.getSession().getUsername();
        String fpsNumber = String.valueOf(Counter.getCurrentFPS());
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String ping = Server.getPing(owner.mc.player) + "ms";
        String server = owner.mc.getCurrentServerEntry() != null && owner.mc.getCurrentServerEntry().address != null
                ? owner.mc.getCurrentServerEntry().address
                : "localhost";

        long currentTime = System.currentTimeMillis();

        if (!fpsNumber.equals(exp4LastFps)) {
            exp4OldFps = exp4LastFps;
            exp4LastFps = fpsNumber;
            exp4FpsStart = currentTime;
        }
        if (!time.equals(exp4LastTime)) {
            exp4OldTime = exp4LastTime;
            exp4LastTime = time;
            exp4TimeStart = currentTime;
        }

        float fpsAnimation = Math.min(1.0f, (currentTime - exp4FpsStart) / 200.0f);
        float timeAnimation = Math.min(1.0f, (currentTime - exp4TimeStart) / 200.0f);

        float textSize = 6f;
        float spacing = 7.5f;
        float padding = 5f;
        float iconTextGap = 3.0f;
        float iconSize = 8.5f;

        float usernameWidth = Fonts.SFBOLD.get().getWidth(username, textSize);
        float pingWidth = Fonts.SFBOLD.get().getWidth(ping, textSize);
        float fpsWidth = Fonts.SFBOLD.get().getWidth(fpsNumber + " FPS", textSize);
        float timeWidth = Fonts.SFBOLD.get().getWidth(time, textSize);
        float serverWidth = Fonts.SFBOLD.get().getWidth(server, textSize);

        float totalWidth = padding * 2 + spacing * 2;
        totalWidth += iconSize + iconTextGap;
        totalWidth += iconSize + iconTextGap;
        totalWidth += usernameWidth + spacing;
        totalWidth += iconSize + iconTextGap + pingWidth + spacing;
        totalWidth += iconSize + iconTextGap + fpsWidth + spacing;
        totalWidth += iconSize + iconTextGap + timeWidth + spacing;
        totalWidth += iconSize + iconTextGap + serverWidth;

        float height = 15f;

        int iconColor = ColorProvider.rgba(135, 145, 255, 255);
        int textColor = ColorProvider.rgba(255, 255, 255, 255);

        DrawUtil.drawRound(x, y, totalWidth, height, 5f,
                ColorProvider.rgba(20, 20, 20, 230),
                ColorProvider.rgba(15, 15, 15, 230),
                ColorProvider.rgba(20, 20, 20, 230),
                ColorProvider.rgba(15, 15, 15, 230));
        owner.drawExp4Border(x, y, totalWidth, height, 5f, 230);

        float textY = y + 5.0f;
        float iconY = y + 2.5f;
        float logoIconY = y + 3.5f;
        float logoAY = y + 2.0f;
        float offsetX = x + padding + 2.0f;

        DrawUtil.drawText(Fonts.ICONS2.get(), "A", offsetX, logoAY, iconColor, 8.5f);
        offsetX += iconSize + spacing;

        DrawUtil.drawText(Fonts.ICONS2.get(), "d", offsetX, logoIconY, iconColor, 8.5f);
        offsetX += iconSize + iconTextGap;

        DrawUtil.drawText(Fonts.SFBOLD.get(), username, offsetX, textY, textColor, textSize);
        offsetX += usernameWidth + spacing;

        DrawUtil.drawText(Fonts.ICONS2.get(), "K", offsetX, iconY, iconColor, 8.5f);
        offsetX += iconSize + iconTextGap;
        DrawUtil.drawText(Fonts.SFBOLD.get(), ping, offsetX, textY, textColor, textSize);
        offsetX += pingWidth + spacing;

        DrawUtil.drawText(Fonts.ICONS2.get(), "C", offsetX, iconY, iconColor, 8.5f);
        offsetX += iconSize + iconTextGap;
        drawExp4AnimatedPerChar(fpsNumber, exp4OldFps, offsetX, textY, textSize, fpsAnimation);
        offsetX += Fonts.SFBOLD.get().getWidth(fpsNumber, textSize);
        DrawUtil.drawText(Fonts.SFBOLD.get(), " FPS", offsetX, textY, textColor, textSize);
        offsetX += Fonts.SFBOLD.get().getWidth(" FPS", textSize) + spacing;

        DrawUtil.drawText(Fonts.ICONS2.get(), "A", offsetX, iconY, iconColor, 8.5f);
        offsetX += iconSize + iconTextGap;
        drawExp4AnimatedPerChar(time, exp4OldTime, offsetX, textY, textSize, timeAnimation);
        offsetX += timeWidth + spacing;

        DrawUtil.drawText(Fonts.ICONS2.get(), "B", offsetX, iconY, iconColor, 8.5f);
        offsetX += iconSize + iconTextGap;
        DrawUtil.drawText(Fonts.SFBOLD.get(), server, offsetX, textY, textColor, textSize);

        drag.setWidth(totalWidth);
        drag.setHeight(height);
    }

    private void drawExp4AnimatedPerChar(String newText, String oldText, float x, float y, float size, float progress) {
        if (oldText == null || oldText.isEmpty() || progress >= 1.0f) {
            DrawUtil.drawText(Fonts.SFBOLD.get(), newText, x, y, ColorProvider.rgba(255, 255, 255, 255), size);
            return;
        }

        float offsetX = x;
        int maxLen = Math.max(newText.length(), oldText.length());
        String paddedNew = padLeft(newText, maxLen);
        String paddedOld = padLeft(oldText, maxLen);

        for (int i = 0; i < paddedNew.length(); i++) {
            char newChar = paddedNew.charAt(i);
            char oldChar = paddedOld.charAt(i);
            if (newChar == ' ' && oldChar == ' ') continue;

            float charWidth = Fonts.SFBOLD.get().getWidth(String.valueOf(newChar != ' ' ? newChar : oldChar), size);
            boolean isNewDigit = Character.isDigit(newChar) || newChar == '.';
            boolean isOldDigit = Character.isDigit(oldChar) || oldChar == '.';
            boolean hasChanged = newChar != oldChar;

            if (!hasChanged || (!isNewDigit && !isOldDigit)) {
                if (newChar != ' ') {
                    DrawUtil.drawText(Fonts.SFBOLD.get(), String.valueOf(newChar), offsetX, y, ColorProvider.rgba(255, 255, 255, 255), size);
                }
            } else {
                float easedProgress = 1.0f - (float) Math.pow(1.0 - progress, 3);
                if (oldChar != ' ' && isOldDigit) {
                    float oldAlpha = 1.0f - easedProgress;
                    float oldOffsetY = easedProgress * 8.0f;
                    int alpha = Math.max(0, Math.min(255, (int) (oldAlpha * 255)));
                    if (alpha > 0) DrawUtil.drawText(Fonts.SFBOLD.get(), String.valueOf(oldChar), offsetX, y + oldOffsetY, ColorProvider.rgba(255, 255, 255, alpha), size);
                }
                if (newChar != ' ' && isNewDigit) {
                    float newAlpha = easedProgress;
                    float newOffsetY = (1.0f - easedProgress) * -8.0f;
                    int alpha = Math.max(0, Math.min(255, (int) (newAlpha * 255)));
                    if (alpha > 0) DrawUtil.drawText(Fonts.SFBOLD.get(), String.valueOf(newChar), offsetX, y + newOffsetY, ColorProvider.rgba(255, 255, 255, alpha), size);
                }
            }
            if (newChar != ' ') offsetX += charWidth;
        }
    }

    private static String padLeft(String text, int length) {
        if (text.length() >= length) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - text.length(); i++) sb.append(" ");
        sb.append(text);
        return sb.toString();
    }

    private void renderAxiom(DrawContext context) {
        Counter.updateFPS();

        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();

        String titleText = "";
        String styleTag = "AXIOM";
        String userText = trimAxiomText(Onetap.getInstance().getAuthManager().getDiscordUsername(), 18);
        String serverText = trimAxiomText(
                owner.mc.getCurrentServerEntry() != null && owner.mc.getCurrentServerEntry().address != null
                        ? owner.mc.getCurrentServerEntry().address
                        : "Singleplayer",
                26
        );
        String pingText = Server.getPing(owner.mc.player) + " ms";
        String fpsText = Counter.getCurrentFPS() + " fps";
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        double deltaX = owner.mc.player.getX() - owner.mc.player.prevX;
        double deltaZ = owner.mc.player.getZ() - owner.mc.player.prevZ;
        String speedText = String.format("%.1f b/s", Math.hypot(deltaX, deltaZ) * 20.0);

        pingAnimatedText.update(pingText);
        fpsAnimatedText.update(fpsText);
        timeAnimatedText.update(timeText);
        speedAnimatedText.update(speedText);

        float rowHeight = 16.5f;
        float rowGap = 3.0f;
        float titleSize = 8.75f;
        float userSize = 7.05f;
        float serverSize = 6.95f;
        float statLabelSize = 5.8f;
        float statValueSize = 7.0f;
        float badgeWidth = 18.0f;

        int primaryText = owner.getAxiomPrimaryTextColor(255);
        int secondaryText = owner.getAxiomSecondaryTextColor(255);
        int dividerColor = ColorProvider.rgba(255, 255, 255, 54);

        float titleX = x + 4.0f + badgeWidth + 5.0f;
        float titleWidth = Fonts.RUBIK.get().getWidth(titleText, titleSize);
        float separatorOneX = titleX + titleWidth + 6.0f;
        float userX = separatorOneX + 6.0f;
        float userWidth = Fonts.SFSEMIBOLD.get().getWidth(userText, userSize);
        float separatorTwoX = userX + userWidth + 6.0f;
        float serverX = separatorTwoX + 6.0f;
        float serverWidth = Fonts.SFMEDIUM.get().getWidth(serverText, serverSize);
        float rowOneContentWidth = serverX - x + serverWidth + 5.0f;

        float pingWidth = getAxiomStatWidth("PING", pingAnimatedText, statLabelSize, statValueSize);
        float fpsWidth = getAxiomStatWidth("FPS", fpsAnimatedText, statLabelSize, statValueSize);
        float speedWidth = getAxiomStatWidth("BPS", speedAnimatedText, statLabelSize, statValueSize);
        float timeWidth = getAxiomStatWidth("TIME", timeAnimatedText, statLabelSize, statValueSize);

        float pingX = x + 5.0f;
        float separatorThreeX = pingX + pingWidth + 5.0f;
        float fpsX = separatorThreeX + 6.0f;
        float separatorFourX = fpsX + fpsWidth + 5.0f;
        float speedX = separatorFourX + 6.0f;
        float separatorFiveX = speedX + speedWidth + 5.0f;
        float timeX = separatorFiveX + 6.0f;
        float rowTwoContentWidth = timeX - x + timeWidth + 5.0f;

        float width = Math.max(rowOneContentWidth, rowTwoContentWidth);
        float secondRowY = y + rowHeight + rowGap;

        owner.drawBackground(x, y, width, rowHeight, 5.0f, 235);
        owner.drawBackground(x, secondRowY, width, rowHeight, 5.0f, 225);

        owner.drawAxiomAccent(x + 4.0f, y + 3.0f, badgeWidth, rowHeight - 6.0f, 3.5f, 255);
        DrawUtil.drawText(Fonts.RUBIK.get(), "AX", x + 6.05f, y + 3.55f, primaryText, 7.4f);
        DrawUtil.drawText(Fonts.RUBIK.get(), titleText, titleX, y + 3.65f, primaryText, titleSize);
        DrawUtil.drawRound(separatorOneX, y + 4.0f, 0.75f, rowHeight - 8.0f, 0.35f, dividerColor);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), userText, userX, y + 4.1f, primaryText, userSize);
        DrawUtil.drawRound(separatorTwoX, y + 4.0f, 0.75f, rowHeight - 8.0f, 0.35f, dividerColor);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), serverText, serverX, y + 4.05f, secondaryText, serverSize);

        float styleTagSize = 5.95f;
        float styleTagWidth = Fonts.RUBIK.get().getWidth(styleTag, styleTagSize);
        DrawUtil.drawText(Fonts.RUBIK.get(), styleTag, x + width - styleTagWidth - 5.0f, y + 4.5f, secondaryText, styleTagSize);

        drawAxiomAnimatedStat(context, "PING", pingAnimatedText, pingX, secondRowY, rowHeight, statLabelSize, statValueSize, primaryText, secondaryText);
        DrawUtil.drawRound(separatorThreeX, secondRowY + 4.0f, 0.75f, rowHeight - 8.0f, 0.35f, dividerColor);
        drawAxiomAnimatedStat(context, "FPS", fpsAnimatedText, fpsX, secondRowY, rowHeight, statLabelSize, statValueSize, primaryText, secondaryText);
        DrawUtil.drawRound(separatorFourX, secondRowY + 4.0f, 0.75f, rowHeight - 8.0f, 0.35f, dividerColor);
        drawAxiomAnimatedStat(context, "BPS", speedAnimatedText, speedX, secondRowY, rowHeight, statLabelSize, statValueSize, primaryText, secondaryText);
        DrawUtil.drawRound(separatorFiveX, secondRowY + 4.0f, 0.75f, rowHeight - 8.0f, 0.35f, dividerColor);
        drawAxiomAnimatedStat(context, "TIME", timeAnimatedText, timeX, secondRowY, rowHeight, statLabelSize, statValueSize, primaryText, secondaryText);

        drag.setWidth(width);
        drag.setHeight((rowHeight * 2.0f) + rowGap);
    }

    private void renderMoonward(DrawContext context) {
        Counter.updateFPS();
        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();
        float titleSize = 12.25f;
        String titleText = "";
        String userText = Onetap.getInstance().getAuthManager().getDiscordUsername();
        String serverText = owner.mc.getCurrentServerEntry() != null && owner.mc.getCurrentServerEntry().address != null
                ? owner.mc.getCurrentServerEntry().address
                : "Singleplayer";
        String pingText = Server.getPing(owner.mc.player) + " ms";
        double deltaX = owner.mc.player.getX() - owner.mc.player.prevX;
        double deltaZ = owner.mc.player.getZ() - owner.mc.player.prevZ;
        String speedText = String.format("%.1f b/s", Math.hypot(deltaX, deltaZ) * 20.0);
        String fpsText = Counter.getCurrentFPS() + " fps";
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        pingAnimatedText.update(pingText);
        fpsAnimatedText.update(fpsText);
        timeAnimatedText.update(timeText);
        float userIconSize = 10.5f;
        float serverIconSize = 10.5f;
        float textY = y + 4f;
        int themeColor = ColorProvider.getThemeColor();
        int textColor = ColorProvider.rgba(200, 200, 200, 255);

        float titleTextX = x + 2.5f + 16f + 5f;
        float titleTextWidth = Fonts.SFBOLD.get().getWidth(titleText, titleSize);
        float separatorX = titleTextX + titleTextWidth + 7f;
        float afterSeparatorX = separatorX + 5.5f;
        float userTextX = afterSeparatorX + userIconSize + 0.5f;
        float userTextWidth = Fonts.SFBOLD.get().getWidth(userText, 8.6f);
        float separatorTwoX = userTextX + userTextWidth + 6f;
        float serverIconX = separatorTwoX + 5.5f;
        float serverTextX = serverIconX + serverIconSize + 0.5f;
        float firstRowWidth = serverTextX - x + Fonts.SFBOLD.get().getWidth(serverText, 8.6f) + 5f;
        float height = 20f;
        float gap = 3f;

        float infoIconSize = 10.5f;
        float infoTextSize = 8.6f;
        float secondRowY = y + height + gap;
        float pingIconX = x + 5f;
        float pingTextX = pingIconX + infoIconSize + 0.5f;
        float pingTextWidth = pingAnimatedText.getMaxWidth(infoTextSize);
        float pingSeparatorX = pingTextX + pingTextWidth + 6f;
        float fpsIconX = pingSeparatorX + 5.5f;
        float fpsTextX = fpsIconX + infoIconSize + 0.5f;
        float fpsTextWidth = fpsAnimatedText.getMaxWidth(infoTextSize);
        float fpsSeparatorX = fpsTextX + fpsTextWidth + 6f;
        float timeIconX = fpsSeparatorX + 5.5f;
        float clockTextX = timeIconX + infoIconSize + 0.5f;
        float timeTextWidth = timeAnimatedText.getMaxWidth(infoTextSize);
        float timeSeparatorX = clockTextX + timeTextWidth + 6f;
        float speedIconX = timeSeparatorX + 5.5f;
        float speedTextX = speedIconX + infoIconSize + 0.5f;
        float speedTextWidth = Fonts.SFBOLD.get().getWidth(speedText, infoTextSize);
        float secondRowWidth = speedTextX - x + speedTextWidth + 5f;
        float width = Math.max(firstRowWidth, secondRowWidth);

        DrawUtil.drawRound(x, y, firstRowWidth, height, 3f, 2f, ColorProvider.rgba(13, 16, 23, 255));
        drawLogoRing(x + 2.5f, y + 2f, 16f, themeColor);
        DrawUtil.drawText(Fonts.SFBOLD.get(), titleText, titleTextX, textY, textColor, titleSize);
        DrawUtil.drawRound(separatorX, y + 4.5f, 1f, height - 9f, 0.2f, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue904", afterSeparatorX, y + 5, themeColor, userIconSize);
        DrawUtil.drawText(Fonts.SFBOLD.get(), userText, userTextX, y + 5f, textColor, 8.6f);
        DrawUtil.drawRound(separatorTwoX, y + 4.5f, 1f, height - 9f, 0.2f, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue905", serverIconX, y + 5, themeColor, serverIconSize);
        DrawUtil.drawText(Fonts.SFBOLD.get(), serverText, serverTextX, y + 5, textColor, 8.6f);

        DrawUtil.drawRound(x, secondRowY, secondRowWidth, height, 3f, 2f, ColorProvider.rgba(13, 16, 23, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue906", pingIconX, secondRowY + 5, themeColor, infoIconSize);
        pingAnimatedText.render(context, pingTextX, secondRowY + 5, infoTextSize, textColor, secondRowY + 4.2f, height - 8.4f);
        DrawUtil.drawRound(pingSeparatorX, secondRowY + 4.5f, 1f, height - 9f, 0.2f, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue907", fpsIconX, secondRowY + 5, themeColor, infoIconSize);
        fpsAnimatedText.render(context, fpsTextX, secondRowY + 5, infoTextSize, textColor, secondRowY + 4.2f, height - 8.4f);
        DrawUtil.drawRound(fpsSeparatorX, secondRowY + 4.5f, 1f, height - 9f, 0.2f, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue911", timeIconX, secondRowY + 5, themeColor, infoIconSize);
        timeAnimatedText.render(context, clockTextX, secondRowY + 5, infoTextSize, textColor, secondRowY + 4.2f, height - 8.4f);
        DrawUtil.drawRound(timeSeparatorX, secondRowY + 4.5f, 1f, height - 9f, 0.2f, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue910", speedIconX, secondRowY + 5, themeColor, infoIconSize);
        DrawUtil.drawText(Fonts.SFBOLD.get(), speedText, speedTextX, secondRowY + 5, textColor, infoTextSize);

        drag.setWidth(width);
        drag.setHeight((height * 2f) + gap);
    }

    private void renderCelestial(DrawContext context) {
        Counter.updateFPS();
        String userText = Onetap.getInstance().getAuthManager().getDiscordUsername();
        String uid = "UID: 1";
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        String fullText = userText + " » " + uid + " » " + timeText;
        float fontSize = 7.5f;

        Draggable drag = owner.getWatermarkDrag();
        float x = drag.getX();
        float y = drag.getY();
        float height = 14f;
        float width = Fonts.CELESTIAL.get().getWidth(fullText, fontSize) + 12f;

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 300.0, 110);
        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        drawCelestialGlow(m, x, y, width, height, 4f, 1.0f);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, width + 1f, height + 1f, 4f, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, width, height, 4f, ColorProvider.rgba(14, 10, 6, 255));

        float textY = y + (height - fontSize) / 2f;
        DrawUtil.drawText(Fonts.CELESTIAL.get(), fullText, x + 3f, textY, ColorProvider.rgba(255, 255, 255, 255), fontSize);

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderNursultan(DrawContext context) {
        Counter.updateFPS();

        String userText = Onetap.getInstance().getAuthManager().getDiscordUsername();
        String fpsValue = Counter.getCurrentFPS() + " Fps";
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String coordsText = (int) owner.mc.player.getX() + " " + (int) owner.mc.player.getY() + " " + (int) owner.mc.player.getZ();
        String pingText = Server.getPing(owner.mc.player) + " Ping";
        String tpsText = String.format("%.1f Ticks", Onetap.getInstance().getTpsGetter().getTPS());
        double dX = owner.mc.player.getX() - owner.mc.player.prevX;
        double dZ = owner.mc.player.getZ() - owner.mc.player.prevZ;
        String speedText = String.format("%.1f Bps", Math.hypot(dX, dZ) * 20);

        Draggable drag = owner.getWatermarkDrag();
        float startX = drag.getX();
        float startY = drag.getY();

        int sepColor = ColorProvider.rgba(255, 255, 255, 100);
        int themeColor = ColorProvider.getThemeColor();
        int themeColorTwo = ColorProvider.getThemeColorTwo();
        int whiteColor = -1;
        long time = System.currentTimeMillis();
        int backgroundAlpha = getNursultanBackgroundAlpha();

        boolean showLogo = owner.getNursultanShowLogoSetting().getValue();
        boolean compactLogoMode = owner.getNursultanWatermarkModeSetting().is("Компактное");

        float totalWidth = 0.0f;
        float totalHeight = 0.0f;

        List<NursultanStat> rowOneStats = buildNursultanRowOneStats(userText, fpsValue, timeText);
        List<NursultanStat> rowTwoStats = buildNursultanRowTwoStats(coordsText, pingText, tpsText, speedText);

        float rowOneEndX = startX;
        if (showLogo) {
            float logoWidth = compactLogoMode
                    ? drawNursultanMiniLogoBox(startX, startY, backgroundAlpha, themeColor, time)
                    : drawNursultanLogoBox(startX, startY, backgroundAlpha, sepColor, themeColor, themeColorTwo, time);
            rowOneEndX = startX + logoWidth;
        }

        if (!rowOneStats.isEmpty()) {
            float statsX = rowOneEndX > startX ? rowOneEndX + NURSULTAN_GAP : startX;
            float statsWidth = drawNursultanStatsBox(statsX, startY, rowOneStats, backgroundAlpha, sepColor, themeColor, whiteColor);
            rowOneEndX = statsX + statsWidth;
        }

        float rowOneWidth = Math.max(0.0f, rowOneEndX - startX);
        boolean rowOneRendered = rowOneWidth > 0.0f;

        float rowTwoY = rowOneRendered ? startY + NURSULTAN_HEIGHT + NURSULTAN_GAP : startY;
        float rowTwoEndX = startX;

        if (!rowTwoStats.isEmpty()) {
            if (showLogo) {
                float miniLogoWidth = drawNursultanMiniLogoBox(startX, rowTwoY, backgroundAlpha, themeColor, time);
                rowTwoEndX = startX + miniLogoWidth;
            }

            float statsX = rowTwoEndX > startX ? rowTwoEndX + NURSULTAN_GAP : startX;
            float statsWidth = drawNursultanSeparateStatsBoxes(statsX, rowTwoY, rowTwoStats, backgroundAlpha, themeColor, whiteColor);
            rowTwoEndX = statsX + statsWidth;
        }

        float rowTwoWidth = Math.max(0.0f, rowTwoEndX - startX);
        boolean rowTwoRendered = rowTwoWidth > 0.0f;

        totalWidth = Math.max(rowOneWidth, rowTwoWidth);
        if (rowOneRendered && rowTwoRendered) {
            totalHeight = NURSULTAN_HEIGHT * 2.0f + NURSULTAN_GAP;
        } else if (rowOneRendered || rowTwoRendered) {
            totalHeight = NURSULTAN_HEIGHT;
        }

        drag.setWidth(totalWidth);
        drag.setHeight(totalHeight);

        renderNursultanMenu(context);
    }

    private float getAxiomStatWidth(String label, AnimatedStatText value, float labelSize, float valueSize) {
        return Fonts.RUBIK.get().getWidth(label, labelSize) + 3.0f + value.getMaxWidth(valueSize);
    }

    private void drawAxiomAnimatedStat(
            DrawContext context,
            String label,
            AnimatedStatText value,
            float x,
            float y,
            float height,
            float labelSize,
            float valueSize,
            int primaryColor,
            int secondaryColor
    ) {
        float labelWidth = Fonts.RUBIK.get().getWidth(label, labelSize);
        DrawUtil.drawText(Fonts.RUBIK.get(), label, x, y + 4.55f, secondaryColor, labelSize);
        value.render(context, x + labelWidth + 3.0f, y + 4.1f, valueSize, primaryColor, y + 3.3f, height - 6.6f);
    }

    private String trimAxiomText(String text, int maxLength) {
        if (text == null || text.isEmpty() || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(1, maxLength - 3)) + "...";
    }

    public boolean handleChatClick(double mouseX, double mouseY, int button) {
        if (!owner.getHudStyleSetting().is("Nursultan")) {
            nursultanMenuOpen = false;
            nursultanMenuAnchorX = Float.NaN;
            nursultanMenuAnchorY = Float.NaN;
            return false;
        }

        if (!(owner.mc.currentScreen instanceof ChatScreen)) {
            nursultanMenuOpen = false;
            nursultanMenuAnchorX = Float.NaN;
            nursultanMenuAnchorY = Float.NaN;
            return false;
        }

        Draggable drag = owner.getWatermarkDrag();
        if (button == 1 && HoverUtil.isHovered(mouseX, mouseY, drag.getX(), drag.getY(), drag.getWidth(), drag.getHeight())) {
            nursultanMenuOpen = !nursultanMenuOpen;
            if (nursultanMenuOpen) {
                nursultanMenuAnchorX = (float) mouseX;
                nursultanMenuAnchorY = (float) mouseY;
                syncToggleProgressWithSettings();
                updateNursultanMenuBounds();
            } else {
                nursultanMenuAnchorX = Float.NaN;
                nursultanMenuAnchorY = Float.NaN;
            }
            return true;
        }

        if (!nursultanMenuOpen) {
            return false;
        }

        updateNursultanMenuBounds();
        if (button == 1) {
            return HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX, nursultanMenuY, nursultanMenuWidth, nursultanMenuHeight);
        }

        if (button != 0 || !HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX, nursultanMenuY, nursultanMenuWidth, nursultanMenuHeight)) {
            return false;
        }

        BooleanSetting[] toggleSettings = getNursultanMenuToggleSettings();
        float rowY = nursultanMenuY + NURSULTAN_MENU_PADDING;
        float toggleX = nursultanMenuX + nursultanMenuWidth - NURSULTAN_MENU_PADDING - NURSULTAN_MENU_TOGGLE_WIDTH;
        for (BooleanSetting setting : toggleSettings) {
            float toggleY = rowY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
            if (HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX + NURSULTAN_MENU_PADDING, rowY, nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f), NURSULTAN_MENU_ROW_HEIGHT)
                    || HoverUtil.isHovered(mouseX, mouseY, toggleX, toggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT)) {
                setting.toggle();
                return true;
            }
            rowY += NURSULTAN_MENU_ROW_HEIGHT;
        }

        float modeY = rowY + 4.5f;
        float modeHeight = 13.0f;
        float modeButtonWidth = (nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f) - 4.0f) / 2.0f;
        float fullX = nursultanMenuX + NURSULTAN_MENU_PADDING;
        float compactX = fullX + modeButtonWidth + 4.0f;
        if (HoverUtil.isHovered(mouseX, mouseY, fullX, modeY, modeButtonWidth, modeHeight)) {
            owner.getNursultanWatermarkModeSetting().setValue("Полное");
            return true;
        }
        if (HoverUtil.isHovered(mouseX, mouseY, compactX, modeY, modeButtonWidth, modeHeight)) {
            owner.getNursultanWatermarkModeSetting().setValue("Компактное");
            return true;
        }

        float transparentY = modeY + modeHeight + 3.5f;
        float transparentToggleY = transparentY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX + NURSULTAN_MENU_PADDING, transparentY, nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f), NURSULTAN_MENU_ROW_HEIGHT)
                || HoverUtil.isHovered(mouseX, mouseY, toggleX, transparentToggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT)) {
            owner.getNursultanTransparentBackgroundSetting().toggle();
            return true;
        }

        BooleanSetting watermarkBlur = owner.getBlurBackgroundOption("Ватермарка");
        float watermarkBlurY = transparentY + NURSULTAN_MENU_ROW_HEIGHT;
        float watermarkBlurToggleY = watermarkBlurY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (watermarkBlur != null && (
                HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX + NURSULTAN_MENU_PADDING, watermarkBlurY, nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f), NURSULTAN_MENU_ROW_HEIGHT)
                        || HoverUtil.isHovered(mouseX, mouseY, toggleX, watermarkBlurToggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT))) {
            watermarkBlur.toggle();
            return true;
        }

        BooleanSetting watermarkTheme = owner.getThemeBackgroundOption("Ватермарка");
        float watermarkThemeY = watermarkBlurY + NURSULTAN_MENU_ROW_HEIGHT;
        float watermarkThemeToggleY = watermarkThemeY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (watermarkTheme != null && (
                HoverUtil.isHovered(mouseX, mouseY, nursultanMenuX + NURSULTAN_MENU_PADDING, watermarkThemeY, nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f), NURSULTAN_MENU_ROW_HEIGHT)
                        || HoverUtil.isHovered(mouseX, mouseY, toggleX, watermarkThemeToggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT))) {
            watermarkTheme.toggle();
            return true;
        }

        return true;
    }

    public void onChatClosed() {
        nursultanMenuOpen = false;
        nursultanMenuAnchorX = Float.NaN;
        nursultanMenuAnchorY = Float.NaN;
    }

    private float drawNursultanLogoBox(float x, float y, int backgroundAlpha, int sepColor, int themeColor, int themeColorTwo, long time) {
        String alphaStr = "";
        float alphaW = Fonts.SFMEDIUM.get().getWidth(alphaStr, 7f);
        float firstBoxWidth = 17f + 10f + alphaW;
        owner.drawBackground(x, y, firstBoxWidth, NURSULTAN_HEIGHT, 4, backgroundAlpha);

        float pulse = (float) (Math.sin(time / 200.0) * 0.3 + 0.7);
        int animatedThemeColor = ColorProvider.setAlpha(themeColor, (int) (255 * pulse));
        DrawUtil.drawText(Fonts.MOONWARD.get(), "\ue900", x + 2.5f, y + 0.5f, animatedThemeColor, 14f);

        return firstBoxWidth;
    }

    private float drawNursultanMiniLogoBox(float x, float y, int backgroundAlpha, int themeColor, long time) {
        float width = 17f;
        float pulse = (float) (Math.sin((time + 150) / 250.0) * 0.3 + 0.7);
        int animatedThemeColor = ColorProvider.setAlpha(themeColor, (int) (255 * pulse));
        owner.drawBackground(x, y, width, NURSULTAN_HEIGHT, 4, backgroundAlpha);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "\u0055", x + 4.5f, y + 4.25f, animatedThemeColor, 8f);
        return width;
    }

    private void drawLogoRing(float x, float y, float size, int color) {
        Builder.border()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(size / 2f, size / 2f, size / 2f, size / 2f))
                .color(new QuadColorState(color))
                .thickness(2.2f)
                .smoothness(1f, 1f)
                .build()
                .render(x, y);
    }

    private float drawNursultanStatsBox(float x, float y, List<NursultanStat> stats, int backgroundAlpha, int sepColor, int themeColor, int textColor) {
        if (stats.isEmpty()) {
            return 0.0f;
        }

        float width = 4.0f;
        for (int i = 0; i < stats.size(); i++) {
            NursultanStat stat = stats.get(i);
            width += 10.0f + Fonts.SFMEDIUM.get().getWidth(stat.text, 7f);
            if (i < stats.size() - 1) {
                width += 11.0f;
            } else {
                width += 6.0f;
            }
        }

        owner.drawBackground(x, y, width, NURSULTAN_HEIGHT, 4, backgroundAlpha);

        float currentX = x + 4.0f;
        for (int i = 0; i < stats.size(); i++) {
            NursultanStat stat = stats.get(i);
            float textWidth = Fonts.SFMEDIUM.get().getWidth(stat.text, 7f);
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), stat.icon, currentX, y + 4.25f, themeColor, 8f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), stat.text, currentX + 10.0f, y + 3.5f, textColor, 7f);
            if (i < stats.size() - 1) {
                currentX += 11.0f + textWidth + 5.0f;
                DrawUtil.drawRound(currentX, y + 3.5f, 0.5f, NURSULTAN_HEIGHT - 7f, 0.2f, sepColor);
                currentX += 6.0f;
            }
        }

        return width;
    }

    private float drawNursultanSeparateStatsBoxes(float x, float y, List<NursultanStat> stats, int backgroundAlpha, int themeColor, int textColor) {
        if (stats.isEmpty()) {
            return 0.0f;
        }

        float currentX = x;
        for (NursultanStat stat : stats) {
            boolean isSpeed = "\u0040".equals(stat.icon);
            float textWidth = Fonts.SFMEDIUM.get().getWidth(stat.text, 7f);
            float boxWidth = (isSpeed ? 24.0f : 21.0f) + textWidth;
            float separatorX = currentX + (isSpeed ? 15.0f : ("\u0051".equals(stat.icon) ? 13.5f : 13.0f));
            float textX = currentX + (isSpeed ? 20.0f : 17.0f);

            owner.drawBackground(currentX, y, boxWidth, NURSULTAN_HEIGHT, 4, backgroundAlpha);
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), stat.icon, currentX + 4.0f, y + 4.25f, themeColor, 8f);
            DrawUtil.drawRound(separatorX, y + 3.5f, 0.5f, NURSULTAN_HEIGHT - 7f, 0.2f, ColorProvider.rgba(255, 255, 255, 100));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), stat.text, textX, y + 3.5f, textColor, 7f);
            currentX += boxWidth + NURSULTAN_GAP;
        }

        return Math.max(0.0f, currentX - x - NURSULTAN_GAP);
    }

    private List<NursultanStat> buildNursultanRowOneStats(String userText, String fpsValue, String timeText) {
        List<NursultanStat> stats = new ArrayList<>();
        if (owner.getNursultanShowLoginSetting().getValue()) {
            stats.add(new NursultanStat("\u0057", userText));
        }
        if (owner.getNursultanShowFpsSetting().getValue()) {
            stats.add(new NursultanStat("\u0058", fpsValue));
        }
        if (owner.getNursultanShowTimeSetting().getValue()) {
            stats.add(new NursultanStat("\u0056", timeText));
        }
        return stats;
    }

    private List<NursultanStat> buildNursultanRowTwoStats(String coordsText, String pingText, String tpsText, String speedText) {
        List<NursultanStat> stats = new ArrayList<>();
        if (owner.getNursultanShowCoordsSetting().getValue()) {
            stats.add(new NursultanStat("\u0046", coordsText));
        }
        if (owner.getNursultanShowPingSetting().getValue()) {
            stats.add(new NursultanStat("\u0051", pingText));
        }
        if (owner.getNursultanShowTpsSetting().getValue()) {
            stats.add(new NursultanStat("\u0054", tpsText));
        }
        if (owner.getNursultanShowBpsSetting().getValue()) {
            stats.add(new NursultanStat("\u0040", speedText));
        }
        return stats;
    }

    private List<NursultanStat> buildNursultanCompactStats(
            String userText,
            String fpsValue,
            String timeText,
            String coordsText,
            String pingText,
            String tpsText,
            String speedText
    ) {
        List<NursultanStat> stats = new ArrayList<>();
        stats.addAll(buildNursultanRowOneStats(userText, fpsValue, timeText));
        stats.addAll(buildNursultanRowTwoStats(coordsText, pingText, tpsText, speedText));
        return stats;
    }

    private int getNursultanBackgroundAlpha() {
        return owner.getNursultanTransparentBackgroundSetting().getValue() ? 145 : 255;
    }

    private BooleanSetting[] getNursultanMenuToggleSettings() {
        return new BooleanSetting[]{
                owner.getNursultanShowLoginSetting(),
                owner.getNursultanShowFpsSetting(),
                owner.getNursultanShowTimeSetting(),
                owner.getNursultanShowCoordsSetting(),
                owner.getNursultanShowPingSetting(),
                owner.getNursultanShowTpsSetting(),
                owner.getNursultanShowBpsSetting(),
                owner.getNursultanShowLogoSetting()
        };
    }

    private String[] getNursultanMenuToggleLabels() {
        return new String[]{
                "Логин",
                "ФПС",
                "Время",
                "Координаты",
                "Пинг",
                "ТПС",
                "БПС",
                "Лого"
        };
    }

    private void updateNursultanMenuBounds() {
        Draggable drag = owner.getWatermarkDrag();
        nursultanMenuWidth = NURSULTAN_MENU_WIDTH;
        nursultanMenuHeight = NURSULTAN_MENU_PADDING
                + (NURSULTAN_MENU_TOGGLE_COUNT * NURSULTAN_MENU_ROW_HEIGHT)
                + 4.5f
                + 13.0f
                + 3.5f
                + (NURSULTAN_MENU_ROW_HEIGHT * 3.0f)
                + NURSULTAN_MENU_PADDING;

        float baseX = Float.isNaN(nursultanMenuAnchorX) ? drag.getX() : nursultanMenuAnchorX + 4.0f;
        float baseY = Float.isNaN(nursultanMenuAnchorY) ? (drag.getY() + drag.getHeight() + 5.0f) : nursultanMenuAnchorY + 4.0f;

        float screenWidth = owner.mc.getWindow().getScaledWidth();
        float screenHeight = owner.mc.getWindow().getScaledHeight();
        nursultanMenuX = Math.max(2.0f, Math.min(baseX, screenWidth - nursultanMenuWidth - 2.0f));
        nursultanMenuY = Math.max(2.0f, Math.min(baseY, screenHeight - nursultanMenuHeight - 2.0f));
    }

    private void renderNursultanMenu(DrawContext context) {
        if (!nursultanMenuOpen) {
            return;
        }

        if (!(owner.mc.currentScreen instanceof ChatScreen) || !owner.getHudStyleSetting().is("Nursultan")) {
            nursultanMenuOpen = false;
            nursultanMenuAnchorX = Float.NaN;
            nursultanMenuAnchorY = Float.NaN;
            return;
        }

        updateNursultanMenuBounds();

        int textColor = ColorProvider.rgba(225, 225, 225, 255);
        int rowTextColorOn = ColorProvider.rgba(210, 210, 210, 255);
        int rowTextColorOff = ColorProvider.rgba(210, 210, 210, 195);
        int themeColor = ColorProvider.getThemeColor();

        int menuBackgroundAlpha = 210;
        owner.drawBackground(nursultanMenuX, nursultanMenuY, nursultanMenuWidth, nursultanMenuHeight, 4.0f, menuBackgroundAlpha);

        BooleanSetting[] toggleSettings = getNursultanMenuToggleSettings();
        String[] toggleLabels = getNursultanMenuToggleLabels();
        float rowY = nursultanMenuY + NURSULTAN_MENU_PADDING;
        float toggleX = nursultanMenuX + nursultanMenuWidth - NURSULTAN_MENU_PADDING - NURSULTAN_MENU_TOGGLE_WIDTH;
        for (int i = 0; i < toggleSettings.length; i++) {
            int rowTextColor = toggleSettings[i].getValue() ? rowTextColorOn : rowTextColorOff;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), toggleLabels[i], nursultanMenuX + NURSULTAN_MENU_PADDING, rowY + 2.6f, rowTextColor, 6.7f);
            float progress = animateToggleProgress(i, toggleSettings[i].getValue());
            float toggleY = rowY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, toggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT, progress, themeColor);
            rowY += NURSULTAN_MENU_ROW_HEIGHT;
        }

        float modeY = rowY + 4.5f;
        float modeHeight = 13.0f;
        float modeButtonWidth = (nursultanMenuWidth - (NURSULTAN_MENU_PADDING * 2.0f) - 4.0f) / 2.0f;
        float fullX = nursultanMenuX + NURSULTAN_MENU_PADDING;
        float compactX = fullX + modeButtonWidth + 4.0f;
        boolean fullActive = owner.getNursultanWatermarkModeSetting().is("Полное");
        boolean compactActive = owner.getNursultanWatermarkModeSetting().is("Компактное");

        int activeButtonColor = ColorProvider.setAlpha(themeColor, 95);
        owner.drawBackground(fullX, modeY, modeButtonWidth, modeHeight, 4.0f, 190);
        owner.drawBackground(compactX, modeY, modeButtonWidth, modeHeight, 4.0f, 190);
        nursultanModeProgress = animateModeProgress(nursultanModeProgress, compactActive ? 1.0f : 0.0f);
        float modeHighlightX = fullX + (compactX - fullX) * nursultanModeProgress;
        DrawUtil.drawRound(modeHighlightX, modeY, modeButtonWidth, modeHeight, 4.0f, activeButtonColor);

        String fullText = "Полное";
        String compactText = "Компакт";
        float fullTextX = fullX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(fullText, 6.7f)) / 2.0f;
        float compactTextX = compactX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(compactText, 6.7f)) / 2.0f;
        float modeTextY = modeY + (modeHeight - 6.7f) / 2.0f - 0.3f;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullText, fullTextX, modeTextY, textColor, 6.7f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), compactText, compactTextX, modeTextY, textColor, 6.7f);

        float transparentY = modeY + modeHeight + 3.5f;
        int transparentTextColor = owner.getNursultanTransparentBackgroundSetting().getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Прозрачный фон", nursultanMenuX + NURSULTAN_MENU_PADDING, transparentY + 2.6f, transparentTextColor, 6.7f);
        float transparentProgress = animateToggleProgress(
                NURSULTAN_TRANSPARENT_TOGGLE_INDEX,
                owner.getNursultanTransparentBackgroundSetting().getValue()
        );
        float transparentToggleY = transparentY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
        drawMenuToggle(
                toggleX,
                transparentToggleY,
                NURSULTAN_MENU_TOGGLE_WIDTH,
                NURSULTAN_MENU_TOGGLE_HEIGHT,
                transparentProgress,
                themeColor
        );

        BooleanSetting watermarkBlur = owner.getBlurBackgroundOption("Ватермарка");
        float watermarkBlurY = transparentY + NURSULTAN_MENU_ROW_HEIGHT;
        int watermarkBlurTextColor = watermarkBlur != null && watermarkBlur.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Блюр фона", nursultanMenuX + NURSULTAN_MENU_PADDING, watermarkBlurY + 2.6f, watermarkBlurTextColor, 6.7f);
        if (watermarkBlur != null) {
            float watermarkBlurProgress = animateToggleProgress(NURSULTAN_WATERMARK_BLUR_INDEX, watermarkBlur.getValue());
            float watermarkBlurToggleY = watermarkBlurY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, watermarkBlurToggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT, watermarkBlurProgress, themeColor);
        }

        BooleanSetting watermarkTheme = owner.getThemeBackgroundOption("Ватермарка");
        float watermarkThemeY = watermarkBlurY + NURSULTAN_MENU_ROW_HEIGHT;
        int watermarkThemeTextColor = watermarkTheme != null && watermarkTheme.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Фон от темы", nursultanMenuX + NURSULTAN_MENU_PADDING, watermarkThemeY + 2.6f, watermarkThemeTextColor, 6.7f);
        if (watermarkTheme != null) {
            float watermarkThemeProgress = animateToggleProgress(NURSULTAN_WATERMARK_THEME_INDEX, watermarkTheme.getValue());
            float watermarkThemeToggleY = watermarkThemeY + (NURSULTAN_MENU_ROW_HEIGHT - NURSULTAN_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, watermarkThemeToggleY, NURSULTAN_MENU_TOGGLE_WIDTH, NURSULTAN_MENU_TOGGLE_HEIGHT, watermarkThemeProgress, themeColor);
        }
    }

    private void drawMenuToggle(float x, float y, float width, float height, float progress, int themeColor) {
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int offColor = ColorProvider.rgba(58, 62, 78, 210);
        int onColor = ColorProvider.setAlpha(themeColor, 165);
        int bgColor = ColorProvider.interpolateColor(offColor, onColor, clampedProgress);
        DrawUtil.drawRound(x, y, width, height, NURSULTAN_MENU_TOGGLE_RADIUS, bgColor);

        float knobSize = Math.max(
                1.0f,
                height - (NURSULTAN_MENU_TOGGLE_KNOB_PADDING * 2.0f) - NURSULTAN_MENU_TOGGLE_KNOB_REDUCTION
        );
        float minKnobX = x + NURSULTAN_MENU_TOGGLE_KNOB_PADDING;
        float maxKnobX = x + width - knobSize - NURSULTAN_MENU_TOGGLE_KNOB_PADDING;
        float knobX = minKnobX + (maxKnobX - minKnobX) * clampedProgress;
        float knobY = y + (height - knobSize) / 2.0f;
        int knobOffColor = ColorProvider.rgba(206, 206, 206, 255);
        int knobOnColor = ColorProvider.rgba(232, 232, 232, 255);
        int knobColor = ColorProvider.interpolateColor(knobOffColor, knobOnColor, clampedProgress);
        DrawUtil.drawRound(knobX, knobY, knobSize, knobSize, knobSize / 2.5f, knobColor);
    }

    private float animateToggleProgress(int index, boolean enabled) {
        if (index < 0 || index >= nursultanToggleProgress.length) {
            return enabled ? 1.0f : 0.0f;
        }
        float target = enabled ? 1.0f : 0.0f;
        float current = nursultanToggleProgress[index];
        current += (target - current) * 0.24f;
        if (Math.abs(target - current) < 0.001f) {
            current = target;
        }
        nursultanToggleProgress[index] = current;
        return current;
    }

    private float animateModeProgress(float currentValue, float targetValue) {
        float nextValue = currentValue + (targetValue - currentValue) * 0.24f;
        if (Math.abs(targetValue - nextValue) < 0.001f) {
            nextValue = targetValue;
        }
        return nextValue;
    }

    private void syncToggleProgressWithSettings() {
        BooleanSetting[] toggleSettings = getNursultanMenuToggleSettings();
        for (int i = 0; i < toggleSettings.length; i++) {
            nursultanToggleProgress[i] = toggleSettings[i].getValue() ? 1.0f : 0.0f;
        }
        nursultanModeProgress = owner.getNursultanWatermarkModeSetting().getIndex() == 1 ? 1.0f : 0.0f;
        nursultanToggleProgress[NURSULTAN_TRANSPARENT_TOGGLE_INDEX] = owner.getNursultanTransparentBackgroundSetting().getValue() ? 1.0f : 0.0f;
        BooleanSetting watermarkBlur = owner.getBlurBackgroundOption("Ватермарка");
        nursultanToggleProgress[NURSULTAN_WATERMARK_BLUR_INDEX] = watermarkBlur != null && watermarkBlur.getValue() ? 1.0f : 0.0f;
        BooleanSetting watermarkTheme = owner.getThemeBackgroundOption("Ватермарка");
        nursultanToggleProgress[NURSULTAN_WATERMARK_THEME_INDEX] = watermarkTheme != null && watermarkTheme.getValue() ? 1.0f : 0.0f;
    }

    private static class NursultanStat {
        private final String icon;
        private final String text;

        private NursultanStat(String icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }

    private int colorLerp(int start, int end, float speed, float offset) {
        long t = System.currentTimeMillis();
        double ph = t * (speed / 1000.0) + offset;
        float p = (float) (Math.sin(ph) * 0.5 + 0.5);

        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;

        int r = (int) (sr * (1f - p) + er * p);
        int g = (int) (sg * (1f - p) + eg * p);
        int b = (int) (sb * (1f - p) + eb * p);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
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

    private static class AnimatedStatText {
        private final Animation animation = new Animation(Easing.EXPO_OUT, 360);
        private final long minUpdateIntervalMs;
        private String currentText = "";
        private String previousText = "";
        private String pendingText = "";
        private long lastSwapTime;

        private AnimatedStatText(long minUpdateIntervalMs) {
            this.minUpdateIntervalMs = minUpdateIntervalMs;
        }

        void update(String text) {
            if (text == null) {
                text = "";
            }

            long now = System.currentTimeMillis();

            if (currentText.isEmpty()) {
                currentText = text;
                previousText = text;
                pendingText = text;
                lastSwapTime = now;
                animation.setValue(1f);
                return;
            }

            pendingText = text;
            if (!currentText.equals(pendingText) && now - lastSwapTime >= minUpdateIntervalMs) {
                previousText = currentText;
                currentText = pendingText;
                lastSwapTime = now;
                animation.setValue(0f);
            }
        }

        float getMaxWidth(float size) {
            return Math.max(
                    Fonts.SFBOLD.get().getWidth(currentText, size),
                    Fonts.SFBOLD.get().getWidth(previousText, size)
            );
        }

        void render(DrawContext context, float x, float y, float size, int color, float clipY, float clipHeight) {
            float progress = animation.run(1f);
            float distance = Math.min(clipHeight * 0.45f, size * 0.7f);
            float currentY = y - (1f - progress) * distance;
            float previousY = y + progress * distance;
            int currentColor = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * progress));
            int previousColor = ColorProvider.setAlpha(color, (int) (ColorProvider.alpha(color) * (1f - progress)));

            Scissor.push();
            Scissor.setFromComponentCoordinates(x - 1f, clipY, getMaxWidth(size) + 2f, clipHeight);
            if (progress < 0.999f) {
                DrawUtil.drawText(Fonts.SFBOLD.get(), previousText, x, previousY, previousColor, size);
            }
            DrawUtil.drawText(Fonts.SFBOLD.get(), currentText, x, currentY, currentColor, size);
            Scissor.unset();
            Scissor.pop();
        }
    }
}
