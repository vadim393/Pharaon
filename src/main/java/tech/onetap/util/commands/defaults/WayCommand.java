package tech.onetap.util.commands.defaults;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;
import tech.onetap.util.commands.api.helpers.Paginator;
import tech.onetap.util.commands.api.helpers.TabCompleteHelper;
import tech.onetap.util.way.WayRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static tech.onetap.util.IMinecraft.mc;
import static tech.onetap.util.commands.api.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class WayCommand extends Command {
    private final WayRenderer wayRenderer;

    public WayCommand() {
        super("way");
        wayRenderer = WayRenderer.get();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add" -> handleAdd(args);
            case "remove" -> handleRemove(args);
            case "list" -> handleList(args, label);
            case "clear" -> handleClear(args);
            default -> logDirect("Use: add / remove / list / clear", Formatting.GRAY);
        }
    }

    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireExactly(4);

        String name = args.getString().trim();
        if (name.isEmpty()) {
            logDirect("Waypoint name can't be empty", Formatting.GRAY);
            return;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args.getString());
            y = Double.parseDouble(args.getString());
            z = Double.parseDouble(args.getString());
        } catch (NumberFormatException ex) {
            logDirect("Coordinates must be numbers", Formatting.GRAY);
            return;
        }

        WayRenderer.AddResult result = wayRenderer.addWaypoint(name, x, y, z);
        if (result == WayRenderer.AddResult.UPDATED) {
            logDirect(Formatting.GRAY + "Waypoint " + Formatting.WHITE + name + Formatting.GRAY + " updated: "
                    + Formatting.WHITE + formatCoords(x, y, z));
        } else {
            logDirect(Formatting.GRAY + "Waypoint " + Formatting.WHITE + name + Formatting.GRAY + " added: "
                    + Formatting.WHITE + formatCoords(x, y, z));
        }
    }

    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireExactly(1);

        String name = args.getString().trim();
        if (name.isEmpty()) {
            logDirect("Waypoint name can't be empty", Formatting.GRAY);
            return;
        }

        if (!wayRenderer.removeWaypoint(name)) {
            logDirect("Waypoint not found", Formatting.GRAY);
            return;
        }

        logDirect(Formatting.GRAY + "Waypoint " + Formatting.WHITE + name + Formatting.GRAY + " removed");
    }

    private void handleList(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);

        List<WayRenderer.Waypoint> waypoints = wayRenderer.getWaypoints();
        if (waypoints.isEmpty()) {
            logDirect("Waypoint list is empty", Formatting.GRAY);
            return;
        }

        Paginator.paginate(
                args,
                new Paginator<>(waypoints),
                () -> logDirect("Waypoints:", Formatting.GRAY),
                waypoint -> {
                    String coords = formatCoords(waypoint.x(), waypoint.y(), waypoint.z());
                    Text base = Text.literal(Formatting.GRAY + "- " + Formatting.WHITE + waypoint.name()
                            + Formatting.DARK_GRAY + " -> " + Formatting.GRAY + coords + " ");
                    Text remove = Text.literal(Formatting.RED + "[Remove]")
                            .styled(style -> style.withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND,
                                            FORCE_COMMAND_PREFIX + "way remove " + waypoint.name()
                                    ))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("Click to remove waypoint")
                                    )));
                    return base.copy().append(remove);
                },
                FORCE_COMMAND_PREFIX + label
        );
    }

    private void handleClear(IArgConsumer args) throws CommandException {
        args.requireMax(1);

        int count = wayRenderer.getWaypoints().size();
        wayRenderer.clearWaypoints();
        logDirect(Formatting.GRAY + "Cleared waypoints: " + Formatting.WHITE + count);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny() && args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .prepend("add", "remove", "list", "clear")
                    .filterPrefix(args.getString())
                    .sortAlphabetically()
                    .stream();
        }

        if (!args.hasAny()) {
            return Stream.empty();
        }

        String action = args.peekString(0).toLowerCase(Locale.ROOT);
        if (action.equals("remove") && args.hasExactly(2)) {
            String prefix = args.peekString(1).toLowerCase(Locale.ROOT);
            return wayRenderer.getWaypoints().stream()
                    .map(WayRenderer.Waypoint::name)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER);
        }

        if (action.equals("add")) {
            if (args.hasExactly(3) && mc.player != null) {
                return Stream.of(String.valueOf((int) mc.player.getX()));
            }
            if (args.hasExactly(4) && mc.player != null) {
                return Stream.of(String.valueOf((int) mc.player.getY()));
            }
            if (args.hasExactly(5) && mc.player != null) {
                return Stream.of(String.valueOf((int) mc.player.getZ()));
            }
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Manage world waypoints";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Creates map markers in the world.",
                "",
                "Usage:",
                "> way add <name> <x> <y> <z> - adds or updates a waypoint.",
                "> way remove <name> - удалить веи.",
                "> way list - показать все веи.",
                "> way clear - удалить все веи."
        );
    }

    private static String formatCoords(double x, double y, double z) {
        return (int) Math.round(x) + " " + (int) Math.round(y) + " " + (int) Math.round(z);
    }
}
