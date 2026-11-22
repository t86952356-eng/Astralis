package astralis.util.render;

import astralis.util.ObjectPool;

import java.awt.Color;

public class RoundedRectShape {
    private static ObjectPool<RoundedRectShape> pool;
    private float x;
    private float y;
    private float width;
    private float height;
    private float topLeftRadius;
    private float topRightRadius;
    private float bottomRightRadius;
    private float bottomLeftRadius;
    private float outlineWidth;
    private int shadowAlpha;
    private boolean outerAntialiasOnly;
    private int topLeftColor;
    private int topRightColor;
    private int bottomRightColor;
    private int bottomLeftColor;
    private int outlineTopLeftColor;
    private int outlineTopRightColor;
    private int outlineBottomRightColor;
    private int outlineBottomLeftColor;

    private RoundedRectShape() {
        this.reset();
    }

    public RoundedRectShape setShadowAlpha(int alpha) {
        long alphaValue;
        alphaValue = alpha;

        this.shadowAlpha = Math.clamp(alphaValue, 0, 255);
        return this;
    }

    public float getTopRightRadius() {
        return this.topRightRadius;
    }

    public RoundedRectShape setOuterAntialiasOnly(boolean outerAntialiasOnly) {
        this.outerAntialiasOnly = outerAntialiasOnly;
        return this;
    }

    public int getTopLeftColor() {
        return this.topLeftColor;
    }

    public int getOutlineTopRightColor() {
        return this.outlineTopRightColor;
    }

    public RoundedRectShape setCornerRadii(
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius) {
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
        this.bottomRightRadius = bottomRightRadius;
        this.bottomLeftRadius = bottomLeftRadius;
        return this;
    }

    public RoundedRectShape enableShadow() {

        this.shadowAlpha = 255;
        return this;
    }

    public RoundedRectShape setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public RoundedRectShape setRadius(float radius) {
        this.topLeftRadius =
                this.topRightRadius = this.bottomRightRadius = this.bottomLeftRadius = radius;
        return this;
    }

    public RoundedRectShape setOutline(float outlineWidth, ShapePaint paint) {
        this.outlineWidth = outlineWidth;
        this.outlineTopLeftColor = paint.getTopLeftColor();
        this.outlineTopRightColor = paint.getTopRightColor();
        this.outlineBottomRightColor = paint.getBottomRightColor();
        this.outlineBottomLeftColor = paint.getBottomLeftColor();
        return this;
    }

    public RoundedRectShape setOutline(float outlineWidth, Color color) {
        this.outlineWidth = outlineWidth;
        this.outlineTopLeftColor =
                this.outlineTopRightColor =
                        this.outlineBottomRightColor = this.outlineBottomLeftColor = color.getRGB();
        return this;
    }

    public int getTopRightColor() {
        return this.topRightColor;
    }

    public float getY() {
        return this.y;
    }

    public RoundedRectShape setColor(int color) {
        this.topLeftColor =
                this.topRightColor = this.bottomRightColor = this.bottomLeftColor = color;
        return this;
    }

    public float getX() {
        return this.x;
    }

    public float getTopLeftRadius() {
        return this.topLeftRadius;
    }

    public boolean isOuterAntialiasOnly() {
        return this.outerAntialiasOnly;
    }

    public float getBottomRightRadius() {
        return this.bottomRightRadius;
    }

    public float getWidth() {
        return this.width;
    }

    public int getOutlineBottomLeftColor() {
        return this.outlineBottomLeftColor;
    }

    public RoundedRectShape reset() {
        this.x = this.y = this.width = this.height = 0.0F;
        this.topLeftRadius =
                this.topRightRadius = this.bottomRightRadius = this.bottomLeftRadius = 0.0F;
        this.outlineWidth = 0.0F;
        this.shadowAlpha = -1;
        this.outerAntialiasOnly = false;
        this.topLeftColor = this.topRightColor = this.bottomRightColor = this.bottomLeftColor = -1;
        this.outlineTopLeftColor =
                this.outlineTopRightColor =
                        this.outlineBottomRightColor = this.outlineBottomLeftColor = 0;
        return this;
    }

    public int getOutlineBottomRightColor() {
        return this.outlineBottomRightColor;
    }

    public int getShadowAlpha() {
        return this.shadowAlpha;
    }

    public static RoundedRectShape obtain() {
        return pool.acquire().reset();
    }

    public RoundedRectShape setColor(Color color) {
        return this.setColor(color.getRGB());
    }

    public RoundedRectShape setPaint(ShapePaint paint) {
        this.topLeftColor = paint.getTopLeftColor();
        this.topRightColor = paint.getTopRightColor();
        this.bottomRightColor = paint.getBottomRightColor();
        this.bottomLeftColor = paint.getBottomLeftColor();
        return this;
    }

    public RoundedRectShape setShadowEnabled(boolean enabled) {
        short alpha;
        if (enabled) {

            alpha = 255;
        } else {
            alpha = -1;
        }

        this.shadowAlpha = alpha;
        return this;
    }

    public RoundedRectShape copy() {
        RoundedRectShape result = obtain();
        result.x = this.x;
        result.y = this.y;
        result.width = this.width;
        result.height = this.height;
        result.topLeftRadius = this.topLeftRadius;
        result.topRightRadius = this.topRightRadius;
        result.bottomRightRadius = this.bottomRightRadius;
        result.bottomLeftRadius = this.bottomLeftRadius;
        result.outlineWidth = this.outlineWidth;
        result.shadowAlpha = this.shadowAlpha;
        result.outerAntialiasOnly = this.outerAntialiasOnly;
        result.topLeftColor = this.topLeftColor;
        result.topRightColor = this.topRightColor;
        result.bottomRightColor = this.bottomRightColor;
        result.bottomLeftColor = this.bottomLeftColor;
        result.outlineTopLeftColor = this.outlineTopLeftColor;
        result.outlineTopRightColor = this.outlineTopRightColor;
        result.outlineBottomRightColor = this.outlineBottomRightColor;
        result.outlineBottomLeftColor = this.outlineBottomLeftColor;
        return result;
    }

    public float getBottomLeftRadius() {
        return this.bottomLeftRadius;
    }

    public static void resetPool() {
        pool.reset();
    }

    public float getOutlineWidth() {
        return this.outlineWidth;
    }

    public int getBottomLeftColor() {
        return this.bottomLeftColor;
    }

    public int getBottomRightColor() {
        return this.bottomRightColor;
    }

    public int getOutlineTopLeftColor() {
        return this.outlineTopLeftColor;
    }

    public float getHeight() {
        return this.height;
    }

    public RoundedRectShape setShadow(boolean enabled, int alpha) {
        long alphaValue;
        int newShadowAlpha;
        if (enabled) {
            alphaValue = alpha;

            newShadowAlpha = Math.clamp(alphaValue, 0, 255);
        } else {
            newShadowAlpha = -1;
        }

        this.shadowAlpha = newShadowAlpha;
        return this;
    }

    static {
        pool = new ObjectPool<>(10000, RoundedRectShape::new);
    }
}
