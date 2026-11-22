package astralis.ui.inventory;

import java.awt.Color;

public record SlotHighlight(long startTimeMillis, Color color) {
    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    public Color getColor() {
        return this.color;
    }
}
