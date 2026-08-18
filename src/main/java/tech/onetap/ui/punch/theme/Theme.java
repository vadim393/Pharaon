package tech.onetap.ui.punch.theme;

import tech.onetap.ui.punch.core.MenuConfigStore;

public final class Theme {
    private static int accentIndex;

    private static final int[] ACCENTS = {
            0xFF58A6FF,
            0xFFFF6B6B,
            0xFF51CF66,
            0xFFFFD43B,
            0xFFBD93F9,
            0xFFFF9F43,
            0xFF4DD0E1,
            0xFFF06292,
    };

    private Theme() {
    }

    public static int accent(int index) {
        return ACCENTS[Math.floorMod(index, ACCENTS.length)];
    }

    public static int accentIndex() {
        return accentIndex;
    }

    public static int accentCount() {
        return ACCENTS.length;
    }

    public static void setAccentIndex(int index) {
        accentIndex = Math.floorMod(index, ACCENTS.length);
        MenuConfigStore.saveAccentIndex(accentIndex);
    }

    public static int getAccent() {
        return ACCENTS[Math.floorMod(accentIndex, ACCENTS.length)];
    }

    public static void loadAccentIndex() {
        accentIndex = MenuConfigStore.getInt("accentIndex", 0);
    }

    public static final class Colors {
        public static final int BACKGROUND_PRIMARY_50 = 0xFF17181C;
        public static final int BACKGROUND_SURFACE_M = 0xFF101116;
        public static final int BACKGROUND_SURFACE_S = 0xFF0B0C0F;
        public static final int CARD_HOVER = 0xFF1D1F26;
        public static final int CONTROL = 0xFFFFFFFF;
        public static final int CONTROL_ALT = 0xFF1A1C22;
        public static final int CONTROL_SHADOW = 0xFF000000;
        public static final int CONTROL_STRONG = 0xFF2A2C34;
        public static final int DIVIDER_HEADER = 0xFF2A2C34;
        public static final int EXTRA_RISK = 0xFFFF3159;
        public static final int ICON = 0xFFD7DAE3;
        public static final int ICON_GHOST = 0xFF888A96;
        public static final int ICON_MUTED = 0xFF5A5C68;
        public static final int OUTLINES_LARGE = 0xFF33353E;
        public static final int OUTLINES_MEDIUM = 0xFF2A2C34;
        public static final int OUTLINES_SMALL = 0xFF23252C;
        public static final int OVERLAY = 0x66000000;
        public static final int PANEL = 0xDE17181D;
        public static final int PANEL_BORDER = 0xFF34363F;
        public static final int PANEL_SHADOW = 0xCC000000;
        public static final int PANEL_SHADOW_STRONG = 0xE6000000;
        public static final int POPUP_SHADOW = 0xE6000000;
        public static final int PRIMARY = 0xFF58A6FF;
        public static final int RISK = 0xFFFFB020;
        public static final int SECONDARY = 0xFFFFFFFF;
        public static final int SECONDARY_DARK = 0xFF8B8E9A;
        public static final int SEPARATOR = 0xFF25272E;
        public static final int SURFACE_ACTIVE = 0xFF262830;
        public static final int SURFACE_HOVER = 0xFF1E2026;
        public static final int SYSTEM_INFORMATION = 0xFF4DD0E1;
        public static final int SYSTEM_RED = 0xFFFF4D6D;
        public static final int TEXT = 0xFFF2F3F7;
        public static final int TEXT_GHOST = 0xFF6A6C78;
        public static final int TEXT_TEXT = 0xFFC9CBD6;
        public static final int TEXT_TITLE = 0xFFF6F7FA;
        public static final int TRAFFIC_CLOSE = 0xFFFF5F57;
        public static final int TRAFFIC_MAXIMIZE = 0xFF28C840;
        public static final int TRAFFIC_MINIMIZE = 0xFFFEBC2E;
    }

    public static final class Sizes {
        public static final int COLOR_BLOCK_RADIUS = 3;
        public static final int COLOR_PICKER_CELL_SIZE = 12;
        public static final int COLOR_PICKER_COLUMNS = 12;
        public static final int COLOR_PICKER_GRID_RADIUS = 8;
        public static final int COLOR_PICKER_OFFSET_Y = 34;
        public static final int COLOR_PICKER_PADDING = 8;
        public static final int COLOR_PICKER_RADIUS = 10;
        public static final int COLOR_PREVIEW_HEIGHT = 26;
        public static final int COLOR_PREVIEW_PADDING = 4;
        public static final int COLOR_PREVIEW_RADIUS = 8;
        public static final int COLOR_PREVIEW_WIDTH = 34;
        public static final int DEFAULT_PANEL_RADIUS_INDEX = 1;
        public static final int DROPDOWN_GAP = 6;
        public static final int DROPDOWN_ICON_PADDING = 6;
        public static final int DROPDOWN_ICON_SIZE = 12;
        public static final int DROPDOWN_POPUP_GAP = 4;
        public static final int DROPDOWN_POPUP_ITEM_HEIGHT = 28;
        public static final int DROPDOWN_POPUP_ITEM_RADIUS = 6;
        public static final int DROPDOWN_POPUP_OFFSET_Y = 2;
        public static final int DROPDOWN_POPUP_PADDING = 4;
        public static final int DROPDOWN_POPUP_RADIUS = 10;
        public static final int DROPDOWN_POPUP_TEXT_WIDTH = 96;
        public static final int DROPDOWN_POPUP_WIDTH = 120;
        public static final int DROPDOWN_VALUE_HEIGHT = 22;
        public static final int DROPDOWN_VALUE_WIDTH = 120;
        public static final int HEADER_BRAND_HEIGHT = 20;
        public static final int HEADER_BRAND_WIDTH = 84;
        public static final int HEADER_CONTROL_BUTTON_PADDING = 4;
        public static final int HEADER_CONTROL_BUTTON_SIZE = 14;
        public static final int HEADER_CONTROLS_GAP = 6;
        public static final int HEADER_CONTROLS_HEIGHT = 36;
        public static final int HEADER_CONTROLS_X = 0;
        public static final int HEADER_CONTROLS_Y = 4;
        public static final int HEADER_DIVIDER_HEIGHT = 30;
        public static final int HEADER_HEIGHT = 40;
        public static final int HEADER_ICON_SIZE = 18;
        public static final int HEADER_PROFILE_ICON_SIZE = 26;
        public static final int HEADER_TRAFFIC_GAP = 6;
        public static final int HEADER_TRAFFIC_SIZE = 12;
        public static final int INPUT_BIND_HEIGHT = 22;
        public static final int INPUT_BIND_PADDING_X = 8;
        public static final int INPUT_BIND_RADIUS = 6;
        public static final int INPUT_BIND_TEXT_SIZE = 10;
        public static final int INPUT_BIND_WIDTH = 96;
        public static final int INPUT_HEIGHT = 26;
        public static final int INPUT_PADDING_X = 10;
        public static final int INPUT_PADDING_Y = 6;
        public static final int INPUT_RADIUS = 8;
        public static final int INPUT_TEXT_SIZE = 12;
        public static final int INPUT_TEXT_WIDTH = 160;
        public static final int INPUT_WIDTH = 220;
        public static final int MODULE_CARD_ACTION_ICON = 16;
        public static final int MODULE_CARD_HEADER_HEIGHT = 34;
        public static final int MODULE_CARD_PADDING = 10;
        public static final int MODULE_CARD_RADIUS = 10;
        public static final int MODULE_CARD_ROW_HEIGHT = 26;
        public static final int MODULE_CARD_SLIDER_HEIGHT = 22;
        public static final int MODULE_CARD_SWITCH_HEIGHT = 20;
        public static final int MODULE_CARD_SWITCH_WIDTH = 36;
        public static final int PANEL_RADIUS = 12;
        public static final int SLIDER_HEIGHT = 22;
        public static final int SLIDER_KNOB_SIZE = 12;
        public static final int SLIDER_PADDING = 6;
        public static final int SLIDER_SHADOW_BLUR = 12;
        public static final int SLIDER_TRACK_HEIGHT = 4;
        public static final int SLIDER_WIDTH = 140;
        public static final int[] PANEL_RADII = {0, 8, 12, 16, 24};
    }
}