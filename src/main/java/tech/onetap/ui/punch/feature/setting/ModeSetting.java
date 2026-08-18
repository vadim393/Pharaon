package tech.onetap.ui.punch.feature.setting;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ModeSetting extends Setting<String> {
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final List<String> modes;

    public ModeSetting(String name, Supplier<String> getter, Consumer<String> setter, List<String> modes) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.modes = List.copyOf(modes);
    }

    public String getValue() {
        return this.getter.get();
    }

    public void setValue(String value) {
        this.setter.accept(value);
    }

    public List<String> getModes() {
        return this.modes;
    }

    public boolean is(String equalsIgnoreCase) {
        String current = this.getValue();
        return current != null && current.equalsIgnoreCase(equalsIgnoreCase);
    }
}