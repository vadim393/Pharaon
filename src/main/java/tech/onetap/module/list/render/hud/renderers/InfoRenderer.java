package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
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

import java.util.Locale;

public class InfoRenderer {
    private static final double BPS_SMOOTHING = 0.05;
    private static final double DISPLAY_SMOOTHING = 0.03;

    private final Interface owner;
    private final Animation chatAnimation = new Animation(Easing.EXPO_OUT, 250);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private double lastX = 0;
    private double lastZ = 0;
    private double currentBps = 0;
    private double displayBps = 0;
    private double targetBps = 0;
    private long lastUpdateTime = 0;

    public InfoRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        if (owner.getHudStyleSetting().is("Exp4.0")) {
            renderExp4_0(context);
        } else {
            renderClassic(context);
        }
    }

    private void renderExp4_0(DrawContext context) {
        alpha.run(1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0;
        if (lastUpdateTime > 0 && deltaTime > 0) {
            double dx = owner.mc.player.getX() - lastX;
            double dz = owner.mc.player.getZ() - lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double instantBps = distance / deltaTime;
            currentBps += (instantBps - currentBps) * BPS_SMOOTHING;
            targetBps = roundToStep(currentBps, 0.1);
        }
        displayBps += (targetBps - displayBps) * DISPLAY_SMOOTHING;

        lastX = owner.mc.player.getX();
        lastZ = owner.mc.player.getZ();
        lastUpdateTime = currentTime;

        chatAnimation.run(owner.mc.currentScreen instanceof ChatScreen ? 1f : 0f);
        float chatOffset = (float) chatAnimation.getValue() * 14f;

        int totalHeight = 16 + 18 + 18;
        int screenHeight = owner.mc.getWindow().getScaledHeight();
        Draggable drag = owner.getInfoDrag();
        float x = drag.getX();
        float y = Math.min(drag.getY(), screenHeight - totalHeight - 5f - chatOffset);
        float currentY = y;

        float tps = Onetap.getInstance().getTpsGetter().getTPS();
        String tpsValue = String.format(Locale.US, "%.1f", tps).replace('.', ',');
        float width = drawInfoBox(context, x, currentY, "A", "TPS ", tpsValue, aInt);
        currentY += 18;

        String bpsValue = String.format(Locale.US, "%.1f", displayBps).replace('.', ',');
        float bpsWidth = drawInfoBox(context, x, currentY, "E", "BPS ", bpsValue, aInt);
        width = Math.max(width, bpsWidth);
        currentY += 18;

        int playerX = (int) owner.mc.player.getX();
        int playerY = (int) owner.mc.player.getY();
        int playerZ = (int) owner.mc.player.getZ();
        float coordsWidth = drawCoordsBox(context, x, currentY, "n", playerX, playerY, playerZ, aInt);
        width = Math.max(width, coordsWidth);

        drag.setWidth(width);
        drag.setHeight(totalHeight);
    }

    private float drawInfoBox(DrawContext context, float x, float y, String icon, String label, String value, int alpha) {
        float labelWidth = Fonts.SFBOLD.get().getWidth(label, 7f);
        float valueWidth = Fonts.SFBOLD.get().getWidth(value, 7f);
        float boxWidth = 5f + 10f + 5f + labelWidth + valueWidth + 8f;
        float boxHeight = 16f;

        int bgAlpha = MathHelper.clamp((int) (200 * (alpha / 255f)), 0, 255);
        DrawUtil.drawRound(x, y, boxWidth, boxHeight, 4f, ColorProvider.rgba(15, 15, 15, bgAlpha));
        owner.drawExp4Border(x, y, boxWidth, boxHeight, 4f, bgAlpha);

        float iconBoxSize = 12f;
        float iconBoxX = x + 4f;
        float iconBoxY = y + 2f;
        DrawUtil.drawRound(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 3f,
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha),
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha));
        DrawUtil.drawText(Fonts.ICONS2.get(), icon, x + 5.5f, y + 2.5f, ColorProvider.rgba(130, 140, 255, alpha), 9f);
        DrawUtil.drawText(Fonts.SFBOLD.get(), label, x + 19f, y + 5.5f, ColorProvider.rgba(160, 160, 160, alpha), 7f);
        DrawUtil.drawText(Fonts.SFBOLD.get(), value, x + 19f + labelWidth, y + 5.5f, ColorProvider.rgba(255, 255, 255, alpha), 7f);
        return boxWidth;
    }

    private float drawCoordsBox(DrawContext context, float x, float y, String icon, int px, int py, int pz, int alpha) {
        String xLab = "X ";
        String yLab = " Y ";
        String zLab = " Z ";
        String xVal = String.valueOf(px);
        String yVal = String.valueOf(py);
        String zVal = String.valueOf(pz);

        float xLabW = Fonts.SFBOLD.get().getWidth(xLab, 7f);
        float yLabW = Fonts.SFBOLD.get().getWidth(yLab, 7f);
        float zLabW = Fonts.SFBOLD.get().getWidth(zLab, 7f);
        float xValW = Fonts.SFBOLD.get().getWidth(xVal, 7f);
        float yValW = Fonts.SFBOLD.get().getWidth(yVal, 7f);
        float zValW = Fonts.SFBOLD.get().getWidth(zVal, 7f);

        float boxWidth = 5f + 10f + 5f + xLabW + xValW + yLabW + yValW + zLabW + zValW + 8f;
        float boxHeight = 16f;

        int bgAlpha = MathHelper.clamp((int) (200 * (alpha / 255f)), 0, 255);
        DrawUtil.drawRound(x, y, boxWidth, boxHeight, 4f, ColorProvider.rgba(15, 15, 15, bgAlpha));
        owner.drawExp4Border(x, y, boxWidth, boxHeight, 4f, bgAlpha);

        float iconBoxSize = 12f;
        float iconBoxX = x + 4f;
        float iconBoxY = y + 2f;
        DrawUtil.drawRound(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 3f,
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha),
                ColorProvider.rgba(30, 30, 45, bgAlpha),
                ColorProvider.rgba(25, 25, 40, bgAlpha));
        DrawUtil.drawText(Fonts.ICONS2.get(), icon, x + 5.5f, y + 4f, ColorProvider.rgba(130, 140, 255, alpha), 9f);

        float curX = x + 19f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), xLab, curX, y + 5.5f, ColorProvider.rgba(160, 160, 160, alpha), 7f);
        curX += xLabW;
        DrawUtil.drawText(Fonts.SFBOLD.get(), xVal, curX, y + 5.5f, ColorProvider.rgba(255, 255, 255, alpha), 7f);
        curX += xValW;

        DrawUtil.drawText(Fonts.SFBOLD.get(), yLab, curX, y + 5.5f, ColorProvider.rgba(160, 160, 160, alpha), 7f);
        curX += yLabW;
        DrawUtil.drawText(Fonts.SFBOLD.get(), yVal, curX, y + 5.5f, ColorProvider.rgba(255, 255, 255, alpha), 7f);
        curX += yValW;

        DrawUtil.drawText(Fonts.SFBOLD.get(), zLab, curX, y + 5.5f, ColorProvider.rgba(160, 160, 160, alpha), 7f);
        curX += zLabW;
        DrawUtil.drawText(Fonts.SFBOLD.get(), zVal, curX, y + 5.5f, ColorProvider.rgba(255, 255, 255, alpha), 7f);
        return boxWidth;
    }

    private double roundToStep(double value, double step) {
        return Math.round(value / step) * step;
    }

    private void renderClassic(DrawContext context) {
        Draggable drag = owner.getInfoDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        DrawUtil.drawRound(posX, posY, 14.5f, 14.5f, 3f, ColorProvider.rgba(15, 15, 15, 200));
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Info", posX + 4f, posY + 4f, ColorProvider.rgba(255, 255, 255, 255), 6.5f);
        drag.setWidth(14.5f);
        drag.setHeight(14.5f);
    }
}
