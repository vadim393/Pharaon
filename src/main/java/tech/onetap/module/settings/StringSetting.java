package tech.onetap.module.settings;

import java.util.function.Supplier;

public class StringSetting extends Setting {
    private String value;
    private final int maxLength;

    public StringSetting(String name, String defaultValue) {
        this(name, defaultValue, 30);
    }

    public StringSetting(String name, String defaultValue, int maxLength) {
        super(name);
        this.maxLength = Math.max(1, maxLength);
        setValue(defaultValue);
    }

    public String getValue() {
        return value;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.length() > maxLength) {
            safeValue = safeValue.substring(0, maxLength);
        }
        this.value = safeValue;
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        setValue(value);
    }

    @Override
    public StringSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
