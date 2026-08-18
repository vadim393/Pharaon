package tech.onetap.util.blockesp;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BlockEspRepository {
    private static final File FILE = new File("onetap/blockesp.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() {}.getType();

    private static final LinkedHashSet<String> blocks = new LinkedHashSet<>();
    private static boolean loaded;

    private BlockEspRepository() {}

    public static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static Set<String> getBlocks() {
        ensureLoaded();
        return Collections.unmodifiableSet(blocks);
    }

    public static boolean add(String blockId) {
        ensureLoaded();
        boolean added = blocks.add(blockId);
        if (added) {
            save();
        }
        return added;
    }

    public static boolean remove(String blockId) {
        ensureLoaded();
        boolean removed = blocks.removeIf(id -> id.equalsIgnoreCase(blockId));
        if (removed) {
            save();
        }
        return removed;
    }

    public static void clear() {
        ensureLoaded();
        if (blocks.isEmpty()) return;
        blocks.clear();
        save();
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(blocks, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        loaded = true;
        if (!FILE.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
            Set<String> loadedBlocks = GSON.fromJson(reader, SET_TYPE);
            blocks.clear();
            if (loadedBlocks != null) {
                loadedBlocks.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .map(String::trim)
                        .forEach(blocks::add);
            }
        } catch (IOException ignored) {
        }
    }
}
