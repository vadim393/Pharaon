package tech.onetap.ui.punch.feature.setting;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ColorSetting extends Setting<Integer> {
    private final IntSupplier getter;
    private final IntConsumer setter;

    public ColorSetting(String name, IntSupplier getter, IntConsumer setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
    }

    public int getValue() {
        return this.getter.getAsInt();
    }

    public void setValue(int value) {
        this.setter.accept(value);
    }
}