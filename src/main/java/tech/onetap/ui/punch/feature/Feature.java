package tech.onetap.ui.punch.feature;

import tech.onetap.module.Module;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.module.settings.ThemeSetting;
import tech.onetap.ui.punch.feature.setting.BindSetting;
import tech.onetap.ui.punch.feature.setting.InputBindSetting;
import tech.onetap.ui.punch.feature.setting.MultiSelectSetting;
import tech.onetap.ui.punch.feature.setting.NumberSetting;
import tech.onetap.ui.punch.feature.setting.Setting;
import tech.onetap.ui.punch.feature.setting.TextSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Feature {
    private final Module module;
    private final BindSetting bind = new BindSetting();
    private final List<Setting<?>> settings;
    private boolean settingsInitialized;

    public Feature(Module module) {
        this.module = module;
        this.settings = new ArrayList<>();
    }

    public Module module() {
        return this.module;
    }

    public String getName() {
        return this.module.getName();
    }

    public String getDescription() {
        return this.module.getDesc();
    }

    public boolean isEnabled() {
        return this.module.isEnabled();
    }

    public boolean isToggleable() {
        return true;
    }

    public void toggle() {
        this.module.toggle();
    }

    public void setEnabled(boolean enabled) {
        this.module.setEnabled(enabled);
    }

    public boolean supportsBinds() {
        return true;
    }

    public BindSetting getBind() {
        return this.bind;
    }

    public boolean hasBindAt(int index) {
        return index >= 0 && index < this.bind.size();
    }

    public BindMode getBindModeAt(int index) {
        return this.bind.getMode(index);
    }

    public void setBindModeAt(int index, BindMode mode) {
        this.bind.setMode(index, mode);
    }

    public boolean isBindVisibleAt(int index) {
        return this.bind.isVisible(index);
    }

    public void setBindVisibleAt(int index, boolean visible) {
        this.bind.setVisible(index, visible);
    }

    public int addBind(int code) {
        return this.bind.add(code);
    }

    public int setBindAt(int index, int code) {
        return this.bind.set(index, code);
    }

    public void removeBindAt(int index) {
        this.bind.removeAt(index);
    }

    public void clearBind() {
        this.bind.clear();
    }

    public synchronized List<Setting<?>> getSettings() {
        if (this.settingsInitialized) {
            return this.settings;
        }
        this.settings.clear();
        for (tech.onetap.module.settings.Setting source : this.module.getSettings()) {
            Setting<?> mapped = map(source);
            if (mapped != null) {
                mapped.setVisible(source.visible);
                this.settings.add(mapped);
            }
        }
        this.settingsInitialized = true;
        return this.settings;
    }

    private Setting<?> map(tech.onetap.module.settings.Setting source) {
        if (source instanceof BooleanSetting booleanSetting) {
            return new tech.onetap.ui.punch.feature.setting.BooleanSetting(
                    source.getName(), booleanSetting::getValue, booleanSetting::setValue);
        }
        if (source instanceof SliderSetting slider) {
            String suffix = slider.getUnit() == null ? "" : slider.getUnit().format(1.0);
            return new NumberSetting(
                    source.getName(), slider::getValue, slider::setValue,
                    slider.getMin(), slider.getMax(), slider.getStep(), suffix);
        }
        if (source instanceof ModeSetting mode) {
            return new tech.onetap.ui.punch.feature.setting.ModeSetting(
                    source.getName(), mode::getValue, mode::setValue, mode.getModes());
        }
        if (source instanceof ModeListSetting modeList) {
            List<String> options = new ArrayList<>();
            Map<String, java.util.function.Consumer<Boolean>> toggles = new LinkedHashMap<>();
            for (BooleanSetting option : modeList.getSettings()) {
                options.add(option.getName());
                toggles.put(option.getName(), option::setValue);
            }
            return new MultiSelectSetting(
                    source.getName(), options, modeList::isEnabled, toggles);
        }
        if (source instanceof ColorSetting color) {
            return new tech.onetap.ui.punch.feature.setting.ColorSetting(
                    source.getName(), color::getValue, color::setValue);
        }
        if (source instanceof StringSetting string) {
            return new TextSetting(source.getName(), string::getValue, string::setValue);
        }
        if (source instanceof ThemeSetting theme) {
            List<String> options = new ArrayList<>();
            for (tech.onetap.module.settings.impl.Theme option : theme.getThemes()) {
                options.add(option.name);
            }
            return new tech.onetap.ui.punch.feature.setting.ModeSetting(
                    source.getName(),
                    () -> theme.getValue() == null ? null : theme.getValue().name,
                    theme::setValueFromString,
                    options);
        }
        if (source instanceof tech.onetap.module.settings.BindSetting bind) {
            return new InputBindSetting(source.getName(), bind.getValue(), bind::setValue);
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Feature feature && feature.module == this.module;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this.module);
    }
}
