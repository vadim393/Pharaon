package tech.onetap.ui.punch.feature.setting;

public final class InputBindSetting extends Setting<Integer> {
    private int code;
    private final java.util.function.IntConsumer onBind;

    public InputBindSetting(String name, int initialCode, java.util.function.IntConsumer onBind) {
        super(name);
        this.code = initialCode;
        this.onBind = onBind;
    }

    public int getCode() {
        return this.code;
    }

    public boolean isBound() {
        return this.code != -1;
    }

    public String getDisplayValue() {
        return isBound() ? BindSetting.labelRaw(this.code) : "None";
    }

    public void clear() {
        setCode(-1);
    }

    public void setKey(int key) {
        setCode(BindSetting.key(key));
    }

    public void setMouse(int button) {
        setCode(button);
    }

    private void setCode(int code) {
        if (this.code != code) {
            this.code = code;
            if (this.onBind != null) {
                this.onBind.accept(code);
            }
        }
    }
}
