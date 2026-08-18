package tech.onetap.ui.punch.feature.setting;

public final class ButtonSetting extends Setting<Runnable> {
    private final String label;
    private final Runnable action;

    public ButtonSetting(String name, String label, Runnable action) {
        super(name);
        this.label = label == null ? "" : label;
        this.action = action;
    }

    public String getButtonLabel() {
        return this.label;
    }

    public void press() {
        if (this.action != null) {
            this.action.run();
        }
    }
}