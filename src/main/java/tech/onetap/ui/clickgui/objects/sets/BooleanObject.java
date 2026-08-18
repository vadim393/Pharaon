package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class BooleanObject extends Object {
    public ModuleObject object;
    public BooleanSetting set;

    public BooleanObject(ModuleObject object, BooleanSetting set) {
        this.object = object;
        this.set = set;
        this.setting = set;
        this.height = 19.0F;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);
        float anim = set.getAnimation().getValue();

        DrawUtil.drawText(Fonts.SFREGULAR.get(), set.getName(), this.x + 10.0F, this.y + (this.height - ClickGuiUtil.NC12) / 2.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC12);

        float trackW = 24.0F;
        float trackH = 11.0F;
        float trackX = this.x + this.width - trackW - 10.0F;
        float trackY = this.y + (this.height - trackH) / 2.0F;
        int trackColor = ColorProvider.interpolateColor(ClickGuiUtil.track(), ClickGuiUtil.accent(), anim);
        DrawUtil.drawRound(trackX, trackY, trackW, trackH, trackH / 2.0F, trackColor);

        float knob = 7.0F;
        float knobX = trackX + 1.5F + (trackW - 3.0F - knob) * anim;
        float knobY = trackY + (trackH - knob) / 2.0F;
        DrawUtil.drawRound(knobX, knobY, knob, knob, knob / 2.0F, ColorProvider.setAlpha(0xFFFFFFFF, (int) (255.0F * (0.5F + 0.5F * anim))));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            set.toggle();
        }
    }
}