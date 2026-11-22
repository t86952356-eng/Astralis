package astralis.media.music;

public record LyricWord(long startMs, long endMs, String text) {
    public long getStartMs() {
        return this.startMs;
    }

    public long getEndMs() {
        return this.endMs;
    }

    public String getText() {
        return this.text;
    }
}
