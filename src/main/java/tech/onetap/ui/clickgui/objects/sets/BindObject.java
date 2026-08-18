package tech.onetap.ui.clickgui.objects.sets;

import org.lwjgl.glfw.GLFW;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.keyboard.KeyStorage;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public class BindObject extends Object {
    private static BindObject activeBind;

    private final ModuleObject object;
    private final String bindName;
    private final BindSetting bind;
    private boolean listening;

    public BindObject(ModuleObject object) {
        this.object = object;
        this.bindName = "Клавиша";
        this.bind = null;
        this.height = 19.0F;
    }

    public BindObject(ModuleObject object, BindSetting bind) {
        this.object = object;
        this.bindName = bind.getName();
        this.bind = bind;
        this.setting = bind;
        this.height = 19.0F;
    }

    public static boolean hasActiveListening() {
        return activeBind != null && activeBind.listening;
    }

    public static boolean captureMouseButton(int button) {
        if (activeBind == null || !activeBind.listening) return false;
        if (button <= GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return false;
        activeBind.setKey(button);
        activeBind.listening = false;
        activeBind = null;
        return true;
    }

    private int getKey() {
        return bind != null ? bind.getValue() : object.module.getKey();
    }

    private void setKey(int key) {
        if (bind != null) {
            bind.setValue(key);
        } else {
            object.module.setKey(key);
        }
    }

    private String keyName(int key) {
        if (key == -1) return "Нет";
        if (key >= GLFW.GLFW_MOUSE_BUTTON_4 && key <= GLFW.GLFW_MOUSE_BUTTON_8) return "M" + (key + 1);
        String name = KeyStorage.getKey(key);
        if (name == null || name.isEmpty()) return "Код " + key;
        return name;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);

        DrawUtil.drawText(Fonts.SFREGULAR.get(), bindName, this.x + 10.0F, this.y + (this.height - ClickGuiUtil.NC12) / 2.0F, ClickGuiUtil.textSecondary(), ClickGuiUtil.NC12);

        int key = getKey();
        String text = listening ? "..." : keyName(key);

        float pillW = 52.0F;
        float pillH = 14.0F;
        float pillX = this.x + this.width - pillW - 10.0F;
        float pillY = this.y + (this.height - pillH) / 2.0F;

        int bg = listening
                ? ColorProvider.interpolateColor(ClickGuiUtil.track(), ClickGuiUtil.accent(), 0.7F)
                : ClickGuiUtil.track();
        DrawUtil.drawRound(pillX, pillY, pillW, pillH, pillH / 2.0F, bg);

        if (isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH)) {
            DrawUtil.drawRound(pillX, pillY, pillW, pillH, pillH / 2.0F, ColorProvider.setAlpha(ClickGuiUtil.accent(), 40));
        }

        float textSize = 8.0F;
        float textW = Fonts.SFREGULAR.get().getWidth(text, textSize);
        int color = listening
                ? ColorProvider.setAlpha(0xFFFFFFFF, 220)
                : (key == -1 ? ClickGuiUtil.textMuted() : ClickGuiUtil.textColor());
        DrawUtil.drawText(Fonts.SFREGULAR.get(), text, pillX + (pillW - textW) / 2.0F, pillY + (pillH - textSize) / 2.0F, color, textSize);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isHovered(mouseX, mouseY)) return;

        if (activeBind != null && activeBind != this) {
            activeBind.listening = false;
        }
        this.listening = !this.listening;
        activeBind = this.listening ? this : null;
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (!listening) return;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            setKey(-1);
        } else if (keyCode >= 0) {
            setKey(keyCode);
        }
        listening = false;
        activeBind = null;
    }

    @Override
    public void exit() {
        listening = false;
        if (activeBind == this) {
            activeBind = null;
        }
    }
}