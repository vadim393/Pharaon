package tech.onetap.util.alt;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import tech.onetap.mixin.IMinecraftClientAccessor;
import tech.onetap.util.bot.BotSessionManager;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AltManager {
    private static final File file = new File("onetap/alts.json");
    private static final File currentFile = new File("onetap/current_alt.txt");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Object lock = new Object();
    private static final List<Alt> alts = new ArrayList<>();
    private static boolean loaded = false;

    private static final String[] SYLLABLES_START = {
            "Al", "Ba", "Be", "Bl", "Bo", "Ca", "Ch", "Cl", "Co", "Cr", "De", "Dr",
            "El", "Em", "En", "Fa", "Fe", "Fi", "Fl", "Fo", "Fr", "Ga", "Gl", "Go",
            "Gr", "Ha", "He", "Ho", "Il", "In", "Is", "Ja", "Jo", "Ka", "Ke", "Ki",
            "La", "Le", "Li", "Lo", "Lu", "Ma", "Me", "Mi", "Mo", "Mu", "Na", "Ne",
            "Ni", "No", "Ol", "On", "Op", "Or", "Os", "Pa", "Pe", "Pi", "Po", "Pr",
            "Qu", "Ra", "Re", "Ri", "Ro", "Ru", "Sa", "Se", "Sh", "Si", "So", "St",
            "Ta", "Te", "Th", "Ti", "To", "Tr", "Ul", "Um", "Un", "Ur", "Va", "Ve",
            "Vi", "Vo", "Wa", "We", "Wi", "Wo", "Xa", "Za", "Ze"
    };

    private static final String[] SYLLABLES_END = {
            "a", "an", "ar", "ax", "bo", "by", "ca", "ce", "ck", "da", "de", "do",
            "dy", "er", "ex", "ey", "go", "ia", "ic", "in", "is", "ix", "ka", "ko",
            "la", "le", "li", "lon", "ly", "ma", "me", "mi", "mo", "my", "na", "ne",
            "ni", "no", "ny", "ok", "ol", "on", "or", "os", "ot", "ox", "ra", "re",
            "ri", "ro", "ry", "sa", "se", "si", "so", "sy", "ta", "te", "ti", "to",
            "ty", "un", "us", "ux", "va", "ve", "vi", "vy", "wa", "we", "wo", "ya",
            "ye", "yo", "za", "ze"
    };

    public static List<Alt> getAlts() {
        ensureLoaded();
        synchronized (lock) {
            List<Alt> snapshot = new ArrayList<>(alts);
            snapshot.sort((a, b) -> Boolean.compare(b.pinned(), a.pinned()));
            return snapshot;
        }
    }

    public static boolean addAlt(String name) {
        String normalized = sanitizeName(name);
        if (!isValidName(normalized)) {
            return false;
        }

        synchronized (lock) {
            ensureLoaded();
            for (Alt alt : alts) {
                if (alt.name().equalsIgnoreCase(normalized)) {
                    return false;
                }
            }
            alts.add(new Alt(normalized, UUID.randomUUID().toString(), false));
        }
        save();
        return true;
    }

    public static boolean removeAlt(String name) {
        if (name == null) {
            return false;
        }
        synchronized (lock) {
            ensureLoaded();
            boolean removed = alts.removeIf(alt -> alt.name().equalsIgnoreCase(name));
            if (removed) {
                save();
            }
            return removed;
        }
    }

    public static boolean setPinned(String name, boolean pinned) {
        if (name == null) {
            return false;
        }
        synchronized (lock) {
            ensureLoaded();
            for (int i = 0; i < alts.size(); i++) {
                if (alts.get(i).name().equalsIgnoreCase(name)) {
                    alts.set(i, alts.get(i).withPinned(pinned));
                    save();
                    return true;
                }
            }
        }
        return false;
    }

    public static void login(Alt alt) {
        if (alt == null || alt.name() == null || alt.name().isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Session current = mc.getSession();
        ((IMinecraftClientAccessor) mc).setSession(BotSessionManager.createSessionWithName(current, alt.name()));
        saveCurrentAlt(alt.name());
    }

    public static void applyPersistedAlt() {
        ensureLoaded();
        String name = loadCurrentAlt();
        if (name == null || name.isEmpty()) {
            return;
        }
        synchronized (lock) {
            for (Alt alt : alts) {
                if (alt.name().equalsIgnoreCase(name)) {
                    login(alt);
                    return;
                }
            }
        }
    }

    public static String generateRandomName(Random random) {
        String name;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder();
            int syllables = 2 + random.nextInt(2);
            for (int i = 0; i < syllables; i++) {
                sb.append(SYLLABLES_START[random.nextInt(SYLLABLES_START.length)]);
                if (i < syllables - 1 && random.nextBoolean()) {
                    sb.append(SYLLABLES_END[random.nextInt(SYLLABLES_END.length)]);
                }
            }
            if (sb.length() > 16) {
                sb.setLength(16);
            }
            name = sb.toString();
            attempts++;
        } while (!isValidName(name) && attempts < 50);

        if (!isValidName(name)) {
            name = generateFallbackName(random);
        }
        return name;
    }

    public static String sanitizeName(String input) {
        if (input == null) {
            return "";
        }
        String cleaned = input.trim().replaceAll("[^a-zA-Z0-9_]", "");
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z0-9_]{3,16}");
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            List<Alt> snapshot;
            synchronized (lock) {
                snapshot = new ArrayList<>(alts);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(snapshot, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static void saveCurrentAlt(String name) {
        try {
            currentFile.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8)) {
                writer.write(name);
            }
        } catch (IOException ignored) {
        }
    }

    private static String loadCurrentAlt() {
        if (!currentFile.exists()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(currentFile), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            String name = sb.toString().trim();
            return isValidName(name) ? name : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static void load() {
        if (!file.exists()) {
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Alt>>() {}.getType();
            List<Alt> loadedList = gson.fromJson(reader, listType);
            synchronized (lock) {
                alts.clear();
                if (loadedList != null) {
                    for (Alt alt : loadedList) {
                        if (alt != null && isValidName(alt.name())) {
                            alts.add(new Alt(alt.name(), alt.uuid() == null ? UUID.randomUUID().toString() : alt.uuid(), alt.pinned()));
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static String generateFallbackName(Random random) {
        int length = 3 + random.nextInt(8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (lock) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }
}
