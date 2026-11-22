package astralis.event.impl;

import astralis.event.Event;

/**
 * Supplies the yaw used for the local player's jump boost in {@code LivingEntity.jumpFromGround}.
 */
public class JumpYawEvent implements Event {
    private float yaw;

    public JumpYawEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
