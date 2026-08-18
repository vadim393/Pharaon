package tech.onetap.ui.punch.feature.setting;

import org.lwjgl.glfw.GLFW;
import tech.onetap.ui.punch.feature.BindMode;

import java.util.ArrayList;
import java.util.List;

public final class BindSetting {
    private final List<Integer> codes = new ArrayList<>();
    private final List<BindMode> modes = new ArrayList<>();
    private final List<Boolean> visible = new ArrayList<>();

    public static int key(int keyCode) {
        return keyCode;
    }

    public static int mouse(int button) {
        return ~button;
    }

    public static boolean isMouse(int code) {
        return code < 0;
    }

    public static String label(int code) {
        if (code < 0) {
            int button = ~code;
            return switch (button) {
                case 0 -> "Mouse Left";
                case 1 -> "Mouse Right";
                case 2 -> "Mouse Middle";
                default -> "Mouse " + (button + 1);
            };
        }
        return keyName(code);
    }

    public static String labelRaw(int code) {
        if (isMouse(code)) {
            return label(code);
        }
        if (code >= 0 && code < 8) {
            return switch (code) {
                case 0 -> "Mouse Left";
                case 1 -> "Mouse Right";
                case 2 -> "Mouse Middle";
                case 3 -> "Mouse 4";
                case 4 -> "Mouse 5";
                default -> "Mouse " + (code + 1);
            };
        }
        return keyName(code);
    }

    public static boolean isAllowed(int code) {
        if (!isMouse(code)) {
            return true;
        }
        int button = ~code;
        return button >= GLFW.GLFW_MOUSE_BUTTON_4;
    }

    public static boolean isForbiddenMouse(int code) {
        return isMouse(code) && !isAllowed(code);
    }

    public boolean isEmpty() {
        return this.codes.isEmpty();
    }

    public int size() {
        return this.codes.size();
    }

    public int get(int index) {
        return this.codes.get(index);
    }

    public BindMode getMode(int index) {
        return this.modes.get(index);
    }

    public void setMode(int index, BindMode mode) {
        this.modes.set(index, mode);
    }

    public boolean isVisible(int index) {
        return this.visible.get(index);
    }

    public void setVisible(int index, boolean value) {
        this.visible.set(index, value);
    }

    public String getDisplayValue(int index) {
        return label(this.codes.get(index));
    }

    public int indexOfCode(int code) {
        return this.codes.indexOf(code);
    }

    public int add(int code) {
        if (!isAllowed(code)) {
            return -1;
        }
        this.codes.add(code);
        this.modes.add(BindMode.TOGGLE);
        this.visible.add(true);
        return this.codes.size() - 1;
    }

    public int set(int index, int code) {
        if (!isAllowed(code)) {
            return index;
        }
        this.codes.set(index, code);
        return index;
    }

    public void removeAt(int index) {
        this.codes.remove(index);
        this.modes.remove(index);
        this.visible.remove(index);
    }

    public void clear() {
        this.codes.clear();
        this.modes.clear();
        this.visible.clear();
    }

    public static String keyName(int keyCode) {
        if (keyCode >= 65 && keyCode <= 90) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= 48 && keyCode <= 57) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= 290 && keyCode <= 301) {
            return "F" + (keyCode - 289);
        }
        return switch (keyCode) {
            case 32 -> "Space";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case 256 -> "Escape";
            case 257 -> "Enter";
            case 258 -> "Tab";
            case 259 -> "Backspace";
            case 260 -> "Insert";
            case 261 -> "Delete";
            case 262 -> "Right";
            case 263 -> "Left";
            case 264 -> "Down";
            case 265 -> "Up";
            case 266 -> "Page Up";
            case 267 -> "Page Down";
            case 268 -> "Home";
            case 269 -> "End";
            case 280 -> "Caps Lock";
            case 340 -> "Left Shift";
            case 341 -> "Left Control";
            case 342 -> "Left Alt";
            case 343 -> "Left Super";
            case 344 -> "Right Shift";
            case 345 -> "Right Control";
            case 346 -> "Right Alt";
            case 347 -> "Right Super";
            default -> "None";
        };
    }
}
