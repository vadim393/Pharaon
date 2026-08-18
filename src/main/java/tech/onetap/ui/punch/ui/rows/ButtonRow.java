package tech.onetap.ui.punch.ui.rows;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.ui.punch.feature.setting.ButtonSetting;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.ui.controls.ButtonComponent;
import tech.onetap.ui.punch.theme.Theme;

public final class ButtonRow extends SettingRow {
    private final ButtonComponent button;

    public ButtonRow(ButtonSetting setting) {
        super(setting);
        this.button = new ButtonComponent(setting.getButtonLabel(), setting::press);
    }

    @Override
    protected void placeControl(Component owner) {
        this.button.place(
                        owner,
                        controlX(),
                        controlY(),
                        controlWidth(),
                        controlHeight(),
                        this.mouseX,
                        this.mouseY
                )
                .alpha(this.rowAlpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY) {
        this.host.closeOtherRows(this);
        return this.button.handleClick(mouseX, mouseY);
    }

    @Override
    protected void renderControl(
            MinecraftClient minecraft,
            DrawContext context
    ) {
        this.button.render(minecraft, context);
    }

    @Override
    protected int labelColor() {
        return Theme.Colors.TEXT_TEXT;
    }

    @Override
    protected int controlWidth() {
        return Theme.Sizes.INPUT_WIDTH;
    }

    @Override
    protected int controlHeight() {
        return Theme.Sizes.INPUT_HEIGHT;
    }
}
