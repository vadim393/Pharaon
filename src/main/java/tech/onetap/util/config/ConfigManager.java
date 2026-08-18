package tech.onetap.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.list.render.ClickGui;
import tech.onetap.module.list.misc.ScoreboardHealth;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.module.settings.ThemeSetting;
import tech.onetap.module.settings.impl.Theme;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FOLDER = Paths.get("onetap/configs");

    public static void save(String name) {
        JsonObject root = new JsonObject();

        for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("enabled", module.isEnabled());
            moduleObject.addProperty("keybind", module.getKey());

            JsonObject settingsObject = new JsonObject();
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof BindSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof ModeSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof SliderSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof StringSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof ColorSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof ThemeSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue().name);
                } else if (setting instanceof ModeListSetting s) {
                    JsonArray enabledModes = new JsonArray();
                    for (String modeName : s.getEnabledModules()) {
                        enabledModes.add(modeName);
                    }
                    settingsObject.add(setting.getName(), enabledModes);
                }
            }

            moduleObject.add("settings", settingsObject);
            root.add(module.getName(), moduleObject);
        }

        try {
            Files.createDirectories(CONFIG_FOLDER);
            Path configFile = CONFIG_FOLDER.resolve(name + ".json");
            Files.write(configFile, gson.toJson(root).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(String name) {
        Path configFile = CONFIG_FOLDER.resolve(name + ".json");
        if (!Files.exists(configFile)) return;

        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject moduleObject = root.getAsJsonObject(module.getName());

                if (moduleObject.has("enabled")) {
                    boolean moduleEnabled = moduleObject.get("enabled").getAsBoolean();
                    if (module instanceof ClickGui) {
                        module.setEnabled(false);
                    } else {
                        module.setEnabled(moduleEnabled);
                    }
                }
                if (moduleObject.has("keybind")) {
                    int keybind = moduleObject.get("keybind").getAsInt();
                    module.setKey(keybind);
                }
                if (moduleObject.has("settings")) {
                    JsonObject settingsObject = moduleObject.getAsJsonObject("settings");
                    for (Setting setting : module.getSettings()) {
                        if (!settingsObject.has(setting.getName())) continue;

                        JsonElement element = settingsObject.get(setting.getName());
                        if (setting instanceof BooleanSetting s) {
                            s.setValue(element.getAsBoolean());
                        } else if (setting instanceof BindSetting s) {
                            s.setValue(element.getAsInt());
                        } else if (setting instanceof ModeSetting s) {
                            s.setValue(element.getAsString());
                        } else if (setting instanceof SliderSetting s) {
                            s.setValue(element.getAsDouble());
                        } else if (setting instanceof StringSetting s) {
                            s.setValue(element.getAsString());
                        } else if (setting instanceof ColorSetting s) {
                            s.setValue(element.getAsInt());
                        } else if (setting instanceof ThemeSetting s) {
                            String themeName = element.getAsString();
                            for (Theme theme : s.getThemes()) {
                                if (theme.name.equals(themeName)) {
                                    s.setValue(theme);
                                    break;
                                }
                            }
                        } else if (setting instanceof ModeListSetting s && element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            List<String> enabledValues = new ArrayList<>();
                            for (JsonElement jsonElement : array) {
                                enabledValues.add(jsonElement.getAsString());
                            }
                            for (BooleanSetting subSetting : s.getSettings()) {
                                subSetting.setValue(enabledValues.contains(subSetting.getName()));
                            }
                        }
                    }
                }
            }

            migrateLegacyScoreboardHealth(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrateLegacyScoreboardHealth(JsonObject root) {
        if (!root.has("Tags")) return;

        JsonObject tagsObject = root.getAsJsonObject("Tags");
        if (tagsObject == null || !tagsObject.has("settings")) return;

        JsonObject settingsObject = tagsObject.getAsJsonObject("settings");
        if (settingsObject == null || !settingsObject.has("HpFix")) return;

        ScoreboardHealth scoreboardHealth = Onetap.getInstance().getModuleStorage().get(ScoreboardHealth.class);
        if (scoreboardHealth == null) return;

        scoreboardHealth.setEnabled(settingsObject.get("HpFix").getAsBoolean());
    }

    public static List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        try {
            if (Files.exists(CONFIG_FOLDER)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_FOLDER, "*.json")) {
                    for (Path path : stream) {
                        String fileName = path.getFileName().toString();
                        if (fileName.endsWith(".json")) {
                            configs.add(fileName.substring(0, fileName.length() - 5));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        configs.sort(String.CASE_INSENSITIVE_ORDER);
        return configs;
    }

    public static boolean exists(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) return false;
        return Files.exists(CONFIG_FOLDER.resolve(normalized + ".json"));
    }

    public static boolean delete(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) return false;

        Path configFile = CONFIG_FOLDER.resolve(normalized + ".json");
        if (!Files.exists(configFile)) return false;

        try {
            Files.delete(configFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean rename(String oldName, String newName) {
        String from = normalizeName(oldName);
        String to = normalizeName(newName);
        if (from.isEmpty() || to.isEmpty()) return false;
        if (from.equalsIgnoreCase(to)) return true;

        Path fromFile = CONFIG_FOLDER.resolve(from + ".json");
        Path toFile = CONFIG_FOLDER.resolve(to + ".json");
        if (!Files.exists(fromFile) || Files.exists(toFile)) return false;

        try {
            Files.createDirectories(CONFIG_FOLDER);
            Files.move(fromFile, toFile, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException ignored) {
            try {
                Files.move(fromFile, toFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static String normalizeName(String value) {
        if (value == null) return "";
        String normalized = value.trim().replaceAll("\\s+", "_");
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]", "");
        normalized = normalized.replaceAll("^\\.+", "");
        normalized = normalized.replaceAll("\\.+$", "");
        if (normalized.length() > 40) normalized = normalized.substring(0, 40);
        return normalized;
    }
}
