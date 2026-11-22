package astralis.event.impl;

import astralis.event.CancellableEvent;

import net.minecraft.world.entity.MoverType;

/** Posted when the local player moves; handlers may change the motion, and cancelling zeroes it. */
public class PlayerMoveEvent extends CancellableEvent {
    private MoverType moverType;
    private double x;
    private double y;
    private double z;

    public PlayerMoveEvent(MoverType moverType, double x, double y, double z) {
        super();
        this.moverType = moverType;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getZ() {
        return this.z;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public MoverType getMoverType() {
        return this.moverType;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setMoverType(MoverType moverType) {
        this.moverType = moverType;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setX(double x) {
        this.x = x;
    }
}
