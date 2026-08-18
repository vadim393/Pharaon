package tech.onetap.util.alt;

public record Alt(String name, String uuid, boolean pinned) {
    public Alt withPinned(boolean pinned) {
        return new Alt(this.name, this.uuid, pinned);
    }
}