package tech.onetap.util.render.msdf;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FormattedTextProcessor {

    public static List<TextSegment> processText(Text text, int defaultColor) {
        List<TextSegment> segments = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                int baseColor = extractColor(style, defaultColor);
                StyleState baseState = new StyleState(
                        baseColor,
                        style.isBold(),
                        style.isItalic(),
                        style.isUnderlined(),
                        style.isStrikethrough()
                );
                appendLegacyAwareSegments(string, baseState, segments);
            }
            return Optional.empty();
        }, Style.EMPTY);

        return segments;
    }

    private static int extractColor(Style style, int defaultColor) {
        TextColor textColor = style.getColor();
        if (textColor != null) {
            return textColor.getRgb() | 0xFF000000;
        }
        return defaultColor;
    }

    private static void appendLegacyAwareSegments(String raw, StyleState baseState, List<TextSegment> out) {
        if (raw.indexOf('\u00A7') < 0) {
            out.add(new TextSegment(raw, baseState.color, baseState.bold, baseState.italic, baseState.underlined, baseState.strikethrough));
            return;
        }

        StyleState current = new StyleState(baseState);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\u00A7' && i + 1 < raw.length()) {
                if (!builder.isEmpty()) {
                    out.add(new TextSegment(
                            builder.toString(),
                            current.color,
                            current.bold,
                            current.italic,
                            current.underlined,
                            current.strikethrough
                    ));
                    builder.setLength(0);
                }

                char code = raw.charAt(++i);
                if (!applyLegacyCode(code, current, baseState)) {
                    builder.append('\u00A7').append(code);
                }
                continue;
            }

            builder.append(ch);
        }

        if (!builder.isEmpty()) {
            out.add(new TextSegment(
                    builder.toString(),
                    current.color,
                    current.bold,
                    current.italic,
                    current.underlined,
                    current.strikethrough
            ));
        }
    }

    private static boolean applyLegacyCode(char code, StyleState current, StyleState baseState) {
        char lower = Character.toLowerCase(code);
        Integer color = colorByLegacyCode(lower);
        if (color != null) {
            current.color = color;
            current.bold = false;
            current.italic = false;
            current.underlined = false;
            current.strikethrough = false;
            return true;
        }

        switch (lower) {
            case 'l' -> {
                current.bold = true;
                return true;
            }
            case 'o' -> {
                current.italic = true;
                return true;
            }
            case 'n' -> {
                current.underlined = true;
                return true;
            }
            case 'm' -> {
                current.strikethrough = true;
                return true;
            }
            case 'r' -> {
                current.color = baseState.color;
                current.bold = baseState.bold;
                current.italic = baseState.italic;
                current.underlined = baseState.underlined;
                current.strikethrough = baseState.strikethrough;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static Integer colorByLegacyCode(char code) {
        Formatting formatting = Formatting.byCode(code);
        Integer colorValue = formatting == null ? null : formatting.getColorValue();
        return colorValue == null ? null : (colorValue | 0xFF000000);
    }

    private static final class StyleState {
        private int color;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strikethrough;

        private StyleState(int color, boolean bold, boolean italic, boolean underlined, boolean strikethrough) {
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
            this.strikethrough = strikethrough;
        }

        private StyleState(StyleState other) {
            this(other.color, other.bold, other.italic, other.underlined, other.strikethrough);
        }
    }

    public record TextSegment(String text, int color, boolean bold, boolean italic, boolean underlined,
                              boolean strikethrough) {
    }
}
