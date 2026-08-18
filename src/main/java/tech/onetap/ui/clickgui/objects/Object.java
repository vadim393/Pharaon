package tech.onetap.ui.clickgui.objects;

import tech.onetap.module.settings.Setting;

public abstract class Object implements IObject {
    public float x;
    public float y;
    public float width;
    public float height;
    public Setting setting;

    public boolean isHovered(int mouseX, int mouseY, float width, float height) {
        return (float) mouseX >= this.x && (float) mouseY >= this.y && (float) mouseX < this.x + width && (float) mouseY < this.y + height;
    }

    public boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return (float) mouseX >= x && (float) mouseY >= y && (float) mouseX < x + width && (float) mouseY < y + height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return (float) mouseX > this.x && (float) mouseX < this.x + this.width && (float) mouseY > this.y && (float) mouseY < this.y + this.height;
    }

    public boolean isHovered(int mouseX, int mouseY, float height) {
        return (float) mouseX > this.x && (float) mouseX < this.x + this.width && (float) mouseY > this.y && (float) mouseY < this.y + height;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
    }

    @Override
    public void exit() {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}
