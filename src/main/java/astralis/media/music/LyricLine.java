package astralis.media.music;

import java.util.List;

public record LyricLine(long startMs, long endMs, String text, List<LyricWord> words) {

    public boolean hasWords() {
        return this.words != null && !this.words.isEmpty();
    }

    public long getStartMs() {
        return this.startMs;
    }

    public long getEndMs() {
        return this.endMs;
    }

    public String getText() {
        return this.text;
    }

    public List<LyricWord> getWords() {
        return this.words;
    }
}
