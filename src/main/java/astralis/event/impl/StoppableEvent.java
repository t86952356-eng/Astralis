package astralis.event.impl;

import astralis.event.Event;

/**
 * Base for events whose dispatch can be stopped; the event bus skips remaining handlers once
 * stopped.
 */
public abstract class StoppableEvent implements Event {
    private boolean stopped;

    protected StoppableEvent() {}

    public void stop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
