package astralis.util.render;

public enum PaintMode {
    STANDARD(0),
    LINEAR_GRADIENT(1),
    QUAD_GRADIENT(2),
    DIRECTIONAL_GRADIENT(3);

    final int shaderId;

    PaintMode(int shaderId) {
        this.shaderId = shaderId;
    }

    public int getShaderId() {
        return this.shaderId;
    }
}
