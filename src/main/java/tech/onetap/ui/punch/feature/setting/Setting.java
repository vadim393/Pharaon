package tech.onetap.ui.punch.feature.setting;

import java.util.function.Supplier;

public abstract class Setting<T> {
    public enum WarningLevel {
        NONE,
        RISK,
        EXTRA_RISK
    }

    private final String name;
    private Supplier<Boolean> visible = () -> true;
    private WarningLevel warningLevel = WarningLevel.NONE;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public Setting<T> setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }

    public WarningLevel warningLevel() {
        return this.warningLevel;
    }

    public WarningLevel warningLevel(String option) {
        return this.warningLevel;
    }

    public void setWarningLevel(WarningLevel warningLevel) {
        this.warningLevel = warningLevel;
    }
}