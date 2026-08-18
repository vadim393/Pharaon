package tech.onetap.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.Event;

@Getter
@AllArgsConstructor
public class EventSlotClick extends Event {
    private final Slot slot;
    private final int slotId;
    private final int button;
    private final SlotActionType actionType;
    @Setter
    private boolean cancelled;
}