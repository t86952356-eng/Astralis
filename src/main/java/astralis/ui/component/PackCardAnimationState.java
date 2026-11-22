package astralis.ui.component;

public record PackCardAnimationState(
        float appearProgress, float textureFadeProgress, float hoverProgress) {
    public float getAppearProgress() {
        return this.appearProgress;
    }

    public float getTextureFadeProgress() {
        return this.textureFadeProgress;
    }

    public float getHoverProgress() {
        return this.hoverProgress;
    }
}
