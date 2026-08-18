package tech.onetap.util.friend;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.util.QuickLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class FriendRepository implements QuickLogger {

    private static final File file = new File("onetap/friends.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Object lock = new Object();
    private static final List<Friend> friends = new ArrayList<>();

    public static List<Friend> getFriends() {
        synchronized (lock) {
            sanitizeInPlace();
            return new ArrayList<>(friends);
        }
    }

    public static void addFriend(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null || isFriend(normalizedName)) {
            return;
        }

        synchronized (lock) {
            friends.add(new Friend(normalizedName));
        }
    }

    public static void removeFriend(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return;
        }

        synchronized (lock) {
            sanitizeInPlace();
            friends.removeIf(friend -> normalizedName.equalsIgnoreCase(friendName(friend)));
        }
    }

    public static boolean shouldAttack(PlayerEntity player) {
        return player != null && !isFriend(player.getNameForScoreboard());
    }

    public static boolean isFriend(String friend) {
        String normalizedName = normalizeName(friend);
        if (normalizedName == null) {
            return false;
        }

        return getFriends().stream().anyMatch(f -> normalizedName.equalsIgnoreCase(friendName(f)));
    }

    public static Friend getFriend(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }

        return getFriends().stream()
                .filter(friend -> normalizedName.equalsIgnoreCase(friendName(friend)))
                .findFirst()
                .orElse(null);
    }

    public static void clear() {
        synchronized (lock) {
            friends.clear();
        }
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(getFriends(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        if (!file.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Friend>>() {}.getType();
            List<Friend> loaded = gson.fromJson(reader, listType);
            synchronized (lock) {
                friends.clear();
                friends.addAll(sanitizeFriends(loaded));
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static void sanitizeInPlace() {
        List<Friend> sanitized = sanitizeFriends(friends);
        friends.clear();
        friends.addAll(sanitized);
    }

    private static List<Friend> sanitizeFriends(List<Friend> source) {
        List<Friend> sanitized = new ArrayList<>();
        if (source == null) {
            return sanitized;
        }

        Set<String> seen = new HashSet<>();
        for (Friend friend : source) {
            String name = friendName(friend);
            if (name == null) {
                continue;
            }

            String loweredName = name.toLowerCase(Locale.ROOT);
            if (!seen.add(loweredName)) {
                continue;
            }

            sanitized.add(new Friend(name));
        }

        return sanitized;
    }

    private static String friendName(Friend friend) {
        if (friend == null) {
            return null;
        }

        return normalizeName(friend.name());
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String normalizedName = name.trim();
        return normalizedName.isEmpty() ? null : normalizedName;
    }
}
