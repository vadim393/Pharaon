package tech.onetap.ui.punch.ui.rows;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.ui.punch.feature.setting.InputBindSetting;
import tech.onetap.ui.punch.i18n.MenuText;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.ui.controls.InputBindComponent;
import tech.onetap.ui.punch.theme.Theme;

public final class BindChipRow extends SettingRow {
    private final InputBindSetting bindSetting;
    private final InputBindComponent chip;

    public BindChipRow(InputBindSetting setting) {
        super(setting);
        this.bindSetting = setting;
        this.chip = new InputBindComponent(setting);
    }

    @Override
    protected void placeControl(Component owner) {
        this.chip.place(owner, controlX(), controlY(), controlWidth(), controlHeight())
                .alpha(this.rowAlpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY) {
        this.host.closeOtherRows(this);
        this.chip.handleClick(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean key(int key) {
        return this.chip.captureKey(key);
    }

    @Override
    public boolean captureMouse(int button) {
        return this.chip.captureMouse(button);
    }

    @Override
    public boolean isCapturingBind() {
        return this.chip.isListening();
    }

    @Override
    public void closeTransient() {
        this.chip.stopListening();
    }

    @Override
    protected void renderControl(MinecraftClient minecraft, DrawContext context) {
        this.chip.render(minecraft, context);
    }

    @Override
    protected int labelColor() {
        return Theme.Colors.TEXT_TEXT;
    }

    @Override
    protected int controlWidth() {
        return InputBindComponent.pillWidth(MenuText.bind(this.bindSetting.getDisplayValue()));
    }

    @Override
    protected int controlHeight() {
        return Theme.Sizes.INPUT_BIND_HEIGHT;
    }
}
