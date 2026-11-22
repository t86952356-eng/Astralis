package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

/**
 * Allows overriding the clock value returned by {@code ClientClockManager.getTotalTicks}; null
 * keeps vanilla time.
 */
public class WorldTimeEvent implements Event {
    private Long totalTicks;

    @Generated
    public Long getTotalTicks() {
        return this.totalTicks;
    }

    @Generated
    public void setTotalTicks(Long totalTicks) {
        this.totalTicks = totalTicks;
    }
}
