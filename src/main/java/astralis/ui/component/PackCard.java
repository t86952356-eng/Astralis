package astralis.ui.component;

import astralis.Astralis;
import astralis.resourcepack.PackThumbnailCache;
import astralis.resourcepack.PvprpPack;
import astralis.ui.screen.PackBrowserScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import lombok.Generated;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;

public class PackCard extends PackBrowserComponent {
    private final PvprpPack pack;
    private final PackThumbnailCache thumbnails;
    private final Runnable onClick;
    private final int index;
    private final PackCardAnimator animator;
    private boolean selected;

    public PackCard(
            PackBrowserScreen screen,
            PvprpPack pack,
            PackThumbnailCache thumbnails,
            Runnable onClick,
            int index) {
        super(screen);
        this.pack = pack;
        this.thumbnails = thumbnails;
        this.onClick = onClick;
        this.index = index;
        this.animator = new PackCardAnimator(index);
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 8.0F);

        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 6.5F);
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        boolean hovered =
                RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight());
        float thumbnailHeight = this.getHeight() - 37.0F;
        long nowMs = System.nanoTime() / 1000000L;
        GpuTexture gpuTexture = this.thumbnails.get(this.pack.getIconUrl());
        PackCardAnimationState animationState =
                this.animator.advance(nowMs, hovered, gpuTexture != null);
        Color color3 =
                this.selected
                        ? PackBrowserPaletteColors.CONTROL_ACTIVE
                        : new Color(
                                Math.round(
                                        PackBrowserPaletteColors.CONTROL.getRed()
                                                + (PackBrowserPaletteColors.CONTROL_HOVER.getRed()
                                                                - PackBrowserPaletteColors.CONTROL
                                                                        .getRed())
                                                        * animationState.getHoverProgress()),
                                Math.round(
                                        PackBrowserPaletteColors.CONTROL.getGreen()
                                                + (PackBrowserPaletteColors.CONTROL_HOVER.getGreen()
                                                                - PackBrowserPaletteColors.CONTROL
                                                                        .getGreen())
                                                        * animationState.getHoverProgress()),
                                Math.round(
                                        PackBrowserPaletteColors.CONTROL.getBlue()
                                                + (PackBrowserPaletteColors.CONTROL_HOVER.getBlue()
                                                                - PackBrowserPaletteColors.CONTROL
                                                                        .getBlue())
                                                        * animationState.getHoverProgress()));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                        .setRadius(7.0F)
                        .setColor(scaleAlpha(color3, animationState.getAppearProgress())));
        float thumbnailX = this.getX() + 5.0F;
        float thumbnailY = this.getY() + 5.0F;
        float thumbnailWidth = this.getWidth() - 10.0F;
        if (gpuTexture == null || animationState.getTextureFadeProgress() < 1.0F) {
            boolean loading = this.thumbnails.isLoading(this.pack.getIconUrl());
            int loadingPulse;
            if (loading) {

                loadingPulse =
                        27 + Math.round(3.0F * (float) Math.sin(nowMs / 420.0 + this.index * 0.35));
            } else {

                loadingPulse = 25;
            }

            int grayLevel = loadingPulse;
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(thumbnailX, thumbnailY, thumbnailWidth, thumbnailHeight)
                            .setRadius(5.0F)
                            .setColor(
                                    scaleAlpha(
                                            new Color(grayLevel, grayLevel, grayLevel),
                                            animationState.getAppearProgress())));
            if (gpuTexture == null && !loading) {
                TextDrawRequest placeholderRequest = TextDrawRequest.obtain();

                context.drawText(
                        placeholderRequest
                                .setText("Preview unavailable")
                                .setPosition(thumbnailX, thumbnailY)
                                .centerInBox(thumbnailWidth, thumbnailHeight)
                                .setFont(bodyFont.getMsdfFont())
                                .setFontSize(bodyFont.getSize())
                                .setColor(
                                        scaleAlpha(
                                                        PackBrowserPaletteColors.TEXT_MUTED,
                                                        animationState.getAppearProgress())
                                                .getRGB()));
            }
        }

        if (gpuTexture != null) {
            context.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(thumbnailX, thumbnailY, thumbnailWidth, thumbnailHeight)
                            .setTexture(gpuTexture)
                            .setRadius(5.0F)
                            .setColor(
                                    scaleAlpha(
                                            Color.WHITE,
                                            animationState.getAppearProgress()
                                                    * animationState.getTextureFadeProgress())));
        }

        float badgeWidth = bodyFont.getStringWidth(this.pack.getResolution()) + 10.0F;
        RoundedRectShape badgeShape =
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX() + this.getWidth() - badgeWidth - 9.0F,
                                this.getY() + 9.0F,
                                badgeWidth,
                                14.0F)
                        .setRadius(5.0F);

        context.drawRect(
                badgeShape.setColor(
                        scaleAlpha(
                                new Color(10, 10, 10, 190), animationState.getAppearProgress())));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(this.pack.getResolution())
                        .setPosition(
                                this.getX() + this.getWidth() - badgeWidth - 9.0F,
                                this.getY() + 9.0F)
                        .centerInBox(badgeWidth, 14.0F)
                        .setFont(bodyFont.getMsdfFont())
                        .setFontSize(bodyFont.getSize())
                        .enableShadow()
                        .setColor(
                                scaleAlpha(Color.WHITE, animationState.getAppearProgress())
                                        .getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(
                                titleFont.truncateWithEllipsis(
                                        this.pack.getName(), this.getWidth() - 16.0F))
                        .setPosition(this.getX() + 8.0F, this.getY() + thumbnailHeight + 10.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(
                                scaleAlpha(
                                                PackBrowserPaletteColors.TEXT,
                                                animationState.getAppearProgress())
                                        .getRGB()));
        TextDrawRequest subtitleRequest = TextDrawRequest.obtain();
        String author = this.pack.getAuthor();
        String downloads = this.pack.getDownloads();

        context.drawText(
                subtitleRequest
                        .setText(
                                bodyFont.truncateWithEllipsis(
                                        "by " + author + "  ·  " + downloads + " downloads",
                                        this.getWidth() - 16.0F))
                        .setPosition(this.getX() + 8.0F, this.getY() + thumbnailHeight + 23.0F)
                        .setFont(bodyFont.getMsdfFont())
                        .setFontSize(bodyFont.getSize())
                        .enableShadow()
                        .setColor(
                                scaleAlpha(
                                                PackBrowserPaletteColors.TEXT_MUTED,
                                                animationState.getAppearProgress())
                                        .getRGB()));
        if (this.selected) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + 7.0F,
                                    this.getY() + this.getHeight() - 2.0F,
                                    this.getWidth() - 14.0F,
                                    1.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setLinearGradient(
                                                    scaleAlpha(
                                                                    color,
                                                                    animationState
                                                                            .getAppearProgress())
                                                            .getRGB(),
                                                    scaleAlpha(
                                                                    color2,
                                                                    animationState
                                                                            .getAppearProgress())
                                                            .getRGB(),
                                                    this.getX(),
                                                    this.getY(),
                                                    this.getX() + this.getWidth(),
                                                    this.getY())));
        }
    }

    private static Color scaleAlpha(Color color, float alphaFactor) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.round(color.getAlpha() * alphaFactor));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0
                && RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())) {
            this.onClick.run();
        }
    }

    @Generated
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
