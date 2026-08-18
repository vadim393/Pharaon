package tech.onetap.util.commands.defaults;

import net.minecraft.util.Formatting;
import tech.onetap.Onetap;
import tech.onetap.module.list.misc.InvSaver;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;
import tech.onetap.util.commands.api.helpers.TabCompleteHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class InvSaverCommand extends Command {

    public InvSaverCommand() {
        super("invsaver", "isaver", "wellsaver", "wsaver");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        InvSaver module = Onetap.getInstance().getModuleStorage().get(InvSaver.class);
        if (module == null) {
            logDirect("Модуль InvSaver не найден", Formatting.GRAY);
            return;
        }

        String action = args.hasAny() ? args.getString().toLowerCase(Locale.ROOT) : "show";
        switch (action) {
            case "warp", "set" -> handleSetWarp(module, args);
            case "show", "get", "status" -> handleShow(module, args);
            default -> logDirect("Неизвестная подкоманда. Используй warp/show.", Formatting.GRAY);
        }
    }

    private void handleSetWarp(InvSaver module, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String warp = normalizeWarp(args.rawRest());
        if (warp.isEmpty()) {
            logDirect("Укажи варп. Пример: invsaver warp demaz", Formatting.GRAY);
            return;
        }

        module.setWarpTarget(warp);
        logDirect("Варп для InvSaver установлен: " + Formatting.WHITE + module.getWarpTarget(), Formatting.GRAY);
    }

    private void handleShow(InvSaver module, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        logDirect("Текущий варп InvSaver: " + Formatting.WHITE + module.getWarpTarget(), Formatting.GRAY);
    }

    private static String normalizeWarp(String value) {
        if (value == null) return "";

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("warp ")) {
            normalized = normalized.substring(5).trim();
        }

        return normalized;
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny() && args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .prepend("warp", "show")
                    .filterPrefix(args.getString())
                    .sortAlphabetically()
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Настройка варпа для InvSaver";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для настройки варпа в модуле InvSaver.",
                "",
                "Использование:",
                "> invsaver show - показывает текущий варп.",
                "> invsaver warp <name> - устанавливает варп, на который телепортирует InvSaver."
        );
    }
}
