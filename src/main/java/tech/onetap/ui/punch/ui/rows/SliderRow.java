package tech.onetap.ui.punch.ui.rows;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.ui.punch.feature.setting.NumberSetting;
import tech.onetap.ui.punch.i18n.MenuText;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.ui.controls.SliderComponent;
import tech.onetap.ui.punch.color.ColorUtil;
import tech.onetap.ui.punch.theme.Theme;
import tech.onetap.ui.punch.gui.MsdfFont;
import tech.onetap.ui.punch.gui.Render2DUtil;
import tech.onetap.ui.punch.gui.UiFontStyle;
import tech.onetap.ui.punch.gui.UiFonts;

public final class SliderRow extends SettingRow {
    private final NumberSetting number;
    private final SliderComponent slider;

    public SliderRow(NumberSetting setting) {
        super(setting);
        this.number = setting;
        this.slider = new SliderComponent(
                () -> (float) setting.getProgress(),
                progress -> setting.setValue(setting.getMin() + progress * (setting.getMax() - setting.getMin())),
                Theme.Colors.OUTLINES_SMALL,
                Theme.getAccent(),
                ColorUtil.WHITE
        ).step((float) (setting.getStep() / (setting.getMax() - setting.getMin())));
    }

    @Override
    protected void placeControl(Component owner) {
        float fontSize = 10.0F;
        String valStr = MenuText.numberValue(this.number);
        float textWidth = UiFonts.sfProDisplay().measureWidth(valStr, fontSize, fontSize * UiFontStyle.SEMIBOLD.letterSpacingEm());
        float badgeWidth = textWidth + 8.0F;
        float badgeX = this.rowX + this.rowWidth - PADDING - badgeWidth;

        int sliderW = Math.max(45, Math.min(65, this.rowWidth - PADDING * 2 - 95));
        int sliderX = (int) (badgeX - 6.0F - sliderW);

        this.slider.place(owner, sliderX, controlY(), sliderW, controlHeight())
                .style(Theme.Colors.OUTLINES_SMALL, controlAccent(this.setting), ColorUtil.WHITE)
                .alpha(this.rowAlpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY) {
        this.host.closeOtherRows(this);
        return this.slider.handleClick(mouseX, mouseY);
    }

    @Override
    public void drag(int mouseX) {
        this.slider.drag(mouseX);
    }

    @Override
    public boolean scroll(int mouseX, int mouseY, double vertical) {
        return this.slider.handleScroll(mouseX, mouseY, vertical);
    }

    @Override
    public void releasePointer() {
        this.slider.releasePointer();
    }

    @Override
    protected void renderExtras(MinecraftClient minecraft, DrawContext context) {
        float fontSize = 10.0F;
        String valStr = MenuText.numberValue(this.number);
        MsdfFont font = UiFonts.sfProDisplay();
        float textWidth = font.measureWidth(valStr, fontSize, fontSize * UiFontStyle.SEMIBOLD.letterSpacingEm());

        float badgeWidth = textWidth + 10.0F;
        float badgeHeight = 16.0F;
        float badgeX = this.rowX + this.rowWidth - PADDING - badgeWidth;
        float badgeY = this.rowY + (ROW_HEIGHT - badgeHeight) / 2.0F;

        Render2DUtil.rect(sx(badgeX), sy(badgeY), px(badgeWidth), px(badgeHeight))
                .color(ColorUtil.multiplyAlpha(ColorUtil.rgba(28, 29, 39, 255), this.rowAlpha))
                .radius(px(4.0F))
                .border(px(1.0F), ColorUtil.multiplyAlpha(Theme.getAccent(), this.rowAlpha))
                .draw();

        Render2DUtil.text(sx(badgeX + 5.0F), font.centeredTextY(sy(badgeY + badgeHeight / 2.0F), px(fontSize)), px(fontSize), valStr)
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, this.rowAlpha))
                .draw();
    }

    @Override
    protected void renderControl(MinecraftClient minecraft, DrawContext context) {
        this.slider.render(minecraft, context);
    }

    @Override
    protected int controlWidth() {
        return Math.max(45, Math.min(65, this.rowWidth - PADDING * 2 - 95));
    }

    @Override
    protected int controlHeight() {
        return Theme.Sizes.MODULE_CARD_SLIDER_HEIGHT;
    }

    @Override
    protected float labelRight() {
        float fontSize = 10.0F;
        String valStr = MenuText.numberValue(this.number);
        float textWidth = UiFonts.sfProDisplay().measureWidth(valStr, fontSize, fontSize * UiFontStyle.SEMIBOLD.letterSpacingEm());
        float badgeWidth = textWidth + 8.0F;
        float badgeX = this.rowX + this.rowWidth - PADDING - badgeWidth;
        int sliderW = Math.max(45, Math.min(65, this.rowWidth - PADDING * 2 - 95));
        int sliderX = (int) (badgeX - 6.0F - sliderW);
        return sliderX - 6.0F;
    }
}
