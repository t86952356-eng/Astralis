package astralis.event.impl;

import astralis.event.Event;

/** Abstract event with a one-way marker flag. Not extended or referenced in this build. */
public abstract class MarkableEvent implements Event {
    private boolean marked;

    protected MarkableEvent() {}

    public void mark() {
        this.marked = true;
    }

    public boolean isMarked() {
        return this.marked;
    }
}
