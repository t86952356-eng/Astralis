package com.astralis.scripting.api.render;

import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

public class TextBuilder {
    private static final String DEFAULT_FONT = "Inter Medium";
    private final RenderContext context;
    private final TextDrawRequest textDraw;
    private final ShapePaint paint = new ShapePaint();
    private String fontName = "Inter Medium";

    TextBuilder(RenderContext context, String text, float x, float y) {
        this.context = context;
        TextDrawRequest request =
                TextDrawRequest.obtain().setText(text).setPosition(x, y).setFontSize(10.0F);
        SizedFont sizedFont = context.resolveFont("Inter Medium", 10.0F);
        this.textDraw = request.setFont(sizedFont.getMsdfFont());
        this.paint.setColor(-1);
    }

    public TextBuilder size(double size) {
        TextDrawRequest request = this.textDraw.setFontSize((float) size);
        SizedFont sizedFont = this.context.resolveFont(this.fontName, (float) size);
        request.setFont(sizedFont.getMsdfFont());
        return this;
    }

    public TextBuilder font(String fontName) {
        this.fontName = fontName;
        TextDrawRequest request = this.textDraw;
        SizedFont sizedFont = this.context.resolveFont(fontName, this.textDraw.getFontSize());
        request.setFont(sizedFont.getMsdfFont());
        return this;
    }

    public TextBuilder shadow() {
        this.textDraw.enableShadow();
        return this;
    }

    public TextBuilder color(int argb) {
        this.paint.setColor(argb);
        return this;
    }

    public TextBuilder color(ScriptColor color) {
        return this.color(color.argb());
    }

    public TextBuilder horizontalGradient(int leftColor, int rightColor) {
        this.paint.setHorizontalGradient(leftColor, rightColor);
        return this;
    }

    public TextBuilder horizontalGradient(ScriptColor leftColor, ScriptColor rightColor) {
        return this.horizontalGradient(leftColor.argb(), rightColor.argb());
    }

    public TextBuilder verticalGradient(int topColor, int bottomColor) {
        this.paint.setVerticalGradient(topColor, bottomColor);
        return this;
    }

    public TextBuilder verticalGradient(ScriptColor topColor, ScriptColor bottomColor) {
        return this.verticalGradient(topColor.argb(), bottomColor.argb());
    }

    public TextBuilder align(double width, double height) {
        this.context.validateRenderValues(width, height);
        if (!(width < 0.0) && !(height < 0.0) && !(width > 1000000.0) && !(height > 1000000.0)) {
            this.textDraw.centerInBox((float) width, (float) height);
            return this;
        } else {
            throw new IllegalArgumentException(
                    "Text alignment bounds must be between 0 and 1,000,000.");
        }
    }

    public TextBuilder alignX(double width) {
        this.context.validateRenderValues(width);
        if (!(width < 0.0) && !(width > 1000000.0)) {
            this.textDraw.centerHorizontally((float) width);
            return this;
        } else {
            throw new IllegalArgumentException(
                    "Text alignment width must be between 0 and 1,000,000.");
        }
    }

    public TextBuilder alignY(double height) {
        this.context.validateRenderValues(height);
        if (!(height < 0.0) && !(height > 1000000.0)) {
            this.textDraw.centerVertically((float) height);
            return this;
        } else {
            throw new IllegalArgumentException(
                    "Text alignment height must be between 0 and 1,000,000.");
        }
    }

    public void draw() {
        this.context.submitText(this.textDraw.setPaint(this.paint));
    }
}
