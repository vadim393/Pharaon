package tech.onetap.util.config;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ChangelogManager {

    private static final File FILE = new File(MinecraftClient.getInstance().runDirectory, "onetap/changelog.txt");

    private static List<String> cache = new ArrayList<>();
    private static long lastModified = -1L;

    private ChangelogManager() {
    }

    public static File getFile() {
        return FILE;
    }

    public static List<String> getLines() {
        long modified = FILE.lastModified();
        if (modified != lastModified || cache.isEmpty()) {
            lastModified = modified;
            cache = readLines();
        }
        return cache;
    }

    private static List<String> readLines() {
        List<String> lines = new ArrayList<>();
        try {
            if (!FILE.exists()) {
                FILE.getParentFile().mkdirs();
                Files.writeString(FILE.toPath(), "Changelog - напиши здесь что-нибудь\n", StandardCharsets.UTF_8);
            }
            for (String line : Files.readAllLines(FILE.toPath(), StandardCharsets.UTF_8)) {
                lines.add(line);
            }
        } catch (Exception ignored) {
        }
        return lines;
    }
}