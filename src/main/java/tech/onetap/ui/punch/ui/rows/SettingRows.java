package tech.onetap.ui.punch.ui.rows;

import tech.onetap.ui.punch.feature.setting.BooleanSetting;
import tech.onetap.ui.punch.feature.setting.ButtonSetting;
import tech.onetap.ui.punch.feature.setting.ColorSetting;
import tech.onetap.ui.punch.feature.setting.InputBindSetting;
import tech.onetap.ui.punch.feature.setting.ModeSetting;
import tech.onetap.ui.punch.feature.setting.MultiSelectSetting;
import tech.onetap.ui.punch.feature.setting.NumberSetting;
import tech.onetap.ui.punch.feature.setting.Setting;
import tech.onetap.ui.punch.feature.setting.TextSetting;

public final class SettingRows {
    private SettingRows() {
    }

    public static SettingRow create(String featureName, Setting<?> setting) {
        SettingRow row = switch (setting) {
            case BooleanSetting booleanSetting -> new ToggleRow(booleanSetting);
            case ButtonSetting buttonSetting -> new ButtonRow(buttonSetting);
            case NumberSetting numberSetting -> new SliderRow(numberSetting);
            case ModeSetting modeSetting -> new DropdownRow(modeSetting);
            case MultiSelectSetting multiSelectSetting -> new DropdownRow(multiSelectSetting);
            case ColorSetting colorSetting -> new ColorRow(colorSetting);
            case TextSetting textSetting -> new TextRow(textSetting);
            case InputBindSetting inputBindSetting -> new BindChipRow(inputBindSetting);
            default -> null;
        };
        return row == null ? null : row.context(featureName);
    }
}
