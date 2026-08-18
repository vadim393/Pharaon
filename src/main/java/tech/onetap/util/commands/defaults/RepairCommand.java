package tech.onetap.util.commands.defaults;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.player.other.InventoryUtil;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class RepairCommand extends Command implements IMinecraft {
    private static final int HOTBAR_SLOT = 0;
    private static final long REPAIR_DELAY_MS = 400L;
    private static final long RATE_LIMIT_DELAY_MS = 1_500L;
    private static final long SLOT_APPLY_DELAY_MS = 100L;

    private final StopWatch timer = new StopWatch();

    private boolean running;
    private long nextDelayMs = REPAIR_DELAY_MS;
    private int previousSelectedSlot = -1;
    private State state = State.IDLE;

    public RepairCommand() {
        super("repair");
        Onetap.getInstance().getEventBus().register(this);
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);

        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) {
            logDirect("You need to be in game");
            return;
        }

        if (running) {
            stop();
            logDirect("Repair stopped");
            return;
        }

        int slot = findNextDamagedSlot();
        if (slot == -1) {
            logDirect("No damaged items found");
            return;
        }

        running = true;
        nextDelayMs = REPAIR_DELAY_MS;
        previousSelectedSlot = mc.player.getInventory().selectedSlot;
        prepareSlot(slot);
        logDirect("Repair started");
    }

    @Subscribe
    private void onTick(EventTick event) {
        if (!running) return;
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) {
            stop();
            return;
        }

        switch (state) {
            case WAITING_APPLY_SLOT -> handleWaitingSlot();
            case WAITING_NEXT -> handleWaitingNext();
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (!running || event.getType() != EventPacket.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String message = packet.content().getString();
        if (message == null || message.isBlank()) return;

        if (!isRateLimitMessage(message)) return;

        nextDelayMs = RATE_LIMIT_DELAY_MS;
        timer.reset();
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Repairs damaged inventory items one by one";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Moves each damaged item into hotbar slot 1 and runs /fix all with a 400ms delay."
        );
    }

    private void prepareSlot(int slot) {
        InventoryUtil.moveInventorySlotToHotbar(slot, HOTBAR_SLOT);
        InventoryUtil.selectHotbarSlot(HOTBAR_SLOT);
        state = State.WAITING_APPLY_SLOT;
        timer.reset();
    }

    private void repairCurrentSlot() {
        mc.getNetworkHandler().sendChatCommand("fix all");
        state = State.WAITING_NEXT;
        nextDelayMs = REPAIR_DELAY_MS;
        timer.reset();
    }

    private int findNextDamagedSlot() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > 0) {
                return slot;
            }
        }

        return -1;
    }

    private void handleWaitingSlot() {
        if (!timer.isReached(SLOT_APPLY_DELAY_MS)) return;
        repairCurrentSlot();
    }

    private void handleWaitingNext() {
        if (!timer.isReached(nextDelayMs)) return;

        int slot = findNextDamagedSlot();
        if (slot == -1) {
            stop();
            logDirect("Repair finished");
            return;
        }

        prepareSlot(slot);
    }

    private boolean isRateLimitMessage(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("\u0441\u043f\u0430\u043c")
                || normalized.contains("spam")
                || normalized.contains("\u0437\u0430\u0434\u0435\u0440\u0436")
                || normalized.contains("\u043f\u043e\u0434\u043e\u0436\u0434\u0438")
                || normalized.contains("\u0441\u043b\u0438\u0448\u043a\u043e\u043c \u0431\u044b\u0441\u0442\u0440\u043e")
                || normalized.contains("cooldown")
                || normalized.contains("wait");
    }

    private void stop() {
        running = false;
        nextDelayMs = REPAIR_DELAY_MS;
        state = State.IDLE;
        timer.reset();

        if (mc.player != null && mc.interactionManager != null && previousSelectedSlot >= 0 && previousSelectedSlot < 9) {
            mc.player.getInventory().selectedSlot = previousSelectedSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSelectedSlot));
        }

        previousSelectedSlot = -1;
    }

    private enum State {
        IDLE,
        WAITING_APPLY_SLOT,
        WAITING_NEXT
    }
}
