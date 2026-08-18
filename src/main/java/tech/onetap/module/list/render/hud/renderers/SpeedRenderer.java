package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.renderers.DrawUtil;

public class SpeedRenderer {
    private final Interface owner;

    public SpeedRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        double deltaX = owner.mc.player.getX() - owner.mc.player.prevX;
        double deltaY = owner.mc.player.getY() - owner.mc.player.prevY;
        double deltaZ = owner.mc.player.getZ() - owner.mc.player.prevZ;
        double speedBps = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 20;

        String text = String.format(java.util.Locale.US, "%.2f", speedBps);
        float fontSize = 11f;
        float textWidth = Fonts.SFBOLD.get().getWidth(text, fontSize);

        float x = owner.mc.getWindow().getScaledWidth() / 2f - (textWidth / 2f);
        float y = owner.mc.getWindow().getScaledHeight() / 2f + 8f;

        if (owner.isAxiomStyle()) {
            String valueText = String.format(java.util.Locale.US, "%.2f", speedBps);
            float valueSize = 8.7f;
            float labelSize = 5.8f;
            float width = Math.max(54.0f, Fonts.RUBIK.get().getWidth(valueText, valueSize) + 28.0f);
            float height = 16.5f;
            float panelX = owner.mc.getWindow().getScaledWidth() / 2f - (width / 2f);
            float panelY = owner.mc.getWindow().getScaledHeight() / 2f + 10f;

            owner.drawBackground(panelX, panelY, width, height, 5.0f, 230);
            owner.drawAxiomAccent(panelX + 4.0f, panelY + 3.0f, 13.5f, height - 6.0f, 3.2f, 255);
            DrawUtil.drawText(Fonts.RUBIK.get(), "BPS", panelX + 6.2f, panelY + 4.15f, owner.getAxiomPrimaryTextColor(255), 6.2f);
            DrawUtil.drawText(Fonts.RUBIK.get(), valueText, panelX + 21.0f, panelY + 3.0f, owner.getAxiomPrimaryTextColor(255), valueSize);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "movement", panelX + 21.0f, panelY + 9.55f, owner.getAxiomSecondaryTextColor(255), labelSize);
            return;
        }

        DrawUtil.drawText(Fonts.SFBOLD.get(), text, x, y, -1, fontSize);
    }
}
