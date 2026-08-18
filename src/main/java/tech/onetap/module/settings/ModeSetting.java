package tech.onetap.module.settings;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting {
    private String value;
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name);
        this.value = defaultValue;
        this.modes = Arrays.asList(modes);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        value = remapLegacyValue(value);
        if (modes.contains(value)) {
            this.value = value;
        }
    }

    public boolean is(String equalsIgnoreCase) {
        return value.equalsIgnoreCase(equalsIgnoreCase);
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return modes.indexOf(value);
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.value = modes.get(index);
        }
    }

    public void cycle() {
        int nextIndex = (getIndex() + 1) % modes.size();
        setValue(modes.get(nextIndex));
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        value = remapLegacyValue(value);
        if (modes.contains(value)) {
            this.value = value;
        }
    }

    private String remapLegacyValue(String value) {
        if (value == null) {
            return null;
        }
        if (modes.contains("ReallyWorld") && !modes.contains("Default") && value.equalsIgnoreCase("Default")) {
            return "ReallyWorld";
        }
        if (modes.contains("ReallyWorld") && !modes.contains("Universal Legit") && value.equalsIgnoreCase("Universal Legit")) {
            return "ReallyWorld";
        }
        return value;
    }

    @Override
    public ModeSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
