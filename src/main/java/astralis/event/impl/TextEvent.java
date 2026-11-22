package astralis.event.impl;

import astralis.event.Event;

/** Event carrying a single string. Not posted or handled in this build. */
public record TextEvent(String text) implements Event {
    public String getText() {
        return this.text;
    }
}
