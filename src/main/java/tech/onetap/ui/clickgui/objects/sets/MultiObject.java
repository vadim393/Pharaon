package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.List;

public class MultiObject extends Object {
    public ModeListSetting set;
    public ModuleObject object;

    public MultiObject(ModuleObject object, ModeListSetting set) {
        this.object = object;
        this.set = set;
        this.setting = set;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);
        List<BooleanSetting> options = set.getSettings();
        MsdfFont font = Fonts.SFREGULAR.get();
        float pillH = 15.0F;
        float pillGap = 5.0F;
        float pillPad = 7.0F;
        float right = this.x + this.width - 10.0F;

        DrawUtil.drawText(font, set.getName(), this.x + 10.0F, this.y + 3.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC11);

        float cursorX = this.x + 10.0F;
        float cursorY = this.y + 15.0F;
        int lines = 1;
        for (BooleanSetting option : options) {
            String name = option.getName();
            float pillW = font.getWidth(name, ClickGuiUtil.NC11) + pillPad * 2.0F;
            if (cursorX + pillW > right + 0.1F && cursorX > this.x + 10.0F) {
                cursorX = this.x + 10.0F;
                cursorY += pillH + pillGap;
                lines++;
            }
            boolean selected = option.getValue();
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, cursorX, cursorY, pillW, pillH);
            int bg = selected ? ClickGuiUtil.accent() : ColorProvider.interpolateColor(ClickGuiUtil.track(), ColorProvider.setAlpha(ClickGuiUtil.accent(), 70), hovered ? 0.5F : 0.0F);
            DrawUtil.drawRound(cursorX, cursorY, pillW, pillH, pillH / 2.0F, bg);
            int textColor = selected ? 0xFFFFFFFF : (hovered ? ClickGuiUtil.textSecondary() : ClickGuiUtil.textMuted());
            DrawUtil.drawText(font, name, cursorX + pillPad, cursorY + (pillH - ClickGuiUtil.NC11) / 2.0F, textColor, ClickGuiUtil.NC11);
            cursorX += pillW + pillGap;
        }
        this.height = 24.0F + lines * (pillH + pillGap);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        MsdfFont font = Fonts.SFREGULAR.get();
        float pillH = 15.0F;
        float pillGap = 5.0F;
        float pillPad = 7.0F;
        float right = this.x + this.width - 10.0F;
        float cursorX = this.x + 10.0F;
        float cursorY = this.y + 15.0F;
        for (BooleanSetting option : set.getSettings()) {
            String name = option.getName();
            float pillW = font.getWidth(name, ClickGuiUtil.NC11) + pillPad * 2.0F;
            if (cursorX + pillW > right + 0.1F && cursorX > this.x + 10.0F) {
                cursorX = this.x + 10.0F;
                cursorY += pillH + pillGap;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, cursorX, cursorY, pillW, pillH)) {
                option.setValue(!option.getValue());
                return;
            }
            cursorX += pillW + pillGap;
        }
    }
}