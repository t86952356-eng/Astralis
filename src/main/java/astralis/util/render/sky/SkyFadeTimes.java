package astralis.util.render.sky;

public record SkyFadeTimes(
        int startFadeIn, int endFadeIn, int startFadeOut, int endFadeOut, boolean alwaysVisible) {

    public float getFadeFactor(int dayTime) {
        if (this.alwaysVisible
                || SkyLayerProperties.isTimeInWindow(dayTime, this.endFadeIn, this.startFadeOut)) {
            return 1.0F;
        } else if (SkyLayerProperties.isTimeInWindow(dayTime, this.startFadeIn, this.endFadeIn)) {
            int fadeInDuration = ticksBetween(this.startFadeIn, this.endFadeIn);
            return fadeInDuration == 0
                    ? 1.0F
                    : (float) ticksBetween(this.startFadeIn, dayTime) / fadeInDuration;
        } else if (SkyLayerProperties.isTimeInWindow(dayTime, this.startFadeOut, this.endFadeOut)) {
            int fadeOutDuration = ticksBetween(this.startFadeOut, this.endFadeOut);
            return fadeOutDuration == 0
                    ? 1.0F
                    : 1.0F - (float) ticksBetween(this.startFadeOut, dayTime) / fadeOutDuration;
        } else {
            return 0.0F;
        }
    }

    private static int ticksBetween(int from, int to) {
        return (to - from + 24000) % 24000;
    }

    public int getStartFadeIn() {
        return this.startFadeIn;
    }

    public int getEndFadeIn() {
        return this.endFadeIn;
    }

    public int getStartFadeOut() {
        return this.startFadeOut;
    }

    public int getEndFadeOut() {
        return this.endFadeOut;
    }

    public boolean isAlwaysVisible() {
        return this.alwaysVisible;
    }
}
