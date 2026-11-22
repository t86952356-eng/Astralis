package astralis.media.music;

public record TrackInfo(
        String title,
        String artist,
        String album,
        long durationMs,
        long positionMs,
        long positionUpdatedAtMs,
        boolean playing) {

    public long getEstimatedPositionMs() {
        long position =
                this.playing
                        ? this.positionMs + (System.currentTimeMillis() - this.positionUpdatedAtMs)
                        : this.positionMs;
        return this.durationMs > 0L ? Math.clamp(position, 0L, this.durationMs) : position;
    }

    public String getAlbum() {
        return this.album;
    }

    public long getPositionUpdatedAtMs() {
        return this.positionUpdatedAtMs;
    }

    public String getArtist() {
        return this.artist;
    }

    public long getPositionMs() {
        return this.positionMs;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public String getTitle() {
        return this.title;
    }

    public long getDurationMs() {
        return this.durationMs;
    }
}
