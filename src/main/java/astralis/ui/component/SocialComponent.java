package astralis.ui.component;

import astralis.Astralis;
import astralis.ui.screen.FriendsScreen;
import astralis.util.render.ColorUtil;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Base for social-screen components, with shared text, avatar and scrollbar drawing helpers. */
public abstract class SocialComponent extends Widget implements FlatUiPalette {
    protected final FriendsScreen socialScreen;

    protected SocialComponent(FriendsScreen screen) {
        this.socialScreen = screen;
    }

    protected SocialComponent(FriendsScreen screen, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.socialScreen = screen;
    }

    protected boolean containsIgnoreCase(List<String> values, String target) {
        if (target == null) {
            return false;
        }

        Iterator iterator = values.iterator();

        while (iterator.hasNext()) {
            String value = (String) iterator.next();
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }

        return false;
    }

    protected void drawAvatar(
            Render2DContext context,
            String username,
            float x,
            float y,
            float size,
            SizedFont font) {
        GpuTexture gpuTexture =
                Astralis.getInstance().getFriendManager().getFriendProfilePictureTexture(username);
        if (gpuTexture == null) {
            Color color = Astralis.getInstance().getPrimaryColor();
            Color color2 = Astralis.getInstance().getSecondaryColor();
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(x, y, size, size)
                            .setRadius(size / 2.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    ColorUtil.waveBlend(
                                                            x, color.getRGB(), color2.getRGB()),
                                                    ColorUtil.waveBlend(
                                                            y, color.getRGB(), color2.getRGB()))));
            TextDrawRequest initialRequest = TextDrawRequest.obtain();
            String initial;
            if (username.isEmpty()) {
                initial = "?";
            } else {
                initial = username.substring(0, 1);
                initial = initial.toUpperCase(Locale.ROOT);
            }

            context.drawText(
                    initialRequest
                            .setText(initial)
                            .setPosition(x, y)
                            .centerInBox(size, size)
                            .setFont(font.getMsdfFont())
                            .setFontSize(font.getSize())
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
            return;
        }

        context.drawTexture(
                TexturedRectShape.obtain()
                        .setBounds(x, y, size, size)
                        .setTexture(gpuTexture)
                        .setRadius(size / 2.0F)
                        .setColor(Color.WHITE));
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
                        .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
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
        return y
                + (viewportHeight - this.getScrollThumbHeight(viewportHeight, contentHeight))
                        * Math.clamp(scrollOffset / (viewportHeight - contentHeight), 0.0F, 1.0F);
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
