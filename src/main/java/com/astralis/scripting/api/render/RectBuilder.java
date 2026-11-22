package com.astralis.scripting.api.render;

import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;

public class RectBuilder {
    private final RenderContext context;
    private final RoundedRectShape rectDraw;
    private final ShapePaint fillPaint = new ShapePaint();
    private final ShapePaint outlinePaint = new ShapePaint();
    private float outlineWidth;

    RectBuilder(RenderContext context, float x, float y, float width, float height) {
        this.context = context;
        this.rectDraw = RoundedRectShape.obtain().setBounds(x, y, width, height);
    }

    public RectBuilder color(int argb) {
        this.fillPaint.setColor(argb);
        return this;
    }

    public RectBuilder color(ScriptColor color) {
        return this.color(color.argb());
    }

    public RectBuilder verticalGradient(int topColor, int bottomColor) {
        this.fillPaint.setVerticalGradient(topColor, bottomColor);
        return this;
    }

    public RectBuilder verticalGradient(ScriptColor topColor, ScriptColor bottomColor) {
        return this.verticalGradient(topColor.argb(), bottomColor.argb());
    }

    public RectBuilder horizontalGradient(int leftColor, int rightColor) {
        this.fillPaint.setHorizontalGradient(leftColor, rightColor);
        return this;
    }

    public RectBuilder horizontalGradient(ScriptColor leftColor, ScriptColor rightColor) {
        return this.horizontalGradient(leftColor.argb(), rightColor.argb());
    }

    public RectBuilder quadGradient(
            int topLeftColor, int topRightColor, int bottomRightColor, int bottomLeftColor) {
        this.fillPaint.setQuadGradient(
                topLeftColor, topRightColor, bottomRightColor, bottomLeftColor);
        return this;
    }

    public RectBuilder radius(double radius) {
        this.context.validateRenderValues(radius);
        if (!(radius < 0.0) && !(radius > 4096.0)) {
            this.rectDraw.setRadius((float) radius);
            return this;
        } else {
            throw new IllegalArgumentException("Rectangle radius must be between 0 and 4,096.");
        }
    }

    public RectBuilder cornerRadius(
            double topLeftRadius,
            double topRightRadius,
            double bottomRightRadius,
            double bottomLeftRadius) {
        this.context.validateRenderValues(
                topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
        if (!(topLeftRadius < 0.0)
                && !(topRightRadius < 0.0)
                && !(bottomRightRadius < 0.0)
                && !(bottomLeftRadius < 0.0)
                && !(topLeftRadius > 4096.0)
                && !(topRightRadius > 4096.0)
                && !(bottomRightRadius > 4096.0)
                && !(bottomLeftRadius > 4096.0)) {
            this.rectDraw.setCornerRadii(
                    (float) topLeftRadius,
                    (float) topRightRadius,
                    (float) bottomRightRadius,
                    (float) bottomLeftRadius);
            return this;
        } else {
            throw new IllegalArgumentException("Corner radii must be between 0 and 4,096.");
        }
    }

    public RectBuilder outline(double width, int color) {
        this.context.validateRenderValues(width);
        if (!(width < 0.0) && !(width > 4096.0)) {
            this.outlineWidth = (float) width;
            this.outlinePaint.setColor(color);
            return this;
        } else {
            throw new IllegalArgumentException("Outline width must be between 0 and 4,096.");
        }
    }

    public RectBuilder outlineGradient(double width, int topColor, int bottomColor) {
        this.context.validateRenderValues(width);
        if (!(width < 0.0) && !(width > 4096.0)) {
            this.outlineWidth = (float) width;
            this.outlinePaint.setVerticalGradient(topColor, bottomColor);
            return this;
        } else {
            throw new IllegalArgumentException("Outline width must be between 0 and 4,096.");
        }
    }

    public RectBuilder shadow() {
        this.rectDraw.enableShadow();
        return this;
    }

    public RectBuilder shadow(int alpha) {
        this.rectDraw.setShadowAlpha(alpha);
        return this;
    }

    public void draw() {
        this.rectDraw.setPaint(this.fillPaint);
        if (this.outlineWidth > 0.0F) {
            this.rectDraw.setOutline(this.outlineWidth, this.outlinePaint);
        }

        this.context.submitRect(this.rectDraw);
    }
}
