package tech.onetap.util.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.util.base.Instance;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpekTracker {
    private static final long SUSPECT_LIFETIME_MS = 30_000L;
    private static final List<String> TRIGGER_KEYWORDS = Arrays.asList(
            "спек",
            "spec",
            "spek",
            "наблюд",
            "report",
            "репорт",
            "soft",
            "софт"
    );
    private static final List<String> OWN_CALLOUT_KEYWORDS = TRIGGER_KEYWORDS;

    private static final Map<String, Long> suspects = new ConcurrentHashMap<>();

    private SpekTracker() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            suspects.clear();
            return;
        }

        long now = System.currentTimeMillis();
        suspects.entrySet().removeIf(entry -> now - entry.getValue() > SUSPECT_LIFETIME_MS);

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (!shouldTrackPlayer(client, player)) {
                continue;
            }

            suspects.put(player.getName().getString(), now);
        }

        KillAura killAura = Instance.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            suspects.put(killAura.getTarget().getName().getString(), now);
        }
    }

    public static boolean isTriggerMessage(String message) {
        return containsKeyword(message, TRIGGER_KEYWORDS);
    }

    @Nullable
    public static String findTriggerKeyword(String message) {
        return findKeyword(message, TRIGGER_KEYWORDS);
    }

    @Nullable
    public static String findMatchingSuspect(String message) {
        if (!isTriggerMessage(message)) {
            return null;
        }

        String normalizedMessage = normalize(message);
        String bestMatch = null;
        long bestSeenAt = Long.MIN_VALUE;

        for (Map.Entry<String, Long> entry : suspects.entrySet()) {
            if (!containsPlayerName(normalizedMessage, normalize(entry.getKey()))) {
                continue;
            }

            if (entry.getValue() > bestSeenAt) {
                bestSeenAt = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        return bestMatch;
    }

    public static boolean isOwnCallout(String message) {
        if (findKeyword(message, OWN_CALLOUT_KEYWORDS) == null) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        return containsPlayerName(normalize(message), normalize(client.player.getName().getString()));
    }

    private static boolean shouldTrackPlayer(MinecraftClient client, AbstractClientPlayerEntity player) {
        if (player == null || client.player == null || player == client.player) {
            return false;
        }
        return !player.isRemoved() && !player.isSpectator();
    }

    private static boolean containsKeyword(String message, List<String> keywords) {
        return findKeyword(message, keywords) != null;
    }

    @Nullable
    private static String findKeyword(String message, List<String> keywords) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String lower = normalize(message);
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private static boolean containsPlayerName(String message, String name) {
        if (message == null || name == null || name.isEmpty()) {
            return false;
        }

        int index = message.indexOf(name);
        while (index >= 0) {
            int leftIndex = index - 1;
            int rightIndex = index + name.length();

            boolean leftOk = leftIndex < 0 || !isNameCharacter(message.charAt(leftIndex));
            boolean rightOk = rightIndex >= message.length() || !isNameCharacter(message.charAt(rightIndex));
            if (leftOk && rightOk) {
                return true;
            }

            index = message.indexOf(name, index + 1);
        }

        return false;
    }

    private static boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
