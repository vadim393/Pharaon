package tech.onetap.util.chat;

import java.util.Locale;
import java.util.Set;

public final class ChatPrivacyController {
    private static final Set<String> SENSITIVE_COMMANDS = Set.of(
            "/reg",
            "/register",
            "/login",
            "/l",
            "/r"
    );

    private static volatile boolean hideSensitiveInfo;

    private ChatPrivacyController() {
    }

    public static boolean isHideSensitiveInfo() {
        return hideSensitiveInfo;
    }

    public static void toggle() {
        hideSensitiveInfo = !hideSensitiveInfo;
    }

    public static String maskSensitiveArguments(String text) {
        ProtectedSegment segment = findProtectedSegment(text);
        if (!hideSensitiveInfo || segment == null) {
            return text;
        }

        StringBuilder masked = new StringBuilder(text);
        for (int i = segment.argumentStartIndex(); i < masked.length(); i++) {
            char ch = masked.charAt(i);
            if (!Character.isWhitespace(ch)) {
                masked.setCharAt(i, '*');
            }
        }
        return masked.toString();
    }

    public static ProtectedSegment findProtectedSegment(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int commandStart = 0;
        while (commandStart < text.length() && Character.isWhitespace(text.charAt(commandStart))) {
            commandStart++;
        }
        if (commandStart >= text.length() || text.charAt(commandStart) != '/') {
            return null;
        }

        int commandEnd = commandStart;
        while (commandEnd < text.length() && !Character.isWhitespace(text.charAt(commandEnd))) {
            commandEnd++;
        }

        String command = text.substring(commandStart, commandEnd).toLowerCase(Locale.ROOT);
        if (!SENSITIVE_COMMANDS.contains(command)) {
            return null;
        }

        int argumentStart = commandEnd;
        while (argumentStart < text.length() && Character.isWhitespace(text.charAt(argumentStart))) {
            argumentStart++;
        }

        if (argumentStart >= text.length()) {
            return null;
        }

        return new ProtectedSegment(commandStart, commandEnd, argumentStart);
    }

    public record ProtectedSegment(int commandStartIndex, int commandEndIndex, int argumentStartIndex) {
    }
}
