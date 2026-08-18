package tech.onetap.ui.punch.feature.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TextSetting extends Setting<String> {
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final boolean secret;

    public TextSetting(String name, Supplier<String> getter, Consumer<String> setter, boolean secret) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.secret = secret;
    }

    public TextSetting(String name, Supplier<String> getter, Consumer<String> setter) {
        this(name, getter, setter, false);
    }

    public String getValue() {
        return this.getter.get();
    }

    public void setValue(String value) {
        this.setter.accept(value);
    }

    public boolean isSecret() {
        return this.secret;
    }
}