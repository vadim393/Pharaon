package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.ColorSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.awt.*;

public class ColorObject extends Object {
    public ModuleObject object;
    public ColorSetting setting;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;
    private boolean colorSelectorDragging;
    private boolean hueSelectorDragging;
    private boolean alphaSelectorDragging;
    protected boolean extended;

    private static final float GRADIENT_X = 10.0F;
    private static final float GRADIENT_Y = 24.0F;
    private static final float GRADIENT_W = 92.0F;
    private static final float GRADIENT_H = 56.0F;
    private static final float HUE_Y = 88.0F;
    private static final float ALPHA_Y = 100.0F;
    private static final float BAR_W = 92.0F;
    private static final float BAR_H = 6.0F;

    public ColorObject(ModuleObject object, ColorSetting setting) {
        this.height = 19.0F;
        this.object = object;
        this.setting = setting;
        float[] hsb = this.RGBtoHSB(setting.getValue());
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = (setting.getValue() >> 24 & 255) / 255.0F;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        this.height = extended ? 112.0F : 19.0F;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), setting.getName(), this.x + 10.0F, this.y + (19.0F - ClickGuiUtil.NC12) / 2.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC12);

        float swatchX = this.x + this.width - 22.0F;
        float swatchY = this.y + 4.0F;
        DrawUtil.drawRound(swatchX, swatchY, 12.0F, 11.0F, 3.0F, setting.getValue());
        ClickGuiUtil.drawRoundOutline(swatchX, swatchY, 12.0F, 11.0F, 3.0F, 0,
                0x70FFFFFF, 0x70FFFFFF, 0x70FFFFFF, 0x70FFFFFF);

        if (!extended) return;

        if (colorSelectorDragging && HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + GRADIENT_Y, GRADIENT_W, GRADIENT_H)) {
            this.saturation = (mouseX - (this.x + GRADIENT_X)) / GRADIENT_W;
            this.brightness = 1.0F - (mouseY - (this.y + GRADIENT_Y)) / GRADIENT_H;
            setting.setValue(ClickGuiUtil.applyOpacity(Color.HSBtoRGB(hue, saturation, brightness), alpha));
        }

        DrawUtil.drawRound(this.x + GRADIENT_X, this.y + GRADIENT_Y, GRADIENT_W, GRADIENT_H, 4.0F,
                0xFFFFFFFF, 0xFF000000, Color.HSBtoRGB(hue, 1.0F, 1.0F), 0xFF000000);
        float selectorX = this.x + GRADIENT_X + saturation * GRADIENT_W;
        float selectorY = this.y + GRADIENT_Y + (1.0F - brightness) * GRADIENT_H;
        DrawUtil.drawCircle(selectorX, selectorY, 3.5F, 0xFFFFFFFF);

        if (hueSelectorDragging && HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + HUE_Y, BAR_W, BAR_H)) {
            this.hue = (mouseX - (this.x + GRADIENT_X)) / BAR_W;
            setting.setValue(ClickGuiUtil.applyOpacity(Color.HSBtoRGB(hue, saturation, brightness), alpha));
        }

        float sliderX = this.x + GRADIENT_X;
        float sliderY = this.y + HUE_Y;
        float hueSize = BAR_W / 5.0F;
        for (int i = 0; i < 5; i++) {
            boolean last = i == 4;
            float size = last ? hueSize - 1.0F : hueSize;
            int color1 = Color.HSBtoRGB(0.2F * i, 1.0F, 1.0F);
            int color2 = Color.HSBtoRGB(0.2F * (i + 1), 1.0F, 1.0F);
            if (i == 0) {
                DrawUtil.drawRound(sliderX, sliderY, size, BAR_H, new org.joml.Vector4f(3.0F, 3.0F, 0.0F, 0.0F), color1, color1, color2, color2);
            } else if (last) {
                DrawUtil.drawRound(sliderX, sliderY, size, BAR_H, new org.joml.Vector4f(0.0F, 0.0F, 3.0F, 3.0F), color1, color1, color2, color2);
            } else {
                DrawUtil.drawRound(sliderX, sliderY, size, BAR_H, 0.0F, color1, color1, color2, color2);
            }
            sliderX += size;
        }
        DrawUtil.drawRound(this.x + GRADIENT_X + hue * (BAR_W - 5.0F), this.y + HUE_Y, 5.0F, BAR_H, 2.0F, 0xFFFFFFFF);

        if (alphaSelectorDragging && HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + ALPHA_Y, BAR_W, BAR_H)) {
            this.alpha = (mouseX - (this.x + GRADIENT_X)) / BAR_W;
            setting.setValue(ClickGuiUtil.applyOpacity(Color.HSBtoRGB(hue, saturation, brightness), alpha));
        }
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        DrawUtil.drawRound(this.x + GRADIENT_X, this.y + ALPHA_Y, BAR_W, BAR_H, 2.0F, 0xFFFFFFFF, 0xFFFFFFFF, color, color);
        DrawUtil.drawRound(this.x + GRADIENT_X + alpha * (BAR_W - 5.0F), this.y + ALPHA_Y, 5.0F, BAR_H, 2.0F, 0xFFFFFFFF);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        if (isHovered(mouseX, mouseY, 19.0F)) {
            extended = !extended;
            return;
        }
        if (!extended) return;
        if (HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + GRADIENT_Y, GRADIENT_W, GRADIENT_H)) {
            colorSelectorDragging = true;
        }
        if (HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + HUE_Y, BAR_W, BAR_H)) {
            hueSelectorDragging = true;
        }
        if (HoverUtil.isHovered(mouseX, mouseY, this.x + GRADIENT_X, this.y + ALPHA_Y, BAR_W, BAR_H)) {
            alphaSelectorDragging = true;
        }
    }

    private float[] RGBtoHSB(int color) {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        return Color.RGBtoHSB(red, green, blue, null);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        colorSelectorDragging = false;
        hueSelectorDragging = false;
        alphaSelectorDragging = false;
    }
}