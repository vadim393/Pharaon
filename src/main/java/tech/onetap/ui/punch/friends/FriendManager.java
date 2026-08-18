package tech.onetap.ui.punch.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tech.onetap.util.friend.FriendRepository;

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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FriendManager {
    public static final FriendManager INSTANCE = new FriendManager();

    public static final class FriendEntry {
        private String name;
        private boolean pinned;
        private long lastSeen;

        public FriendEntry(String name) {
            this.name = name;
        }

        public String name() {
            return this.name;
        }

        public boolean pinned() {
            return this.pinned;
        }

        public long lastSeen() {
            return this.lastSeen;
        }
    }

    private static final File FILE = new File("onetap/punch_friends.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<FriendEntry> entries = new ArrayList<>();
    private boolean loaded;

    private FriendManager() {
    }

    public static void init() {
        INSTANCE.load();
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z0-9_]{3,16}");
    }

    public boolean add(String name) {
        ensureLoaded();
        if (!isValidName(name) || contains(name)) {
            return false;
        }
        this.entries.add(new FriendEntry(name));
        FriendRepository.addFriend(name);
        save();
        return true;
    }

    public void remove(String name) {
        ensureLoaded();
        this.entries.removeIf(entry -> entry.name().equalsIgnoreCase(name));
        FriendRepository.removeFriend(name);
        save();
    }

    public void togglePinned(String name) {
        ensureLoaded();
        for (FriendEntry entry : this.entries) {
            if (entry.name().equalsIgnoreCase(name)) {
                entry.pinned = !entry.pinned;
                save();
                return;
            }
        }
    }

    public void markSeen(String name) {
        ensureLoaded();
        for (FriendEntry entry : this.entries) {
            if (entry.name().equalsIgnoreCase(name)) {
                if (entry.lastSeen != System.currentTimeMillis()) {
                    entry.lastSeen = System.currentTimeMillis();
                }
                return;
            }
        }
    }

    public boolean isFriend(String name) {
        ensureLoaded();
        return contains(name);
    }

    public List<String> getFriends() {
        ensureLoaded();
        List<String> names = new ArrayList<>();
        for (FriendEntry entry : this.entries) {
            names.add(entry.name());
        }
        return names;
    }

    public List<FriendEntry> getEntries() {
        ensureLoaded();
        List<FriendEntry> snapshot = new ArrayList<>(this.entries);
        snapshot.sort(Comparator
                .comparing(FriendEntry::pinned).reversed()
                .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return snapshot;
    }

    public void load() {
        this.entries.clear();
        if (FILE.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                Type listType = new com.google.common.reflect.TypeToken<ArrayList<FriendEntry>>() {
                }.getType();
                List<FriendEntry> loadedEntries = GSON.fromJson(reader, listType);
                if (loadedEntries != null) {
                    for (FriendEntry entry : loadedEntries) {
                        if (entry != null && entry.name != null && isValidName(entry.name) && !contains(entry.name)) {
                            this.entries.add(entry);
                        }
                    }
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }

        for (tech.onetap.util.friend.Friend friend : FriendRepository.getFriends()) {
            if (!contains(friend.name())) {
                this.entries.add(new FriendEntry(friend.name()));
            }
        }
        this.loaded = true;
        save();
    }

    private boolean contains(String name) {
        for (FriendEntry entry : this.entries) {
            if (entry.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            load();
        }
    }

    private void save() {
        try {
            FILE.getParentFile().mkdirs();
            List<FriendEntry> snapshot;
            snapshot = new ArrayList<>(this.entries);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
        } catch (IOException ignored) {
        }
    }
}