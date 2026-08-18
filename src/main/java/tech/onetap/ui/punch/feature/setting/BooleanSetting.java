package tech.onetap.ui.punch.feature.setting;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class BooleanSetting extends Setting<Boolean> {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public BooleanSetting(String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
    }

    public BooleanSetting(String name, BooleanSupplier getter) {
        this(name, getter, value -> {});
    }

    public boolean getValue() {
        return this.getter.getAsBoolean();
    }

    public void setValue(boolean value) {
        this.setter.accept(value);
    }
}