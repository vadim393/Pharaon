package tech.onetap.ui.punch.ui.rows;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.ui.punch.feature.setting.BooleanSetting;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.ui.controls.ToggleComponent;
import tech.onetap.ui.punch.theme.Theme;

public final class ToggleRow extends SettingRow {
    private final ToggleComponent toggle;

    public ToggleRow(BooleanSetting setting) {
        super(setting);
        this.toggle = new ToggleComponent(
                setting::getValue,
                () -> setting.setValue(!setting.getValue()),
                ToggleComponent.Style.SWITCH,
                Theme.Colors.CONTROL_STRONG,
                Theme.getAccent()
        );
    }

    @Override
    protected void placeControl(Component owner) {
        this.toggle.place(owner, controlX(), controlY(), controlWidth(), controlHeight())
                .style(ToggleComponent.Style.SWITCH, Theme.Colors.CONTROL_STRONG, controlAccent(this.setting))
                .alpha(this.rowAlpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY) {
        this.host.closeOtherRows(this);
        return this.toggle.handleClick(mouseX, mouseY);
    }

    @Override
    protected void renderControl(MinecraftClient minecraft, DrawContext context) {
        this.toggle.render(minecraft, context);
    }

    @Override
    protected int controlWidth() {
        return Theme.Sizes.MODULE_CARD_SWITCH_WIDTH;
    }

    @Override
    protected int controlHeight() {
        return Theme.Sizes.MODULE_CARD_SWITCH_HEIGHT;
    }
}
