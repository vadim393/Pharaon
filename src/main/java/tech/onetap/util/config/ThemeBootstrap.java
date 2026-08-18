package tech.onetap.util.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tech.onetap.module.settings.impl.ThemeManager;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ThemeBootstrap {
    private static final Path THEME_FILE = Paths.get("onetap", "themes.json");

    private ThemeBootstrap() {
    }

    public static void applySavedThemeIfPresent() {
        List<ThemeEntry> defaults = getDefaultThemes();
        ThemeEntry fallback = defaults.get(0);
        ThemeEntry selected = fallback;

        if (Files.exists(THEME_FILE)) {
            try {
                String content = Files.readString(THEME_FILE);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();

                String activeName = json.has("activeTheme") ? json.get("activeTheme").getAsString() : fallback.name;

                ThemeEntry defaultMatch = findByName(defaults, activeName);
                if (defaultMatch != null) {
                    selected = defaultMatch;
                }

                if (json.has("customThemes") && json.get("customThemes").isJsonArray()) {
                    JsonArray customThemes = json.getAsJsonArray("customThemes");
                    for (JsonElement element : customThemes) {
                        if (!element.isJsonObject()) continue;
                        JsonObject theme = element.getAsJsonObject();
                        if (!theme.has("name") || !theme.has("color1") || !theme.has("color2")) continue;

                        String name = theme.get("name").getAsString();
                        int c1 = theme.get("color1").getAsInt();
                        int c2 = theme.get("color2").getAsInt();

                        if (name.equals(activeName)) {
                            selected = new ThemeEntry(name, c1, c2);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
                // keep fallback theme if file is invalid
            }
        }

        ThemeManager.getInstance().getCurrentTheme().setColors(selected.color1, selected.color2);
    }

    private static ThemeEntry findByName(List<ThemeEntry> themes, String name) {
        for (ThemeEntry entry : themes) {
            if (entry.name.equals(name)) return entry;
        }
        return null;
    }

    private static List<ThemeEntry> getDefaultThemes() {
        List<ThemeEntry> themes = new ArrayList<>();
        themes.add(new ThemeEntry("Серо-белый", new Color(230, 230, 235, 255).getRGB(), new Color(255, 255, 255, 255).getRGB()));
        themes.add(new ThemeEntry("Cyber Blue", new Color(0, 200, 255, 255).getRGB(), new Color(10, 10, 20, 255).getRGB()));
        themes.add(new ThemeEntry("Violet Void", new Color(180, 0, 255, 255).getRGB(), new Color(30, 0, 40, 255).getRGB()));
        themes.add(new ThemeEntry("Abyss Blue", new Color(0, 102, 204, 255).getRGB(), new Color(10, 10, 30, 255).getRGB()));
        themes.add(new ThemeEntry("Obsidian Glow", new Color(200, 200, 255, 255).getRGB(), new Color(10, 10, 15, 230).getRGB()));
        themes.add(new ThemeEntry("Quantum Shift", new Color(100, 255, 230, 255).getRGB(), new Color(0, 20, 25, 220).getRGB()));
        themes.add(new ThemeEntry("White-Black", new Color(255, 255, 255, 255).getRGB(), new Color(0, 0, 0, 255).getRGB()));
        themes.add(new ThemeEntry("Serenity", new Color(137, 159, 255, 255).getRGB(), new Color(20, 20, 35, 255).getRGB()));
        return themes;
    }

    private record ThemeEntry(String name, int color1, int color2) {
    }
}
