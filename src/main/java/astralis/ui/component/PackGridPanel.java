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
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PackGridPanel extends PackBrowserComponent {
    private final PackThumbnailCache thumbnails;
    private final PackDetailsPanel detailsPanel;
    private final List<PackCard> cards;
    private final Animation scrollAnimation;
    private List<PvprpPack> packs;
    private float listWidth;
    private float listY;
    private float listHeight;
    private float contentHeight;
    private float scrollTarget;
    private float scrollOffset;
    private float thumbGrabOffset;
    private int columnCount;
    private float rowStride;
    private boolean draggingScrollbar;

    public PackGridPanel(
            PackBrowserScreen screen,
            PackThumbnailCache thumbnails,
            PackDetailsPanel detailsPanel) {
        super(screen);
        this.cards = new ArrayList<>();
        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 240L);
        this.packs = List.of();
        this.thumbnails = thumbnails;
        this.detailsPanel = detailsPanel;
    }

    @Override
    public void mouseDragged(float mouseX, float mouseY) {
        this.syncCards();
        this.listWidth = this.getWidth() - 258.0F * this.detailsPanel.getOpenProgress();
        this.listY = this.getY() + 32.0F;
        this.listHeight = this.getHeight() - 56.0F;
        float gridWidth = this.listWidth - 16.0F;
        int columns = Math.clamp((int) ((gridWidth + 7.0F) / 174.0F), 1, 4);
        float cardWidth = (gridWidth - (columns - 1) * 7.0F) / columns;
        float cardHeight = (cardWidth - 10.0F) * 9.0F / 16.0F + 37.0F;
        if (this.columnCount > 0
                && (this.columnCount != columns
                        || Math.abs(this.rowStride - cardHeight - 7.0F) > 0.5F)) {
            float currentScroll = Math.max(0.0F, -this.scrollOffset);
            float targetScroll = Math.max(0.0F, -this.scrollTarget);
            int currentCardIndex = (int) (currentScroll / this.rowStride) * this.columnCount;
            int targetCardIndex = (int) (targetScroll / this.rowStride) * this.columnCount;
            this.scrollOffset =
                    -(currentCardIndex / columns * (cardHeight + 7.0F)
                            + currentScroll
                                    % this.rowStride
                                    * (cardHeight + 7.0F)
                                    / this.rowStride);
            this.scrollTarget =
                    -(targetCardIndex / columns * (cardHeight + 7.0F)
                            + targetScroll % this.rowStride * (cardHeight + 7.0F) / this.rowStride);
            this.scrollAnimation.setValue(this.scrollOffset);
        }

        this.columnCount = columns;
        this.rowStride = cardHeight + 7.0F;
        int rowCount = (this.cards.size() + columns - 1) / columns;
        this.contentHeight = rowCount == 0 ? 0.0F : rowCount * (cardHeight + 7.0F) + 1.0F;
        if (this.draggingScrollbar) {
            this.scrollTarget =
                    this.getScrollOffsetForThumbDrag(
                            mouseY,
                            this.thumbGrabOffset,
                            this.listY,
                            this.listHeight,
                            this.contentHeight);
        }

        this.scrollTarget = Math.clamp(this.scrollTarget, this.getMinScrollOffset(), 0.0F);
        this.scrollAnimation.animateTo(this.scrollTarget);
        if (this.draggingScrollbar) {
            this.scrollAnimation.setValue(this.scrollTarget);
        }

        this.scrollOffset = (float) this.scrollAnimation.getValue();
        if (this.packBrowserScreen.hasMorePages()
                && !this.packBrowserScreen.isLoading()
                && this.scrollTarget <= this.getMinScrollOffset() + 80.0F) {
            this.packBrowserScreen.loadNextPage();
        }

        for (int cardIndex = 0; cardIndex < this.cards.size(); cardIndex++) {
            int column = cardIndex % columns;
            int row = cardIndex / columns;
            this.cards
                    .get(cardIndex)
                    .setBounds(
                            this.getX() + 8.0F + column * (cardWidth + 7.0F),
                            this.listY + 3.0F + this.scrollOffset + row * (cardHeight + 7.0F),
                            cardWidth,
                            cardHeight);
        }
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 9.0F);

        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 7.0F);
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        String headerValue;
        if (this.packBrowserScreen.getPacks().isEmpty() && this.packBrowserScreen.isLoading()) {

            headerValue = "Loading packs...";
        } else {
            int packCount = this.packBrowserScreen.getPacks().size();
            String loadingSuffix;
            if (this.packBrowserScreen.isLoading()) {

                loadingSuffix = "  ·  Loading more...";
            } else {
                loadingSuffix = "";
            }

            headerValue = packCount + " packs" + loadingSuffix;
        }

        String headerText = headerValue;
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.listWidth, this.getHeight())
                        .setCornerRadii(
                                0.0F,
                                0.0F,
                                this.detailsPanel.getOpenProgress() <= 0.01F ? 8.0F : 0.0F,
                                8.0F)
                        .setColor(PackBrowserPaletteColors.BACKGROUND_DIM));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.listWidth, 28.0F)
                        .setColor(PackBrowserPaletteColors.PANEL));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(headerText)
                        .setPosition(this.getX() + 10.0F, this.getY())
                        .centerVertically(28.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                ColorUtil.waveBlend(
                                                        this.getX() + 10.0F,
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                ColorUtil.waveBlend(
                                                        this.getX()
                                                                + 10.0F
                                                                + titleFont.getStringWidth(
                                                                        headerText),
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                this.getX() + 10.0F,
                                                this.getY(),
                                                this.getX()
                                                        + 10.0F
                                                        + titleFont.getStringWidth(headerText),
                                                this.getY())));
        context.beginScissor(
                this.getX() + 3.0F, this.listY, this.listWidth - 6.0F, this.listHeight);
        PvprpPack selectedPack = this.detailsPanel.getPack();

        for (int cardIndex = 0; cardIndex < this.cards.size(); cardIndex++) {
            PackCard card = this.cards.get(cardIndex);
            card.setSelected(
                    selectedPack != null
                            && selectedPack
                                    .getPageUrl()
                                    .equals(this.packs.get(cardIndex).getPageUrl()));
            if (!(card.getY() + card.getHeight() < this.listY)
                    && !(card.getY() > this.listY + this.listHeight)) {
                card.render(context, mouseX, mouseY);
            }
        }

        if (this.cards.isEmpty()) {
            String emptyMessage;
            if (this.packBrowserScreen.isLoading()) {

                emptyMessage = "Fetching packs...";
            } else {

                emptyMessage = "No packs found";
            }

            this.drawPlaceholderText(
                    context,
                    emptyMessage,
                    this.getX(),
                    this.listY,
                    this.listWidth,
                    this.listHeight,
                    bodyFont);
        }

        context.endScissor();
        this.drawScrollbar(
                context,
                this.getX() + this.listWidth - 5.0F,
                this.listY,
                this.listHeight,
                this.contentHeight,
                this.scrollOffset,
                color);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX(),
                                this.getY() + this.getHeight() - 24.0F,
                                this.listWidth,
                                1.0F)
                        .setColor(PackBrowserPaletteColors.DIVIDER));
        String statusMessage = this.packBrowserScreen.getStatusMessage();

        Color color3;
        if (statusMessage.startsWith("Downloaded")) {
            color3 = PackBrowserPaletteColors.SUCCESS;
        } else {
            label66:
            {
                String failedStatus = this.packBrowserScreen.getStatusMessage();

                if (!failedStatus.startsWith("Failed")) {
                    String couldntStatus = this.packBrowserScreen.getStatusMessage();

                    if (!couldntStatus.startsWith("Couldn't")) {
                        color3 = PackBrowserPaletteColors.TEXT_MUTED;
                        break label66;
                    }
                }

                color3 = PackBrowserPaletteColors.ERROR;
            }
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(
                                bodyFont.truncateWithEllipsis(
                                        this.packBrowserScreen.getStatusMessage(),
                                        Math.max(20.0F, this.listWidth - 18.0F)))
                        .setPosition(this.getX() + 9.0F, this.getY() + this.getHeight() - 19.0F)
                        .setFont(bodyFont.getMsdfFont())
                        .setFontSize(bodyFont.getSize())
                        .enableShadow()
                        .setColor(color3.getRGB()));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        float scrollbarX = this.getX() + this.listWidth - 5.0F;
        if (this.contentHeight > this.listHeight
                && RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        scrollbarX - 4.0F,
                        this.listY,
                        10.0F,
                        this.listHeight)) {
            float thumbHeight = this.getScrollThumbHeight(this.listHeight, this.contentHeight);
            float thumbY =
                    this.getScrollThumbY(
                            this.listY, this.listHeight, this.contentHeight, this.scrollOffset);
            this.thumbGrabOffset =
                    RenderUtil.isInsideRect(
                                    mouseButtonEvent.x(),
                                    mouseButtonEvent.y(),
                                    scrollbarX - 4.0F,
                                    thumbY,
                                    10.0F,
                                    thumbHeight)
                            ? (float) mouseButtonEvent.y() - thumbY
                            : thumbHeight / 2.0F;
            this.draggingScrollbar = true;
            this.scrollTarget =
                    this.getScrollOffsetForThumbDrag(
                            (float) mouseButtonEvent.y(),
                            this.thumbGrabOffset,
                            this.listY,
                            this.listHeight,
                            this.contentHeight);
            this.scrollAnimation.setValue(this.scrollTarget);
        } else if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.listY,
                this.listWidth,
                this.listHeight)) {
            for (PackCard card : this.cards) {
                card.click(mouseButtonEvent);
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingScrollbar = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!RenderUtil.isInsideRect(
                mouseX, mouseY, this.getX(), this.listY, this.listWidth, this.listHeight)) {
            return false;
        }

        this.scrollTarget =
                Math.clamp(
                        this.scrollTarget + (float) scrollY * 24.0F,
                        this.getMinScrollOffset(),
                        0.0F);
        return true;
    }

    public boolean isMouseOverList(double mouseX, double mouseY) {
        return RenderUtil.isInsideRect(
                mouseX, mouseY, this.getX(), this.getY(), this.listWidth, this.getHeight());
    }

    private void syncCards() {
        List<PvprpPack> newPacks = this.packBrowserScreen.getPacks();
        if (!newPacks.equals(this.packs)) {
            boolean listExtended =
                    !this.packs.isEmpty()
                            && newPacks.size() >= this.packs.size()
                            && newPacks.subList(0, this.packs.size()).equals(this.packs);
            HashMap cardsByUrl = new HashMap();

            for (int cardIndex = 0;
                    cardIndex < Math.min(this.packs.size(), this.cards.size());
                    cardIndex++) {
                cardsByUrl.put(this.packs.get(cardIndex).getPageUrl(), this.cards.get(cardIndex));
            }

            this.cards.clear();

            for (int packIndex = 0; packIndex < newPacks.size(); packIndex++) {
                PvprpPack pack = (PvprpPack) newPacks.get(packIndex);
                PackCard existingCard = (PackCard) cardsByUrl.get(pack.getPageUrl());
                this.cards.add(
                        existingCard != null
                                ? existingCard
                                : new PackCard(
                                        this.packBrowserScreen,
                                        pack,
                                        this.thumbnails,
                                        () -> this.detailsPanel.showPack(pack),
                                        packIndex));
            }

            this.packs = newPacks;
            if (!listExtended) {
                this.scrollTarget = 0.0F;
                this.scrollAnimation.setValue(0.0);
            }

            PvprpPack selectedPack = this.detailsPanel.getPack();
            if (selectedPack != null
                    && newPacks.stream()
                            .noneMatch(
                                    listedPack ->
                                            listedPack
                                                    .getPageUrl()
                                                    .equals(selectedPack.getPageUrl()))) {
                this.detailsPanel.closePanel();
            }
        }
    }

    private float getMinScrollOffset() {
        return Math.min(0.0F, this.listHeight - this.contentHeight);
    }
}
