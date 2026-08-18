package tech.onetap.ui.punch.ui.controls;

import tech.onetap.ui.punch.feature.setting.MultiSelectSetting;
import tech.onetap.ui.punch.i18n.MenuText;
import tech.onetap.ui.punch.text.StringUtil;

import java.util.List;

public final class MultiSelectComponent extends DropdownComponent {
    public MultiSelectComponent(MultiSelectSetting setting) {
        super(() -> selectedOptions(setting));
    }

    @Override
    protected String displayValue() {
        return value();
    }

    private static String selectedOptions(MultiSelectSetting setting) {
        List<String> selected = setting.getOptions().stream()
                .filter(setting::isSelected)
                .map(MenuText::option)
                .toList();
        if (selected.isEmpty()) {
            return MenuText.option("Nothing selected");
        }
        return StringUtil.joinLimited(selected, 2);
    }
}
