package tech.onetap.util.chat;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.friend.Friend;
import tech.onetap.util.friend.FriendRepository;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class ChatHighlightController {
    private static final int SELF_COLOR = 0x2FA8FF;
    private static final int FRIEND_COLOR = 0x8DFF3D;
    private static final int SPEK_COLOR = 0xFF3B30;
    private static final long SENT_MESSAGE_LIFETIME_MS = 15_000L;
    private static final int MAX_SENT_MESSAGES = 8;

    private static final ArrayDeque<SentMessage> sentMessages = new ArrayDeque<>();
    private static final Map<OrderedText, HighlightType> visibleLineHighlights = Collections.synchronizedMap(new WeakHashMap<>());

    private ChatHighlightController() {
    }

    public static void rememberSentMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (sentMessages) {
            cleanupSentMessages(now);
            sentMessages.addLast(new SentMessage(normalize(message), now));
            while (sentMessages.size() > MAX_SENT_MESSAGES) {
                sentMessages.removeFirst();
            }
        }
    }

    public static void rememberVisibleLine(OrderedText orderedText, Text fullMessage) {
        if (orderedText == null || fullMessage == null) {
            return;
        }

        HighlightType highlightType = resolveHighlightType(fullMessage.getString());
        if (highlightType == null) {
            visibleLineHighlights.remove(orderedText);
            return;
        }

        visibleLineHighlights.put(orderedText, highlightType);
    }

    public static int getBackgroundColor(OrderedText orderedText, int originalColor) {
        HighlightType highlightType = visibleLineHighlights.get(orderedText);
        if (highlightType == null) {
            return originalColor;
        }

        int alpha = originalColor >>> 24;
        return alpha << 24 | highlightType.rgb;
    }

    private static HighlightType resolveHighlightType(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        if (isSpekHighlight(message)) {
            return HighlightType.SPEK;
        }

        if (isOwnMessage(message)) {
            return HighlightType.SELF;
        }

        if (containsFriendName(message)) {
            return HighlightType.FRIEND;
        }

        return null;
    }

    private static boolean isSpekHighlight(String message) {
        return isSpekTrackerEnabled()
                && (SpekTracker.findMatchingSuspect(message) != null || SpekTracker.isOwnCallout(message));
    }

    private static boolean isOwnMessage(String message) {
        String playerName = getOwnName();
        if (playerName == null || !containsPlayerName(message, playerName)) {
            return false;
        }

        String normalizedMessage = normalize(message);
        long now = System.currentTimeMillis();
        synchronized (sentMessages) {
            cleanupSentMessages(now);
            for (SentMessage sentMessage : sentMessages) {
                if (normalizedMessage.contains(sentMessage.message())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean containsFriendName(String message) {
        for (Friend friend : FriendRepository.getFriends()) {
            if (friend == null || friend.name() == null || friend.name().isBlank()) {
                continue;
            }

            if (containsPlayerName(message, friend.name())) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsPlayerName(String message, String name) {
        String lowerMessage = normalize(message);
        String lowerName = normalize(name);
        if (lowerName.isEmpty()) {
            return false;
        }

        int index = lowerMessage.indexOf(lowerName);
        while (index >= 0) {
            int leftIndex = index - 1;
            int rightIndex = index + lowerName.length();

            boolean leftOk = leftIndex < 0 || !isNameCharacter(lowerMessage.charAt(leftIndex));
            boolean rightOk = rightIndex >= lowerMessage.length() || !isNameCharacter(lowerMessage.charAt(rightIndex));
            if (leftOk && rightOk) {
                return true;
            }

            index = lowerMessage.indexOf(lowerName, index + 1);
        }

        return false;
    }

    private static boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static String getOwnName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }

        return client.player.getName().getString();
    }

    private static boolean isSpekTrackerEnabled() {
        if (Onetap.getInstance() == null || Onetap.getInstance().getModuleStorage() == null) {
            return false;
        }

        Interface interfaceModule = Onetap.getInstance().getModuleStorage().get(Interface.class);
        return interfaceModule != null
                && interfaceModule.isEnabled()
                && interfaceModule.getElementsSetting().isEnabled("СпекТрекер");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static void cleanupSentMessages(long now) {
        while (!sentMessages.isEmpty() && now - sentMessages.peekFirst().timestamp() > SENT_MESSAGE_LIFETIME_MS) {
            sentMessages.removeFirst();
        }
    }

    private enum HighlightType {
        SELF(SELF_COLOR),
        FRIEND(FRIEND_COLOR),
        SPEK(SPEK_COLOR);

        private final int rgb;

        HighlightType(int rgb) {
            this.rgb = rgb;
        }
    }

    private record SentMessage(String message, long timestamp) {
    }
}
