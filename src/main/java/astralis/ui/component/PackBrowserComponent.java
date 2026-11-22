package astralis.ui.component;

import astralis.ui.screen.PackBrowserScreen;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.awt.Color;

/** Base for pack-browser components, with shared text and scrollbar drawing helpers. */
public abstract class PackBrowserComponent extends Widget implements PackBrowserPalette {
    protected final PackBrowserScreen packBrowserScreen;

    protected PackBrowserComponent(PackBrowserScreen screen) {
        this.packBrowserScreen = screen;
    }

    protected PackBrowserComponent(
            PackBrowserScreen screen, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.packBrowserScreen = screen;
    }

    protected void drawPlaceholderText(
            Render2DContext context,
            String text,
            float x,
            float y,
            float width,
            float height,
            SizedFont font) {
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(text)
                        .setPosition(
                                x + (width - font.getStringWidth(text)) / 2.0F,
                                y + height / 2.0F - font.getLineHeight())
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor(PackBrowserPaletteColors.TEXT_MUTED.getRGB()));
    }

    protected void drawScrollbar(
            Render2DContext context,
            float x,
            float y,
            float viewportHeight,
            float contentHeight,
            float scrollOffset,
            Color color) {
        if (contentHeight <= viewportHeight) {
            return;
        }

        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, 2.0F, viewportHeight)
                        .setRadius(1.0F)
                        .setColor(new Color(255, 255, 255, 16)));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                x,
                                this.getScrollThumbY(
                                        y, viewportHeight, contentHeight, scrollOffset),
                                2.0F,
                                this.getScrollThumbHeight(viewportHeight, contentHeight))
                        .setRadius(1.0F)
                        .setColor(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), 150)));
    }

    protected float getScrollThumbHeight(float viewportHeight, float contentHeight) {
        return Math.max(18.0F, viewportHeight * viewportHeight / contentHeight);
    }

    protected float getScrollThumbY(
            float y, float viewportHeight, float contentHeight, float scrollOffset) {
        float thumbY =
                y
                        + (viewportHeight
                                        - this.getScrollThumbHeight(viewportHeight, contentHeight))
                                * Math.clamp(
                                        scrollOffset / (viewportHeight - contentHeight),
                                        0.0F,
                                        1.0F);
        return thumbY;
    }

    protected float getScrollOffsetForThumbDrag(
            float mouseY,
            float trackY,
            float grabOffset,
            float viewportHeight,
            float contentHeight) {
        if (contentHeight <= viewportHeight) {
            return 0.0F;
        }

        float scrollFraction =
                Math.clamp(
                        (mouseY - grabOffset - trackY)
                                / Math.max(
                                        1.0F,
                                        viewportHeight
                                                - this.getScrollThumbHeight(
                                                        viewportHeight, contentHeight)),
                        0.0F,
                        1.0F);
        float scrollRange = viewportHeight - contentHeight;
        return scrollFraction * scrollRange;
    }
}
