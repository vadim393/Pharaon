package tech.onetap.util.commands.defaults;

import net.minecraft.registry.Registries;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.BlockESP;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;
import tech.onetap.util.commands.api.helpers.Paginator;
import tech.onetap.util.commands.api.helpers.TabCompleteHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static tech.onetap.util.commands.api.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class BlockEspCommand extends Command {

    public BlockEspCommand() {
        super("blockesp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        BlockESP module = Onetap.getInstance().getModuleStorage().get(BlockESP.class);
        if (module == null) {
            logDirect("Модуль BlockESP не найден", Formatting.GRAY);
            return;
        }

        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add" -> handleAdd(module, args);
            case "remove" -> handleRemove(module, args);
            case "list" -> handleList(module, args, label);
            case "clear" -> handleClear(module, args);
            default -> logDirect("Неизвестная подкоманда. Используй add/remove/list/clear.", Formatting.GRAY);
        }
    }

    private void handleAdd(BlockESP module, IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        Identifier id = BlockESP.normalizeBlockId(args.getString());
        if (id == null || !Registries.BLOCK.containsId(id)) {
            logDirect("Такой блок не найден", Formatting.GRAY);
            return;
        }

        if (module.hasBlock(id)) {
            logDirect("Этот блок уже добавлен", Formatting.GRAY);
            return;
        }

        module.addBlock(id);
        logDirect("Блок " + Formatting.WHITE + BlockESP.toUserBlockName(id.toString()) + Formatting.GRAY + " добавлен в BlockESP");
    }

    private void handleRemove(BlockESP module, IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        Identifier id = BlockESP.normalizeBlockId(args.getString());
        if (id == null || !Registries.BLOCK.containsId(id)) {
            logDirect("Такой блок не найден", Formatting.GRAY);
            return;
        }

        if (!module.removeBlock(id)) {
            logDirect("Этот блок не был добавлен в BlockESP", Formatting.GRAY);
            return;
        }

        logDirect("Блок " + Formatting.WHITE + BlockESP.toUserBlockName(id.toString()) + Formatting.GRAY + " удалён из BlockESP");
    }

    private void handleList(BlockESP module, IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);

        List<String> blocks = module.getTrackedBlocks();
        if (blocks.isEmpty()) {
            logDirect("Список блоков BlockESP пуст", Formatting.GRAY);
            return;
        }

        Paginator.paginate(
                args,
                new Paginator<>(blocks),
                () -> logDirect("Блоки BlockESP:", Formatting.GRAY),
                blockId -> {
                    boolean enabled = module.isTrackedBlockEnabled(blockId);
                    String state = enabled ? Formatting.GREEN + "[ON]" : Formatting.RED + "[OFF]";

                    Text nameText = Text.literal(Formatting.GRAY + "- " + Formatting.WHITE + BlockESP.toUserBlockName(blockId) + " " + state + " ");
                    Text removeText = Text.literal(Formatting.RED + "[Удалить]")
                            .styled(style -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    FORCE_COMMAND_PREFIX + "blockesp remove " + blockId
                            )).withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to remove block")
                            )));
                    return nameText.copy().append(removeText);
                },
                FORCE_COMMAND_PREFIX + label
        );
    }

    private void handleClear(BlockESP module, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        module.clearBlocks();
        logDirect("Список блоков BlockESP очищен", Formatting.GRAY);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny() && args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .prepend("add", "remove", "list", "clear")
                    .filterPrefix(args.getString())
                    .sortAlphabetically()
                    .stream();
        } else if (args.hasAny()) {
            BlockESP module = Onetap.getInstance().getModuleStorage().get(BlockESP.class);
            String action = args.peekString(0).toLowerCase(Locale.ROOT);

            if (action.equals("add") && args.hasExactly(2)) {
                String prefix = args.peekString(1).toLowerCase(Locale.ROOT);
                return Registries.BLOCK.getIds().stream()
                        .map(id -> "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString())
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .distinct()
                        .sorted();
            }

            if (action.equals("remove") && args.hasExactly(2) && module != null) {
                String prefix = args.peekString(1).toLowerCase(Locale.ROOT);
                return module.getTrackedBlocks().stream()
                        .map(BlockESP::toUserBlockName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .distinct()
                        .sorted();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление списком блоков для BlockESP";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления блоками в модуле BlockESP.",
                "",
                "Использование:",
                "> blockesp add <block> - Добавляет блок (например diamond_ore).",
                "> blockesp remove <block> - Удаляет блок из списка.",
                "> blockesp list - Показывает список добавленных блоков.",
                "> blockesp clear - Полностью очищает список."
        );
    }
}
