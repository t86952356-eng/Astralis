package astralis.event.impl;

import astralis.event.Event;

/**
 * Posted at the start of {@code RenderSystem.pollEvents}. Inventory modules set the handled flag
 * after acting so only one acts per poll.
 */
public class InputPollEvent implements Event {
    private boolean handled;

    public InputPollEvent(boolean handled) {
        this.handled = handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public boolean isHandled() {
        return this.handled;
    }
}
