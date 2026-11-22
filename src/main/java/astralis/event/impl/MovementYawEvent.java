package astralis.event.impl;

import astralis.event.Event;

/**
 * Supplies the yaw used to convert movement input in {@code Entity.moveRelative} for the local
 * player.
 */
public class MovementYawEvent implements Event {
    private float yaw;

    public MovementYawEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
