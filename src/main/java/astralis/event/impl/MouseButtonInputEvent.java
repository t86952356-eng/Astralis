package astralis.event.impl;

import astralis.event.CancellableEvent;

/** Wraps {@code MouseHandler.onButton} for presses and releases; cancelling drops the input. */
public class MouseButtonInputEvent extends CancellableEvent {
    private final int button;
    private final boolean pressed;
    private final double x;
    private final double y;

    public MouseButtonInputEvent(int button, boolean pressed, double x, double y) {

        this.button = button;
        this.pressed = pressed;
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public int getButton() {
        return this.button;
    }

    public boolean isPressed() {
        return this.pressed;
    }
}
