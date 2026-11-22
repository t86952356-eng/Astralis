package astralis.ui.component;

import astralis.Astralis;
import astralis.ui.animation.Animation;
import astralis.ui.screen.FriendsScreen;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class SocialTab extends SocialComponent {
    protected final SocialTextField searchField;
    protected final FriendChatPanel chatPanel;
    protected float paneX;
    protected float paneY;
    protected float paneWidth;
    protected float paneHeight;
    protected float scrollTarget;
    protected float scrollOffset;
    protected float contentHeight;
    private final String label;
    private final boolean searchEnabled;
    private final Animation scrollAnimation;
    private float thumbGrabOffset;
    private boolean active;
    private boolean draggingScrollbar;

    protected SocialTab(
            FriendsScreen screen,
            String label,
            boolean searchEnabled,
            SocialTextField searchField,
            FriendChatPanel chatPanel) {
        super(screen);
        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 240L);
        this.label = label;
        this.searchEnabled = searchEnabled;
        this.searchField = searchField;
        this.chatPanel = chatPanel;
    }

    public void setPaneBounds(float paneX, float paneY, float paneWidth, float paneHeight) {
        this.paneX = paneX;
        this.paneY = paneY;
        this.paneWidth = paneWidth;
        this.paneHeight = paneHeight;
    }

    @Override
    public void mouseDragged(float mouseX, float mouseY) {
        if (this.active) {
            this.chatPanel.setBounds(this.paneX, this.paneY, this.paneWidth, this.paneHeight);
            this.chatPanel.mouseDragged(mouseX, mouseY);
            if (this.draggingScrollbar) {
                this.scrollTarget =
                        this.getScrollOffsetForThumbDrag(
                                mouseY,
                                this.thumbGrabOffset,
                                this.paneY + 32.0F,
                                this.paneHeight - 38.0F,
                                this.contentHeight);
            }

            this.scrollTarget = Math.clamp(this.scrollTarget, this.getMinScrollOffset(), 0.0F);
            this.scrollAnimation.animateTo(this.scrollTarget);
            if (this.draggingScrollbar) {
                this.scrollAnimation.setValue(this.scrollTarget);
            }

            this.scrollOffset = (float) this.scrollAnimation.getValue();
            if (this.searchEnabled) {
                this.searchField.setBounds(
                        this.paneX + this.getListWidth() - 146.0F,
                        this.paneY + 4.0F,
                        136.0F,
                        20.0F);
            }
        }
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        SizedFont font = FontManager.getFont("Product Sans Medium", 7.0F);
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        String tabLabel = this.getTabLabel();
        boolean hovered = this.isMouseOverTab(mouseX, mouseY);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                        .setRadius(5.0F)
                        .setColor(
                                this.active
                                        ? FlatUiPaletteColors.CONTROL_ACTIVE
                                        : (hovered
                                                ? FlatUiPaletteColors.CONTROL_HOVER
                                                : FlatUiPaletteColors.CONTROL)));
        if (this.active) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX(),
                                    this.getY() + this.getHeight() - 2.0F,
                                    this.getWidth(),
                                    1.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setLinearGradient(
                                                    color.getRGB(),
                                                    color2.getRGB(),
                                                    this.getX(),
                                                    this.getY(),
                                                    this.getX() + this.getWidth(),
                                                    this.getY())));
        }

        if (this.active) {
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(tabLabel)
                            .setPosition(this.getX(), this.getY())
                            .centerInBox(this.getWidth(), this.getHeight())
                            .setFont(font.getMsdfFont())
                            .setFontSize(font.getSize())
                            .enableShadow()
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setLinearGradient(
                                                    ColorUtil.waveBlend(
                                                            this.getX(),
                                                            color.getRGB(),
                                                            color2.getRGB()),
                                                    ColorUtil.waveBlend(
                                                            this.getX() + this.getWidth(),
                                                            color.getRGB(),
                                                            color2.getRGB()),
                                                    this.getX(),
                                                    this.getY(),
                                                    this.getX() + this.getWidth(),
                                                    this.getY())));
        } else {
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(tabLabel)
                            .setPosition(this.getX(), this.getY())
                            .centerInBox(this.getWidth(), this.getHeight())
                            .setFont(font.getMsdfFont())
                            .setFontSize(font.getSize())
                            .enableShadow()
                            .setColor(
                                    hovered
                                            ? FlatUiPaletteColors.TEXT.getRGB()
                                            : FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        }

        if (this.active) {
            this.renderContent(context, mouseX, mouseY);
            this.chatPanel.render(context, mouseX, mouseY);
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (this.isMouseOverTab(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.socialScreen.selectTab((Class<? extends SocialTab>) this.getClass());
        } else if (this.active) {
            if (this.searchEnabled) {
                this.searchField.click(mouseButtonEvent);
            }

            float scrollbarX = this.paneX + this.getListWidth() - 5.0F;
            float trackY = this.paneY + 32.0F;
            float trackHeight = this.paneHeight - 38.0F;
            if (this.contentHeight > trackHeight
                    && RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            scrollbarX - 4.0F,
                            trackY,
                            10.0F,
                            trackHeight)) {
                float thumbHeight = this.getScrollThumbHeight(trackHeight, this.contentHeight);
                float thumbY =
                        this.getScrollThumbY(
                                trackY, trackHeight, this.contentHeight, this.scrollOffset);
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
                                trackY,
                                trackHeight,
                                this.contentHeight);
                this.scrollAnimation.setValue(this.scrollTarget);
            } else {
                this.clickContent(mouseButtonEvent);
                this.chatPanel.click(mouseButtonEvent);
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingScrollbar = false;
        this.releaseContent(mouseButtonEvent);
        this.chatPanel.release(mouseButtonEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active) {
            return false;
        }

        if (this.chatPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (!RenderUtil.isInsideRect(
                mouseX,
                mouseY,
                this.paneX,
                this.paneY + 28.0F,
                this.getListWidth(),
                this.paneHeight - 32.0F)) {
            return false;
        }

        this.scrollTarget =
                Math.clamp(
                        this.scrollTarget + (float) scrollY * 22.0F,
                        this.getMinScrollOffset(),
                        0.0F);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.active) {
            return false;
        } else if (this.searchEnabled && this.searchField.keyPressed(keyEvent)) {
            return true;
        } else {
            return this.contentKeyPressed(keyEvent) ? true : this.chatPanel.keyPressed(keyEvent);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.active) {
            return false;
        } else if (this.searchEnabled && this.searchField.charTyped(characterEvent)) {
            return true;
        } else {
            return this.contentCharTyped(characterEvent)
                    ? true
                    : this.chatPanel.charTyped(characterEvent);
        }
    }

    public void activate() {
        this.active = true;
        this.scrollTarget = 0.0F;
    }

    public void deactivate() {
        this.active = false;
        this.draggingScrollbar = false;
        this.searchField.setFocused(false);
        this.chatPanel.closeChat();
        this.onDeactivated();
    }

    public float getTabWidth(SizedFont font) {
        return font.getStringWidth(this.getTabLabel()) + 14.0F;
    }

    public boolean isMouseOverTab(double mouseX, double mouseY) {
        return RenderUtil.isInsideRect(
                mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected float getListWidth() {
        return this.paneWidth - 340.0F * this.chatPanel.getOpenProgress();
    }

    protected void drawPaneHeader(
            Render2DContext context, String title, SizedFont font, float mouseX, float mouseY) {
        float listWidth = this.getListWidth();
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.paneX, this.paneY, listWidth, this.paneHeight)
                        .setCornerRadii(
                                0.0F,
                                0.0F,
                                this.chatPanel.getOpenProgress() <= 0.01F ? 8.0F : 0.0F,
                                8.0F)
                        .setColor(FlatUiPaletteColors.BACKGROUND_DIM));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.paneX, this.paneY, listWidth, 28.0F)
                        .setColor(FlatUiPaletteColors.PANEL));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(title)
                        .setPosition(this.paneX + 10.0F, this.paneY)
                        .centerVertically(28.0F)
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                ColorUtil.waveBlend(
                                                        this.paneX + 10.0F,
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                ColorUtil.waveBlend(
                                                        this.paneX
                                                                + 10.0F
                                                                + font.getStringWidth(title),
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                this.paneX + 10.0F,
                                                this.paneY,
                                                this.paneX + 10.0F + font.getStringWidth(title),
                                                this.paneY)));
        if (this.searchEnabled) {
            this.searchField.render(context, mouseX, mouseY);
        }
    }

    protected void drawPaneScrollbar(Render2DContext context) {
        this.drawScrollbar(
                context,
                this.paneX + this.getListWidth() - 5.0F,
                this.paneY + 32.0F,
                this.paneHeight - 38.0F,
                this.contentHeight,
                this.scrollOffset,
                Astralis.getInstance().getPrimaryColor());
    }

    protected void drawStatusMessage(Render2DContext context) {
        SizedFont font;
        Color color;
        label40:
        {
            font = FontManager.getFont("Product Sans Regular", 7.0F);
            String notReadyMessage = this.socialScreen.getStatusMessage();

            if (!notReadyMessage.contains("not ready")) {
                String confirmMessage = this.socialScreen.getStatusMessage();

                if (!confirmMessage.contains("confirm")) {
                    String removedMessage = this.socialScreen.getStatusMessage();

                    if (!removedMessage.startsWith("Removed")) {
                        String declinedMessage = this.socialScreen.getStatusMessage();

                        if (!declinedMessage.startsWith("Declined")) {
                            String sentMessage = this.socialScreen.getStatusMessage();

                            if (!sentMessage.startsWith("Sent")) {
                                String loadedMessage = this.socialScreen.getStatusMessage();

                                if (!loadedMessage.startsWith("Loaded")) {
                                    String acceptMessage = this.socialScreen.getStatusMessage();

                                    if (!acceptMessage.startsWith("Accept")) {
                                        color = FlatUiPaletteColors.TEXT_MUTED;
                                        break label40;
                                    }
                                }
                            }

                            color = FlatUiPaletteColors.SUCCESS;
                            break label40;
                        }
                    }

                    color = FlatUiPaletteColors.ERROR;
                    break label40;
                }
            }

            color = FlatUiPaletteColors.WARNING;
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(
                                font.truncateWithEllipsis(
                                        this.socialScreen.getStatusMessage(),
                                        this.getListWidth() - 20.0F))
                        .setPosition(this.paneX + 10.0F, this.paneY + this.paneHeight - 16.0F)
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor(color.getRGB()));
    }

    protected List<String> filterBySearch(List<String> names) {
        if (this.searchField.getText().isBlank()) {
            return new ArrayList<>(names);
        }

        String query = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        ArrayList matches = new ArrayList();

        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(name);
            }
        }

        return matches;
    }

    protected void setContentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
        this.scrollTarget = Math.clamp(this.scrollTarget, this.getMinScrollOffset(), 0.0F);
    }

    protected void releaseContent(MouseButtonEvent mouseButtonEvent) {}

    protected boolean contentKeyPressed(KeyEvent keyEvent) {
        return false;
    }

    protected boolean contentCharTyped(CharacterEvent characterEvent) {
        return false;
    }

    protected void onDeactivated() {}

    protected abstract int getBadgeCount();

    protected abstract void renderContent(Render2DContext context, float mouseX, float mouseY);

    protected abstract void clickContent(MouseButtonEvent mouseButtonEvent);

    private String getTabLabel() {
        return this.getBadgeCount() < 0 ? this.label : this.label + " " + this.getBadgeCount();
    }

    private float getMinScrollOffset() {
        return Math.min(0.0F, this.paneHeight - 38.0F - this.contentHeight);
    }
}
