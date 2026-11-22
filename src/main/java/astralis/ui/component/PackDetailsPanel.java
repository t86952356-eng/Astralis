package astralis.ui.component;

import astralis.Astralis;
import astralis.resourcepack.PackThumbnailCache;
import astralis.resourcepack.PvprpPack;
import astralis.ui.animation.Animation;
import astralis.ui.screen.PackBrowserScreen;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
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
import net.minecraft.util.Util;

import java.awt.Color;

public class PackDetailsPanel extends PackBrowserComponent {
    private final PackThumbnailCache thumbnails;
    private final PackBrowserButton downloadButton;
    private final PackBrowserButton pageButton;
    private final PackBrowserButton closeButton;
    private final Animation openAnimation;
    private PvprpPack pack;
    private float openProgress;
    private boolean open;

    public PackDetailsPanel(PackBrowserScreen screen, PackThumbnailCache thumbnails) {
        super(screen);
        this.openAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 260L);
        this.thumbnails = thumbnails;
        this.downloadButton =
                new PackBrowserButton(
                        screen,
                        () -> {
                            String label;
                            if (this.pack == null) {

                                label = "Download";
                            } else {
                                label = screen.getDownloadButtonLabel(this.pack);
                            }

                            return label;
                        },
                        118.0F,
                        24.0F,
                        7.0F,
                        PackBrowserButtonStyle.PRIMARY,
                        Color.WHITE,
                        () -> this.pack != null && screen.canDownload(this.pack),
                        () -> {
                            if (this.pack != null) {
                                screen.downloadPack(this.pack);
                            }
                        });
        this.pageButton =
                new PackBrowserButton(
                        screen,
                        () -> "PVPRP page",
                        92.0F,
                        24.0F,
                        7.0F,
                        PackBrowserButtonStyle.ICON,
                        PackBrowserPaletteColors.TEXT_SECONDARY,
                        () -> this.pack != null,
                        () -> {
                            if (this.pack != null) {
                                Util.getPlatform().openUri(this.pack.getPageUrl());
                            }
                        });
        this.closeButton =
                new PackBrowserButton(
                        screen,
                        () -> "x",
                        17.0F,
                        17.0F,
                        8.0F,
                        PackBrowserButtonStyle.ICON,
                        PackBrowserPaletteColors.ERROR,
                        () -> this.pack != null,
                        this::closePanel);
    }

    @Override
    public void mouseDragged(float mouseX, float mouseY) {
        float rightEdge = this.getX() + this.getWidth();
        float panelY = this.getY();
        float panelHeight = this.getHeight();
        this.openAnimation.animateTo(this.open ? 1.0 : 0.0);
        this.openProgress = (float) this.openAnimation.getValue();
        this.setBounds(rightEdge - 250.0F * this.openProgress, panelY, 250.0F, panelHeight);
        this.closeButton.setBounds(
                this.getX() + this.getWidth() - 24.0F, this.getY() + 6.0F, 17.0F, 17.0F);
        this.downloadButton.setBounds(
                this.getX() + 10.0F, this.getY() + this.getHeight() - 34.0F, 118.0F, 24.0F);
        this.pageButton.setBounds(
                this.getX() + this.getWidth() - 102.0F,
                this.getY() + this.getHeight() - 34.0F,
                92.0F,
                24.0F);
        if (!this.open && this.openProgress <= 0.01F) {
            this.pack = null;
        }
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        if (this.pack != null && !(this.openProgress <= 0.01F)) {

            SizedFont titleFont = FontManager.getFont("Product Sans Bold", 10.0F);

            SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 7.0F);

            SizedFont smallFont = FontManager.getFont("Product Sans Regular", 6.5F);
            Color color = Astralis.getInstance().getPrimaryColor();
            Color color2 = Astralis.getInstance().getSecondaryColor();
            context.beginScissor(
                    this.getX(),
                    this.getY(),
                    this.getWidth() * this.openProgress,
                    this.getHeight());
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setCornerRadii(0.0F, 0.0F, 8.0F, 0.0F)
                            .setColor(PackBrowserPaletteColors.BACKGROUND));
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), 28.0F)
                            .setColor(PackBrowserPaletteColors.PANEL));
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), 1.0F, this.getHeight())
                            .setColor(PackBrowserPaletteColors.DIVIDER));
            TextDrawRequest titleRequest = TextDrawRequest.obtain();

            TextDrawRequest titleRequestStyled =
                    titleRequest
                            .setText("Pack details")
                            .setPosition(this.getX() + 10.0F, this.getY())
                            .centerVertically(28.0F)
                            .setFont(titleFont.getMsdfFont())
                            .setFontSize(titleFont.getSize())
                            .enableShadow();
            ShapePaint titlePaint = ShapePaint.obtain();
            int titleStartColor =
                    ColorUtil.waveBlend(this.getX() + 10.0F, color.getRGB(), color2.getRGB());
            float titleX = this.getX() + 10.0F;

            int titleEndColor =
                    ColorUtil.waveBlend(
                            titleX + titleFont.getStringWidth("Pack details"),
                            color.getRGB(),
                            color2.getRGB());
            float gradientStartX = this.getX() + 10.0F;
            float gradientStartY = this.getY();
            float gradientEndBaseX = this.getX() + 10.0F;

            context.drawText(
                    titleRequestStyled.setPaint(
                            titlePaint.setLinearGradient(
                                    titleStartColor,
                                    titleEndColor,
                                    gradientStartX,
                                    gradientStartY,
                                    gradientEndBaseX + titleFont.getStringWidth("Pack details"),
                                    this.getY())));
            this.closeButton.render(context, mouseX, mouseY);
            GpuTexture gpuTexture = this.thumbnails.get(this.pack.getIconUrl());
            if (gpuTexture == null) {
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 10.0F,
                                        this.getY() + 38.0F,
                                        this.getWidth() - 20.0F,
                                        129.0F)
                                .setRadius(6.0F)
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setLinearGradient(
                                                        color.getRGB(),
                                                        color2.getRGB(),
                                                        this.getX() + 10.0F,
                                                        this.getY() + 38.0F,
                                                        this.getX() + this.getWidth() - 10.0F,
                                                        this.getY() + 167.0F)));
            } else {
                context.drawTexture(
                        TexturedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 10.0F,
                                        this.getY() + 38.0F,
                                        this.getWidth() - 20.0F,
                                        129.0F)
                                .setTexture(gpuTexture)
                                .setRadius(6.0F)
                                .setColor(Color.WHITE));
            }

            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(
                                    titleFont.truncateWithEllipsis(
                                            this.pack.getName(), this.getWidth() - 20.0F))
                            .setPosition(this.getX() + 10.0F, this.getY() + 177.0F)
                            .setFont(titleFont.getMsdfFont())
                            .setFontSize(titleFont.getSize())
                            .enableShadow()
                            .setColor(PackBrowserPaletteColors.TEXT.getRGB()));
            TextDrawRequest authorRequest = TextDrawRequest.obtain();
            String author = this.pack.getAuthor();
            context.drawText(
                    authorRequest
                            .setText(
                                    bodyFont.truncateWithEllipsis(
                                            "by " + author, this.getWidth() - 20.0F))
                            .setPosition(this.getX() + 10.0F, this.getY() + 193.0F)
                            .setFont(bodyFont.getMsdfFont())
                            .setFontSize(bodyFont.getSize())
                            .enableShadow()
                            .setColor(PackBrowserPaletteColors.TEXT_SECONDARY.getRGB()));

            this.drawStatBox(
                    context,
                    "Resolution",
                    this.pack.getResolution(),
                    this.getX() + 10.0F,
                    this.getY() + 215.0F,
                    70.0F,
                    bodyFont,
                    smallFont);

            this.drawStatBox(
                    context,
                    "Views",
                    this.pack.getViews(),
                    this.getX() + 87.0F,
                    this.getY() + 215.0F,
                    70.0F,
                    bodyFont,
                    smallFont);

            this.drawStatBox(
                    context,
                    "Downloads",
                    this.pack.getDownloads(),
                    this.getX() + 164.0F,
                    this.getY() + 215.0F,
                    76.0F,
                    bodyFont,
                    smallFont);
            TextDrawRequest disclaimerRequest = TextDrawRequest.obtain();

            context.drawText(
                    disclaimerRequest
                            .setText("Pack files and artwork belong to their creators.")
                            .setPosition(this.getX() + 10.0F, this.getY() + 263.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .enableShadow()
                            .setColor(PackBrowserPaletteColors.TEXT_MUTED.getRGB()));
            TextDrawRequest attributionRequest = TextDrawRequest.obtain();

            context.drawText(
                    attributionRequest
                            .setText("Open the PVPRP page for full attribution.")
                            .setPosition(this.getX() + 10.0F, this.getY() + 274.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .enableShadow()
                            .setColor(PackBrowserPaletteColors.TEXT_MUTED.getRGB()));
            if (this.packBrowserScreen.isDownloading(this.pack)) {
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 10.0F,
                                        this.getY() + this.getHeight() - 45.0F,
                                        this.getWidth() - 20.0F,
                                        3.0F)
                                .setRadius(1.5F)
                                .setColor(PackBrowserPaletteColors.OVERLAY));
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 10.0F,
                                        this.getY() + this.getHeight() - 45.0F,
                                        (this.getWidth() - 20.0F)
                                                * this.packBrowserScreen.getDownloadProgress(
                                                        this.pack),
                                        3.0F)
                                .setRadius(1.5F)
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setLinearGradient(
                                                        color.getRGB(),
                                                        color2.getRGB(),
                                                        this.getX() + 10.0F,
                                                        this.getY(),
                                                        this.getX() + this.getWidth() - 10.0F,
                                                        this.getY())));
            }

            this.downloadButton.render(context, mouseX, mouseY);
            this.pageButton.render(context, mouseX, mouseY);
            context.endScissor();
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (this.open && this.pack != null) {
            this.closeButton.click(mouseButtonEvent);
            this.downloadButton.click(mouseButtonEvent);
            this.pageButton.click(mouseButtonEvent);
        }
    }

    public void showPack(PvprpPack pack) {
        this.pack = pack;
        this.open = true;
    }

    public void closePanel() {
        this.open = false;
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY) {
        return this.openProgress > 0.01F
                && RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth() * this.openProgress,
                        this.getHeight());
    }

    private void drawStatBox(
            Render2DContext context,
            String label,
            String value,
            float x,
            float y,
            float width,
            SizedFont valueFont,
            SizedFont labelFont) {
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, width, 38.0F)
                        .setRadius(6.0F)
                        .setColor(PackBrowserPaletteColors.CONTROL));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(valueFont.truncateWithEllipsis(value, width - 10.0F))
                        .setPosition(x, y + 6.0F)
                        .centerHorizontally(width)
                        .setFont(valueFont.getMsdfFont())
                        .setFontSize(valueFont.getSize())
                        .enableShadow()
                        .setColor(PackBrowserPaletteColors.TEXT.getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(x, y + 22.0F)
                        .centerHorizontally(width)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(PackBrowserPaletteColors.TEXT_MUTED.getRGB()));
    }

    @Generated
    public PvprpPack getPack() {
        return this.pack;
    }

    @Generated
    public float getOpenProgress() {
        return this.openProgress;
    }

    @Generated
    public boolean isOpen() {
        return this.open;
    }
}
