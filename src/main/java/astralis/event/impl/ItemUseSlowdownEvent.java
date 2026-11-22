package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Replaces {@code LocalPlayer.itemUseSpeedMultiplier()} in {@code modifyInput}; cancelling uses a
 * multiplier of 1 (no slowdown).
 */
public class ItemUseSlowdownEvent extends CancellableEvent {
    public float multiplier;

    public ItemUseSlowdownEvent(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }
}
