package tech.onetap.util.commands.defaults;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ParseCommand extends Command {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter HEADER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_PREFIX_LABEL = "NoPrefix";

    public ParseCommand() {
        super("parse", "tabparse");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null) {
            logDirect("Join a server before using this command.", Formatting.GRAY);
            return;
        }

        String fileBaseName = args.hasAny() ? sanitizeFileName(args.getString()) : defaultFileName();
        if (fileBaseName.isBlank()) {
            fileBaseName = defaultFileName();
        }

        List<PlayerListEntry> entries = new ArrayList<>(client.getNetworkHandler().getPlayerList());

        List<String> lines = new ArrayList<>();
        lines.add("# Tab parse");
        lines.add("# Generated: " + LocalDateTime.now().format(HEADER_TIME_FORMATTER));
        lines.add("# Players: " + entries.size());
        lines.add("");

        for (PlayerListEntry entry : entries) {
            ParsedTabEntry parsed = parseEntry(entry);
            StringBuilder line = new StringBuilder()
                    .append(parsed.prefix())
                    .append(" - ")
                    .append(parsed.nick());
            if (!parsed.suffix().isBlank()) {
                line.append(" - ").append(parsed.suffix());
            }
            lines.add(line.toString());
        }

        Path dir = Path.of("onetap", "parses");
        Path output = dir.resolve(fileBaseName + ".txt");

        try {
            Files.createDirectories(dir);
            Files.write(output, lines, StandardCharsets.UTF_8);
            logDirect("Saved tab list to: " + Formatting.WHITE + output.toAbsolutePath(), Formatting.GRAY);
        } catch (IOException e) {
            logDirect("Failed to write file: " + Formatting.WHITE + e.getMessage(), Formatting.GRAY);
        }
    }

    private static ParsedTabEntry parseEntry(PlayerListEntry entry) {
        String nick = entry.getProfile().getName();
        Text displayName = entry.getDisplayName();
        String rawDisplay = displayName == null ? nick : displayName.getString();

        int nickPos = lastIndexOfIgnoreCase(rawDisplay, nick);
        if (nickPos < 0) {
            String fallbackSuffix = rawDisplay.equalsIgnoreCase(nick) ? "" : rawDisplay;
            return new ParsedTabEntry(DEFAULT_PREFIX_LABEL, nick, fallbackSuffix.trim());
        }

        String prefix = rawDisplay.substring(0, nickPos).trim();
        int suffixStart = Math.min(rawDisplay.length(), nickPos + nick.length());
        String suffix = rawDisplay.substring(suffixStart).trim();
        if (prefix.isBlank()) {
            prefix = DEFAULT_PREFIX_LABEL;
        }
        return new ParsedTabEntry(prefix, nick, suffix);
    }

    private static int lastIndexOfIgnoreCase(String source, String part) {
        return source.toLowerCase(Locale.ROOT).lastIndexOf(part.toLowerCase(Locale.ROOT));
    }

    private static String defaultFileName() {
        return "tab_" + LocalDateTime.now().format(FILE_TIME_FORMATTER);
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Saves the current tab list to a text file";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Exports players from the current tab list into a text file.",
                "",
                "Usage:",
                "> parse - creates onetap/parses/tab_<date>.txt",
                "> parse <file_name> - creates onetap/parses/<file_name>.txt",
                "",
                "Each line format:",
                "> prefix - nick - suffix"
        );
    }

    private record ParsedTabEntry(String prefix, String nick, String suffix) {
    }
}
