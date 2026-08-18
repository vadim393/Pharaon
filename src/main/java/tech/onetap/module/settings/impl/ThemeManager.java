package tech.onetap.module.settings.impl;

public class ThemeManager {
    private static ThemeManager instance;

    // Создаем стандартную тему по умолчанию (Название, Цвет 1, Цвет 2 в HEX формате ARGB)
    // 0xFF1E3D70 и 0xFF15305C - тёмно-синие цвета.
    // Вы можете поменять эти HEX-коды на любые другие свои любимые цвета!
    private final Theme defaultTheme = new Theme("Default", 0xFF1E3D70, 0xFF15305C);

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return defaultTheme; // Просто всегда возвращаем эту дефолтную тему
    }

}