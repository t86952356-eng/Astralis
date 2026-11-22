package astralis.media.music;

import java.util.List;

/** Timed lyrics require the removed Astralis backend. */
public final class LyricsState {
    private static volatile List<LyricLine> lyrics;
    private static volatile LyricsProvider provider = LyricsProvider.NONE;

    private LyricsState() {}

    public static synchronized void requestLyrics(TrackInfo track) {
        lyrics = null;
        provider = LyricsProvider.NONE;
    }

    public static List<LyricLine> getLyrics() {
        return lyrics;
    }

    public static LyricsProvider getProvider() {
        return provider;
    }
}
