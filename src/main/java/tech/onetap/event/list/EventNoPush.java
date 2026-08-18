package tech.onetap.event.list;

import lombok.Getter;
import tech.onetap.event.Event;

@Getter
public class EventNoPush extends Event {
    private final NoPushType noPushType;

    public EventNoPush(NoPushType noPushType) {
        this.noPushType = noPushType;
    }

    public enum NoPushType {
        Block,
        Water,
        Player,
        FishingRod
    }
}
