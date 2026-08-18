package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.SliderSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class SliderObject extends Object {
    public ModuleObject object;
    public SliderSetting set;
    public boolean sliding;
    public float animatedFill;
    public float animatedKnobX;

    public SliderObject(ModuleObject object, SliderSetting set) {
        this.object = object;
        this.set = set;
        this.setting = set;
        this.height = 26.0F;
        this.animatedKnobX = this.x;
    }

    private float getFillWidth() {
        float trackWidth = this.width - 20.0F;
        float ratio = (float) ((set.getValue() - set.getMin()) / (set.getMax() - set.getMin()));
        return Math.max(0.0F, Math.min(trackWidth, ratio * trackWidth));
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);

        if (sliding) {
            float trackWidth = this.width - 20.0F;
            float ratio = (float) ((mouseX - (this.x + 10.0F)) / trackWidth);
            double value = set.getMin() + ratio * (set.getMax() - set.getMin());
            if (set.getStep() > 0.0) {
                value = Math.round(value / set.getStep()) * set.getStep();
            }
            set.setValue(value);
        }

        DrawUtil.drawText(Fonts.SFREGULAR.get(), set.getName(), this.x + 10.0F, this.y + 3.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC11);

        String valueText = set.getFormattedValue();
        float valueWidth = Fonts.SFREGULAR.get().getWidth(valueText, ClickGuiUtil.NC11);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), valueText, this.x + this.width - 10.0F - valueWidth, this.y + 3.0F, ClickGuiUtil.textColor(), ClickGuiUtil.NC11);

        float trackX = this.x + 10.0F;
        float trackY = this.y + 17.0F;
        float trackW = this.width - 20.0F;
        float trackH = 4.0F;

        this.animatedFill = ClickGuiUtil.fast(animatedFill, getFillWidth(), 14.0F);
        float targetKnob = trackX + animatedFill;
        this.animatedKnobX = ClickGuiUtil.fast(animatedKnobX, targetKnob, 14.0F);

        DrawUtil.drawRound(trackX, trackY, trackW, trackH, trackH / 2.0F, ClickGuiUtil.track());
        if (animatedFill > 0.5F) {
            DrawUtil.drawRound(trackX, trackY, animatedFill, trackH, trackH / 2.0F, ClickGuiUtil.accent());
        }
        DrawUtil.drawCircle(animatedKnobX, trackY + trackH / 2.0F, 4.0F, ColorProvider.setAlpha(0xFFFFFFFF, 255));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            sliding = true;
        }
    }

    @Override
    public void exit() {
        super.exit();
        sliding = false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        sliding = false;
    }
}