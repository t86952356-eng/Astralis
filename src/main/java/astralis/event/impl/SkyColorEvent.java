package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

/** Supplies the ARGB colour passed to {@code SkyRenderer.renderSkyDisc}. */
public class SkyColorEvent implements Event {
    private int color;

    @Generated
    public int getColor() {
        return this.color;
    }

    @Generated
    public void setColor(int color) {
        this.color = color;
    }
}
