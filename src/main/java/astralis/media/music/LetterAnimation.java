package astralis.media.music;

public record LetterAnimation(SpringValue scale, SpringValue lift) {
    public SpringValue getScale() {
        return this.scale;
    }

    public SpringValue getLift() {
        return this.lift;
    }
}
