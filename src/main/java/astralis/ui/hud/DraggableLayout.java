package astralis.ui.hud;

public record DraggableLayout(float normalizedX, float normalizedY, float scale) {
    public float getNormalizedX() {
        return this.normalizedX;
    }

    public float getNormalizedY() {
        return this.normalizedY;
    }

    public float getScale() {
        return this.scale;
    }
}
