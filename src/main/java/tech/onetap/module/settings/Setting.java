package tech.onetap.module.settings;

import java.util.function.Supplier;

public abstract class Setting implements ISetting {
    private final String name;
    private String desc;
    public Supplier<Boolean> visible = () -> true;

    public Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public abstract String getValueAsString();
    public abstract void setValueFromString(String value);
}