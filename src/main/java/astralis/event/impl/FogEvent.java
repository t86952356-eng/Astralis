package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import java.awt.Color;

/**
 * Posted after fog setup; a non-null colour replaces the fog colour, and the reduction is
 * subtracted from a 512-block sky end.
 */
public class FogEvent implements Event {
    private Float skyEndReduction;
    private Color color;

    @Generated
    public Float getSkyEndReduction() {
        return this.skyEndReduction;
    }

    @Generated
    public Color getColor() {
        return this.color;
    }

    @Generated
    public void setSkyEndReduction(Float skyEndReduction) {
        this.skyEndReduction = skyEndReduction;
    }

    @Generated
    public void setColor(Color color) {
        this.color = color;
    }
}
