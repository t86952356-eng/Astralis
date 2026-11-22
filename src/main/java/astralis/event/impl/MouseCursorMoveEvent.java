package astralis.event.impl;

import astralis.event.CancellableEvent;

/** Posted after the cursor moves, with GUI-scaled coordinates. */
public class MouseCursorMoveEvent extends CancellableEvent {
    private final double x;
    private final double y;

    public MouseCursorMoveEvent(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getY() {
        return this.y;
    }

    public double getX() {
        return this.x;
    }
}
