package astralis.event;

/** An event whose default action can be cancelled. */
public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
