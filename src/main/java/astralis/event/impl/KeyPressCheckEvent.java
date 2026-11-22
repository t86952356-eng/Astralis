package astralis.event.impl;

import astralis.event.Event;

public class KeyPressCheckEvent implements Event {
    private boolean pressed = false;

    public boolean isPressed() {
        return this.pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
}
