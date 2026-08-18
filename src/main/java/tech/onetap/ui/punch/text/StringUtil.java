package tech.onetap.ui.punch.text;

import java.util.List;

public final class StringUtil {
    private StringUtil() {
    }

    public static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 1) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 1) + "\u2026";
    }

    public static String joinLimited(List<String> values, int maxItems) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() <= maxItems) {
            return String.join(", ", values);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxItems; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        builder.append(", +").append(values.size() - maxItems);
        return builder.toString();
    }
}