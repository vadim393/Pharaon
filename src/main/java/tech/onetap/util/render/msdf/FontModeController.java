package tech.onetap.util.render.msdf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FontModeController {

    private static final Path FILE = Paths.get("onetap", "fontmode.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile FontMode mode = FontMode.DEFAULT;

    static {
        load();
    }

    private FontModeController() {
    }

    public static FontMode getMode() {
        return mode;
    }

    public static boolean isDefault() {
        return mode == FontMode.DEFAULT;
    }

    public static boolean isCustom() {
        return mode == FontMode.CUSTOM;
    }

    public static void setMode(FontMode newMode) {
        if (newMode == null || newMode == mode) {
            return;
        }
        mode = newMode;
        save();
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) {
                JsonObject json = GSON.fromJson(Files.newBufferedReader(FILE), JsonObject.class);
                if (json != null && json.has("fontMode")) {
                    FontMode loaded = FontMode.valueOf(json.get("fontMode").getAsString());
                    mode = loaded;
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
            mode = FontMode.DEFAULT;
        }
    }

    private static void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("fontMode", mode.name());
            if (FILE.getParent() != null) {
                Files.createDirectories(FILE.getParent());
            }
            Files.write(FILE, GSON.toJson(json).getBytes());
        } catch (IOException ignored) {
        }
    }
}