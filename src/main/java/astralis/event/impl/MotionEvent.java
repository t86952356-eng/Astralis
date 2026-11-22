package astralis.event.impl;

import astralis.event.CancellableEvent;

public class MotionEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public float getYaw() {
        return this.yaw;
    }

    public double getY() {
        return this.y;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public double getX() {
        return this.x;
    }

    public float getPitch() {
        return this.pitch;
    }

    public double getZ() {
        return this.z;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
