package astralis.event.impl;

import astralis.event.CancellableEvent;

/** Posted for key presses and releases, before other key handling; cancelling consumes the key. */
public class KeyInputEvent extends CancellableEvent {
    private final int key;
    private final boolean pressed;
    private final boolean screenOpen;

    public KeyInputEvent(int key, boolean pressed, boolean screenOpen) {
        this.key = key;
        this.pressed = pressed;
        this.screenOpen = screenOpen;
    }

    public boolean isScreenOpen() {
        return this.screenOpen;
    }

    public int getKey() {
        return this.key;
    }

    public boolean isPressed() {
        return this.pressed;
    }
}
