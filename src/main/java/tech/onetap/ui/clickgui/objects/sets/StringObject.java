package tech.onetap.ui.clickgui.objects.sets;

import tech.onetap.module.settings.StringSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class StringObject extends Object {
    public StringSetting set;
    public ModuleObject object;
    public boolean typingMode;
    public float cursorBlink;

    public StringObject(ModuleObject object, StringSetting set) {
        this.object = object;
        this.set = set;
        this.setting = set;
        this.height = 19.0F;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), set.getName(), this.x + 10.0F, this.y + (this.height - ClickGuiUtil.NC12) / 2.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC12);

        String text = set.getValue();
        if (text.isEmpty() && !typingMode) {
            text = "Здесь текст";
        }
        if (typingMode && (System.currentTimeMillis() / 500L) % 2 == 0) {
            text = text + "_";
        }

        float textW = Fonts.SFREGULAR.get().getWidth(text, ClickGuiUtil.NC11);
        float pillW = Math.min(this.width - 90.0F, Math.max(40.0F, textW + 14.0F));
        float pillH = 15.0F;
        float pillX = this.x + this.width - pillW - 10.0F;
        float pillY = this.y + (this.height - pillH) / 2.0F;

        int bg = typingMode ? ColorProvider.interpolateColor(ClickGuiUtil.track(), ColorProvider.setAlpha(ClickGuiUtil.accent(), 90), 0.6F) : ClickGuiUtil.track();
        DrawUtil.drawRound(pillX, pillY, pillW, pillH, pillH / 2.0F, bg);
        int textColor = set.getValue().isEmpty() && !typingMode ? ClickGuiUtil.textMuted() : ClickGuiUtil.textColor();
        DrawUtil.drawText(Fonts.SFREGULAR.get(), text, pillX + 7.0F, pillY + (pillH - ClickGuiUtil.NC11) / 2.0F, textColor, ClickGuiUtil.NC11);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            typingMode = true;
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (typingMode && keyCode == 259) {
            String value = set.getValue();
            if (!value.isEmpty()) {
                set.setValue(value.substring(0, value.length() - 1));
            }
        }
        if (typingMode && (keyCode == 257 || keyCode == 256)) {
            typingMode = false;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (typingMode) {
            set.setValue(set.getValue() + codePoint);
        }
    }

    @Override
    public void exit() {
        typingMode = false;
    }
}