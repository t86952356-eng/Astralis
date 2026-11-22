package astralis.ui.chat;

public record ChatScrollbarMetrics(
        float trackX,
        float trackY,
        float trackWidth,
        float trackHeight,
        float thumbY,
        float thumbHeight,
        int maxScroll) {
    public float getTrackHeight() {
        return this.trackHeight;
    }

    public float getTrackY() {
        return this.trackY;
    }

    public int getMaxScroll() {
        return this.maxScroll;
    }

    public float getThumbHeight() {
        return this.thumbHeight;
    }

    public float getTrackX() {
        return this.trackX;
    }

    public float getThumbY() {
        return this.thumbY;
    }

    public float getTrackWidth() {
        return this.trackWidth;
    }
}
