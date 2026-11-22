package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Posted when {@code MouseHandler.turnPlayer} applies accumulated mouse movement; handlers may
 * change the deltas and sensitivity factor, disable smooth camera, or cancel the turn.
 */
public class MouseUpdateEvent extends CancellableEvent {
    private double deltaX;
    private double deltaY;
    private double sensitivityFactor;
    private final double originalDeltaX;
    private final double originalDeltaY;
    private final double originalSensitivityFactor;
    private boolean smoothCameraDisabled;

    public MouseUpdateEvent(double deltaX, double deltaY, double sensitivityFactor) {

        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.sensitivityFactor = sensitivityFactor;
        this.originalDeltaX = deltaX;
        this.originalDeltaY = deltaY;
        this.originalSensitivityFactor = sensitivityFactor;
    }

    public void setSensitivityFactor(double sensitivityFactor) {
        this.sensitivityFactor = sensitivityFactor;
    }

    public void setSmoothCameraDisabled(boolean smoothCameraDisabled) {
        this.smoothCameraDisabled = smoothCameraDisabled;
    }

    public boolean isSmoothCameraDisabled() {
        return this.smoothCameraDisabled;
    }

    public double getDeltaX() {
        return this.deltaX;
    }

    public void disableSmoothCamera() {
        this.smoothCameraDisabled = true;
    }

    public double getDeltaY() {
        return this.deltaY;
    }

    public double getOriginalDeltaX() {
        return this.originalDeltaX;
    }

    public double getOriginalSensitivityFactor() {
        return this.originalSensitivityFactor;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }

    public double getOriginalDeltaY() {
        return this.originalDeltaY;
    }

    public double getSensitivityFactor() {
        return this.sensitivityFactor;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }
}
