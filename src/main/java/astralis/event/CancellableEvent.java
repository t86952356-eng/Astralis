package astralis.event;

public abstract class CancellableEvent implements Event, Cancellable {
    private boolean cancelled;

    protected CancellableEvent() {}

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
