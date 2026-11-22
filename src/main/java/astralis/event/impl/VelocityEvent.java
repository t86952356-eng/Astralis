package astralis.event.impl;

import astralis.event.CancellableEvent;

public class VelocityEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;
    private boolean explosion;

    public VelocityEvent(double x, double y, double z, boolean explosion) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.explosion = explosion;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getX() {
        return this.x;
    }

    public void setExplosion(boolean explosion) {
        this.explosion = explosion;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isExplosion() {
        return this.explosion;
    }
}
