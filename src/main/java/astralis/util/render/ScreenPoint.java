package astralis.util.render;

public record ScreenPoint(
        float screenX, float screenY, float depth, boolean behindCamera, boolean valid) {
    public static ScreenPoint invalid() {
        return new ScreenPoint(0.0F, 0.0F, -1.0F, false, false);
    }

    public float getScreenX() {
        return this.screenX;
    }

    public float getScreenY() {
        return this.screenY;
    }

    public float getDepth() {
        return this.depth;
    }

    public boolean isBehindCamera() {
        return this.behindCamera;
    }

    public boolean isValid() {
        return this.valid;
    }
}
