package astralis.media.music;

import java.util.List;

public record LyricLineSplit(String mainText, String backgroundText, List<LyricWord> words) {
    public String getMainText() {
        return this.mainText;
    }

    public String getBackgroundText() {
        return this.backgroundText;
    }

    public List<LyricWord> getWords() {
        return this.words;
    }
}
