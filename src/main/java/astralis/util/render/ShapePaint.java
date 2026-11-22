package astralis.util.render;

import java.awt.Color;

public class ShapePaint {
    private static final ShapePaint SHARED_INSTANCE = new ShapePaint();
    private PaintMode paintType;
    private int topLeftColor;
    private int topRightColor;
    private int bottomRightColor;
    private int bottomLeftColor;
    private float gradientStartX;
    private float gradientStartY;
    private float gradientEndX;
    private float gradientEndY;
    private static boolean legacyFlag;

    public ShapePaint() {
        this.reset();
    }

    static {
        setLegacyFlag(true);
    }

    public int getTopRightColor() {
        return this.topRightColor;
    }

    public ShapePaint setQuadGradient(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.paintType = PaintMode.QUAD_GRADIENT;
        this.topLeftColor = topLeft;
        this.topRightColor = topRight;
        this.bottomRightColor = bottomRight;
        this.bottomLeftColor = bottomLeft;
        return this;
    }

    public boolean isSamePaint(ShapePaint other) {
        if (other == null) {
            return false;
        } else {
            return this == other
                    ? true
                    : this.paintType == other.paintType
                            && this.topLeftColor == other.topLeftColor
                            && this.topRightColor == other.topRightColor
                            && this.bottomRightColor == other.bottomRightColor
                            && this.bottomLeftColor == other.bottomLeftColor
                            && this.gradientStartX == other.gradientStartX
                            && this.gradientStartY == other.gradientStartY
                            && this.gradientEndX == other.gradientEndX
                            && this.gradientEndY == other.gradientEndY;
        }
    }

    public float getGradientEndY() {
        return this.gradientEndY;
    }

    public ShapePaint setColor(Color color) {
        return this.setColor(color.getRGB());
    }

    public ShapePaint reset() {
        this.paintType = PaintMode.STANDARD;
        this.topLeftColor = this.topRightColor = this.bottomRightColor = this.bottomLeftColor = -1;
        this.gradientStartX = 0.0F;
        this.gradientStartY = 0.0F;
        this.gradientEndX = 0.0F;
        this.gradientEndY = 0.0F;
        return this;
    }

    public void copyFrom(ShapePaint source) {
        this.paintType = source.paintType;
        this.topLeftColor = source.topLeftColor;
        this.topRightColor = source.topRightColor;
        this.bottomRightColor = source.bottomRightColor;
        this.bottomLeftColor = source.bottomLeftColor;
        this.gradientStartX = source.gradientStartX;
        this.gradientStartY = source.gradientStartY;
        this.gradientEndX = source.gradientEndX;
        this.gradientEndY = source.gradientEndY;
    }

    public ShapePaint setColor(int color) {
        this.paintType = PaintMode.STANDARD;
        this.topLeftColor =
                this.topRightColor = this.bottomRightColor = this.bottomLeftColor = color;
        return this;
    }

    public ShapePaint setHorizontalGradient(int leftColor, int rightColor) {
        this.paintType = PaintMode.DIRECTIONAL_GRADIENT;
        this.topLeftColor = leftColor;
        this.topRightColor = rightColor;
        this.bottomRightColor = rightColor;
        this.bottomLeftColor = leftColor;
        return this;
    }

    public ShapePaint setVerticalGradient(int topColor, int bottomColor) {
        this.paintType = PaintMode.DIRECTIONAL_GRADIENT;
        this.topLeftColor = topColor;
        this.topRightColor = topColor;
        this.bottomRightColor = bottomColor;
        this.bottomLeftColor = bottomColor;
        return this;
    }

    public ShapePaint setPaintType(PaintMode mode) {
        this.paintType = mode;
        return this;
    }

    public static ShapePaint obtain() {
        return SHARED_INSTANCE.reset();
    }

    public int getBottomLeftColor() {
        return this.bottomLeftColor;
    }

    public static void setLegacyFlag(boolean enabled) {
        legacyFlag = enabled;
    }

    public int getTopLeftColor() {
        return this.topLeftColor;
    }

    public float getGradientStartY() {
        return this.gradientStartY;
    }

    public PaintMode getPaintType() {
        return this.paintType;
    }

    public int getBottomRightColor() {
        return this.bottomRightColor;
    }

    public float getGradientStartX() {
        return this.gradientStartX;
    }

    public float getGradientEndX() {
        return this.gradientEndX;
    }

    public ShapePaint setLinearGradient(
            int startColor, int endColor, float startX, float startY, float endX, float endY) {
        this.paintType = PaintMode.LINEAR_GRADIENT;
        this.topLeftColor = startColor;
        this.topRightColor = endColor;
        this.bottomRightColor = endColor;
        this.bottomLeftColor = startColor;
        this.gradientStartX = startX;
        this.gradientStartY = startY;
        this.gradientEndX = endX;
        this.gradientEndY = endY;
        return this;
    }
}
