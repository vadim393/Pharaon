package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.ThemeSetting;
import tech.onetap.module.settings.impl.Theme;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class ThemObject extends Object {
    public ThemeSetting set;
    public ModuleObject object;

    public ThemObject(ModuleObject object, ThemeSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
        this.height = 34.0F;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);
        Theme[] themes = set.getThemes();
        DrawUtil.drawText(Fonts.SFREGULAR.get(), set.getName(), x + 10, y + 3.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC11);

        int offset = 0;
        for (Theme theme : themes) {
            int color = theme.getColorFirst();
            float dotX = x + 10.0F + offset;
            float dotY = y + 16.0F;
            DrawUtil.drawRound(dotX, dotY, 10.0F, 10.0F, 3.0F, color);
            if (set.getCurrent() == theme) {
                ClickGuiUtil.drawRoundOutline(dotX, dotY, 10.0F, 10.0F, 3.0F, 0,
                        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
            }
            offset += 14;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        int offset = 0;
        for (Theme theme : set.getThemes()) {
            if (HoverUtil.isHovered(mouseX, mouseY, x + 10.0F + offset, y + 16.0F, 10.0F, 10.0F)) {
                set.setValue(theme);
                return;
            }
            offset += 14;
        }
    }
}