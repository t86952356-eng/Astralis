package astralis.util.render.font;

import astralis.util.ObjectPool;
import astralis.util.render.ShapePaint;

import java.awt.Color;

public class TextDrawRequest {
    private static ObjectPool<TextDrawRequest> pool = new ObjectPool<>(10000, TextDrawRequest::new);
    private String text;
    private float x;
    private float y;
    private float fontSize;
    private MsdfFont font;
    private boolean centered;
    private float boxWidth;
    private float boxHeight;
    private boolean shadow;
    private final ShapePaint paint = new ShapePaint();

    private TextDrawRequest() {
        this.reset();
    }

    public TextDrawRequest setText(String text) {
        this.text = text;
        return this;
    }

    public MsdfFont getFont() {
        return this.font;
    }

    public TextDrawRequest centerInBox(float boxWidth, float boxHeight) {
        this.centered = true;
        this.boxWidth = boxWidth;
        this.boxHeight = boxHeight;
        return this;
    }

    public float getBoxWidth() {
        return this.boxWidth;
    }

    public float getBoxHeight() {
        return this.boxHeight;
    }

    public TextDrawRequest setHorizontalGradient(int startColor, int endColor) {
        this.paint.setLinearGradient(
                startColor,
                endColor,
                this.x,
                this.y,
                this.x + this.font.getStringWidth(this.text, this.fontSize),
                this.y);
        return this;
    }

    public static void resetPool() {
        pool.reset();
    }

    public TextDrawRequest copy() {
        TextDrawRequest copy = obtain();
        copy.text = this.text;
        copy.x = this.x;
        copy.y = this.y;
        copy.fontSize = this.fontSize;
        copy.font = this.font;
        copy.centered = this.centered;
        copy.boxWidth = this.boxWidth;
        copy.boxHeight = this.boxHeight;
        copy.shadow = this.shadow;
        copy.paint.copyFrom(this.paint);
        return copy;
    }

    public TextDrawRequest reset() {
        this.text = "";
        this.x = this.y = 0.0F;
        this.fontSize = 10.0F;
        this.font = null;
        this.paint.reset();
        this.centered = false;
        this.boxWidth = 0.0F;
        this.boxHeight = 0.0F;
        this.shadow = false;
        return this;
    }

    public TextDrawRequest enableShadow() {
        this.shadow = true;
        return this;
    }

    public TextDrawRequest setFont(MsdfFont font) {
        this.font = font;
        return this;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public TextDrawRequest setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public float getX() {
        return this.x;
    }

    public TextDrawRequest centerVertically(float height) {
        this.centered = true;
        this.boxHeight = height;
        return this;
    }

    public TextDrawRequest setColor(int color) {
        this.paint.setColor(color);
        return this;
    }

    public String getText() {
        return this.text;
    }

    public TextDrawRequest centerHorizontally(float width) {
        this.centered = true;
        this.boxWidth = width;
        return this;
    }

    public boolean hasShadow() {
        return this.shadow;
    }

    public float getY() {
        return this.y;
    }

    public ShapePaint getPaint() {
        return this.paint;
    }

    public boolean isCentered() {
        return this.centered;
    }

    public TextDrawRequest setColor(Color color) {
        this.paint.setColor(color);
        return this;
    }

    public TextDrawRequest setPaint(ShapePaint paint) {
        this.paint.copyFrom(paint);
        return this;
    }

    public static TextDrawRequest obtain() {
        return pool.acquire().reset();
    }

    public TextDrawRequest setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }
}
