package astralis.event.impl;

import astralis.event.Event;

/**
 * Posted for key presses while no screen is open, after {@link KeyInputEvent} was not cancelled.
 */
public record InGameKeyPressEvent(int key) implements Event {
    public int getKey() {
        return this.key;
    }
}
