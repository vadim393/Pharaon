package tech.onetap.ui.punch.config;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class ConfigIO {
    private static final File ROOT = new File("onetap/punch/menu");

    private ConfigIO() {
    }

    public static Path resolve(String fileName) {
        return ROOT.toPath().resolve(fileName);
    }

    public static JsonObject read(Path path) {
        File file = path.toFile();
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return null;
    }

    public static void write(Path path, JsonObject data) {
        if (data == null) {
            return;
        }
        File file = path.toFile();
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            gson.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }
}