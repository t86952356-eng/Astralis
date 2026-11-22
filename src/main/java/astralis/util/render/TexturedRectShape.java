package astralis.util.render;

import astralis.util.ObjectPool;

import com.mojang.blaze3d.textures.GpuTexture;

import java.awt.Color;

public class TexturedRectShape {
    private static ObjectPool<TexturedRectShape> pool =
            new ObjectPool<>(10000, TexturedRectShape::new);
    private float x;
    private float y;
    private float width;
    private float height;
    private float uMin;
    private float vMin;
    private float uMax;
    private float vMax;
    private float topLeftRadius;
    private float topRightRadius;
    private float bottomRightRadius;
    private float bottomLeftRadius;
    private int variant;
    private GpuTexture texture;
    private int topLeftColor;
    private int topRightColor;
    private int bottomRightColor;
    private int bottomLeftColor;
    private boolean nearestFilter;
    private boolean outerAntialiasOnly;

    private TexturedRectShape() {
        this.reset();
    }

    public TexturedRectShape setTexture(GpuTexture gpuTexture) {
        this.texture = gpuTexture;
        return this;
    }

    public float getVMax() {
        return this.vMax;
    }

    public TexturedRectShape useNearestFilter() {
        this.nearestFilter = true;
        return this;
    }

    public TexturedRectShape setColor(Color color) {
        return this.setColor(color.getRGB());
    }

    public float getX() {
        return this.x;
    }

    public int getVariant() {
        return this.variant;
    }

    public TexturedRectShape setVariant(int variant) {
        this.variant = variant;
        return this;
    }

    public TexturedRectShape setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean isOuterAntialiasOnly() {
        return this.outerAntialiasOnly;
    }

    public float getHeight() {
        return this.height;
    }

    public int getBottomLeftColor() {
        return this.bottomLeftColor;
    }

    public float getUMin() {
        return this.uMin;
    }

    public float getTopRightRadius() {
        return this.topRightRadius;
    }

    public TexturedRectShape setOuterAntialiasOnly(boolean outerAntialiasOnly) {
        this.outerAntialiasOnly = outerAntialiasOnly;
        return this;
    }

    public TexturedRectShape reset() {
        this.x = this.y = this.width = this.height = 0.0F;
        this.uMin = this.vMin = 0.0F;
        this.uMax = this.vMax = 1.0F;
        this.topLeftRadius =
                this.topRightRadius = this.bottomRightRadius = this.bottomLeftRadius = 0.0F;
        this.variant = 0;
        this.texture = null;
        this.topLeftColor = this.topRightColor = this.bottomRightColor = this.bottomLeftColor = -1;
        this.nearestFilter = false;
        this.outerAntialiasOnly = false;
        return this;
    }

    public TexturedRectShape setUv(float uMin, float vMin, float uMax, float vMax) {
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;
        return this;
    }

    public TexturedRectShape setCornerRadii(
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

    public static TexturedRectShape obtain() {
        return pool.acquire().reset();
    }

    public int getBottomRightColor() {
        return this.bottomRightColor;
    }

    public int getTopLeftColor() {
        return this.topLeftColor;
    }

    public float getWidth() {
        return this.width;
    }

    public TexturedRectShape copy() {
        TexturedRectShape copy = obtain();
        copy.x = this.x;
        copy.y = this.y;
        copy.width = this.width;
        copy.height = this.height;
        copy.uMin = this.uMin;
        copy.vMin = this.vMin;
        copy.uMax = this.uMax;
        copy.vMax = this.vMax;
        copy.topLeftRadius = this.topLeftRadius;
        copy.topRightRadius = this.topRightRadius;
        copy.bottomRightRadius = this.bottomRightRadius;
        copy.bottomLeftRadius = this.bottomLeftRadius;
        copy.variant = this.variant;
        copy.texture = this.texture;
        copy.topLeftColor = this.topLeftColor;
        copy.topRightColor = this.topRightColor;
        copy.bottomRightColor = this.bottomRightColor;
        copy.bottomLeftColor = this.bottomLeftColor;
        copy.nearestFilter = this.nearestFilter;
        copy.outerAntialiasOnly = this.outerAntialiasOnly;
        return copy;
    }

    public float getBottomLeftRadius() {
        return this.bottomLeftRadius;
    }

    public float getY() {
        return this.y;
    }

    public TexturedRectShape setPaint(ShapePaint paint) {
        this.topLeftColor = paint.getTopLeftColor();
        this.topRightColor = paint.getTopRightColor();
        this.bottomRightColor = paint.getBottomRightColor();
        this.bottomLeftColor = paint.getBottomLeftColor();
        return this;
    }

    public float getVMin() {
        return this.vMin;
    }

    public boolean isNearestFilter() {
        return this.nearestFilter;
    }

    public float getUMax() {
        return this.uMax;
    }

    public int getTopRightColor() {
        return this.topRightColor;
    }

    public float getBottomRightRadius() {
        return this.bottomRightRadius;
    }

    public static void resetPool() {
        pool.reset();
    }

    public TexturedRectShape setColor(int color) {
        this.topLeftColor =
                this.topRightColor = this.bottomRightColor = this.bottomLeftColor = color;
        return this;
    }

    public float getTopLeftRadius() {
        return this.topLeftRadius;
    }

    public GpuTexture getTexture() {
        return this.texture;
    }

    public TexturedRectShape setRadius(float radius) {
        this.topLeftRadius =
                this.topRightRadius = this.bottomRightRadius = this.bottomLeftRadius = radius;
        return this;
    }
}
