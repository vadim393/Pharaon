package tech.onetap.ui.punch.feature.setting;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class MultiSelectSetting extends Setting<Void> {
    private final List<String> options;
    private final Predicate<String> selected;
    private final Map<String, java.util.function.Consumer<Boolean>> toggles;

    public MultiSelectSetting(String name, List<String> options, Predicate<String> selected,
                              Map<String, java.util.function.Consumer<Boolean>> toggles) {
        super(name);
        this.options = List.copyOf(options);
        this.selected = selected;
        this.toggles = toggles;
    }

    public List<String> getOptions() {
        return this.options;
    }

    public boolean isSelected(String option) {
        return this.selected.test(option);
    }

    public void toggle(String option) {
        java.util.function.Consumer<Boolean> toggle = this.toggles.get(option);
        if (toggle != null) {
            toggle.accept(!isSelected(option));
        }
    }
}