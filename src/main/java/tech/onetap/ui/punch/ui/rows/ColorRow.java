package tech.onetap.ui.punch.ui.rows;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.ui.punch.feature.setting.ColorSetting;
import tech.onetap.ui.punch.ui.controls.ColorComponent;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.theme.Theme;

public final class ColorRow extends SettingRow {
    private final ColorComponent color;

    public ColorRow(ColorSetting setting) {
        super(setting);
        this.color = new ColorComponent(setting::getValue, setting::setValue);
    }

    @Override
    protected void placeControl(Component owner) {
        this.color.place(owner, controlX(), controlY(), controlWidth(), controlHeight())
                .mouse(this.mouseX, this.mouseY)
                .alpha(this.rowAlpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY) {
        this.host.closeOtherRows(this);
        return this.color.handleClick(mouseX, mouseY);
    }

    @Override
    public void closeTransient() {
        this.color.close();
    }

    @Override
    public void closeTransientImmediately() {
        this.color.closeImmediately();
    }

    @Override
    public boolean hasOpenPopup() {
        return this.color.isOpen();
    }

    @Override
    public boolean popupClick(int mouseX, int mouseY) {
        return this.color.handlePopupClick(mouseX, mouseY);
    }

    @Override
    public void renderPopupOverlay(MinecraftClient minecraft, DrawContext context) {
        this.color.renderOverlay();
    }

    @Override
    protected void renderControl(MinecraftClient minecraft, DrawContext context) {
        this.color.render(minecraft, context);
    }

    @Override
    protected int labelColor() {
        return Theme.Colors.TEXT_TEXT;
    }

    @Override
    protected int controlWidth() {
        return Theme.Sizes.COLOR_PREVIEW_WIDTH;
    }

    @Override
    protected int controlHeight() {
        return Theme.Sizes.COLOR_PREVIEW_HEIGHT;
    }
}
