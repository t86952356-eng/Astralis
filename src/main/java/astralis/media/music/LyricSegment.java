package astralis.media.music;

public record LyricSegment(
        String text,
        float offsetX,
        int row,
        long startMs,
        long endMs,
        boolean letterAnimated,
        boolean background) {
    public String getText() {
        return this.text;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public int getRow() {
        return this.row;
    }

    public long getStartMs() {
        return this.startMs;
    }

    public long getEndMs() {
        return this.endMs;
    }

    public boolean isLetterAnimated() {
        return this.letterAnimated;
    }

    public boolean isBackground() {
        return this.background;
    }
}
