package astralis.ui.component;

import astralis.Astralis;
import astralis.social.friends.FriendManager;
import astralis.ui.animation.Animation;
import astralis.ui.screen.FriendsScreen;
import astralis.util.Easing;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendChatPanel extends SocialComponent {
    private final SocialTextField messageField;
    private final SocialButton sendButton;
    private final SocialButton removeButton;
    private final List<ChatMessageBubble> messageBubbles;
    private final Animation openAnimation;
    private final Animation scrollAnimation;
    private List<String> rawMessages;
    private String selectedFriend;
    private String pendingRemovalFriend;
    private String lastLoadedFriend;
    private String lastLayoutFriend;
    private float openProgress;
    private float scrollTarget;
    private float scrollOffset;
    private float contentHeight;
    private float thumbGrabOffset;
    private int lastMessageCount;
    private boolean open;
    private boolean draggingScrollbar;
    private boolean forceScrollToBottom;
    private boolean stickToBottom;

    public FriendChatPanel(FriendsScreen screen) {
        super(screen);
        this.messageBubbles = new ArrayList<>();
        this.openAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 260L);
        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 240L);
        this.rawMessages = List.of();
        this.lastMessageCount = -1;
        this.messageField =
                new SocialTextField(screen, "message", 160, "Product Sans Regular", 8.0F);
        this.messageField.setOnSubmit(this::sendMessage);
        this.sendButton =
                new SocialButton(
                        screen,
                        "Send",
                        60.0F,
                        24.0F,
                        8.0F,
                        SocialButtonStyle.PRIMARY,
                        TEXT,
                        this::sendMessage);
        this.removeButton =
                new SocialButton(
                        screen,
                        "x",
                        17.0F,
                        17.0F,
                        8.0F,
                        SocialButtonStyle.ICON,
                        ERROR,
                        () -> {
                            if (this.selectedFriend != null) {
                                if (!this.selectedFriend.equals(this.pendingRemovalFriend)) {
                                    this.pendingRemovalFriend = this.selectedFriend;
                                    screen.setStatusMessage("Click remove again to confirm");
                                } else {
                                    astralis.OfflineServices.unavailable(
                                            "Astralis social services");
                                    String removedFriend = this.selectedFriend;
                                    screen.setStatusMessage("Removed " + removedFriend);
                                    this.pendingRemovalFriend = null;
                                    this.selectedFriend = null;
                                    this.open = false;
                                    this.stickToBottom = false;
                                    astralis.OfflineServices.unavailable(
                                            "Astralis social services");
                                }
                            }
                        });
    }

    @Override
    public void mouseDragged(float mouseX, float mouseY) {
        float rightEdge = this.getX() + this.getWidth();
        float y = this.getY();
        float height = this.getHeight();
        this.openAnimation.animateTo(this.open ? 1.0 : 0.0);
        this.openProgress = (float) this.openAnimation.getValue();
        this.setBounds(rightEdge - 332.0F * this.openProgress, y, 332.0F, height);
        FriendManager friendManager = Astralis.getInstance().getFriendManager();
        if (this.selectedFriend != null
                && !this.containsIgnoreCase(friendManager.getFriends(), this.selectedFriend)) {
            this.selectedFriend = null;
            this.pendingRemovalFriend = null;
            this.lastLayoutFriend = null;
            this.lastMessageCount = -1;
            this.rawMessages = List.of();
            this.messageBubbles.clear();
            this.open = false;
            this.draggingScrollbar = false;
            this.stickToBottom = false;
        }

        List<String> messages =
                this.selectedFriend == null
                        ? List.of()
                        : new ArrayList<>(
                                friendManager
                                        .getChatHistory()
                                        .getOrDefault(this.selectedFriend, List.of()));
        if (!messages.equals(this.rawMessages)
                || !Objects.equals(this.lastLayoutFriend, this.selectedFriend)) {
            this.messageBubbles.clear();

            for (String message : messages) {
                this.messageBubbles.add(
                        new ChatMessageBubble(this.socialScreen, this.selectedFriend, message));
            }

            this.rawMessages = messages;
        }

        SizedFont font = FontManager.getFont("Product Sans Regular", 8.0F);
        float minScrollOffset = this.getMinScrollOffset();
        this.contentHeight = 10.0F;
        String lastDateText = "";

        for (ChatMessageBubble bubble : this.messageBubbles) {
            bubble.setShowDateHeader(
                    !bubble.getDateText().isEmpty() && !bubble.getDateText().equals(lastDateText));
            if (bubble.isShowDateHeader()) {
                lastDateText = bubble.getDateText();
            }

            this.contentHeight =
                    this.contentHeight
                            + (bubble.layoutHeight(font, this.getWidth() - 24.0F) + 8.0F);
        }

        boolean friendChanged = !Objects.equals(this.lastLayoutFriend, this.selectedFriend);
        boolean messageCountChanged = this.lastMessageCount != this.messageBubbles.size();
        boolean nearBottom =
                Math.abs(this.scrollTarget - minScrollOffset) <= 1.0F
                        || Math.abs(this.scrollOffset - minScrollOffset) <= 1.0F;
        if (!this.forceScrollToBottom && !friendChanged) {
            if (!this.stickToBottom && (!messageCountChanged || !nearBottom)) {
                this.scrollTarget = Math.clamp(this.scrollTarget, this.getMinScrollOffset(), 0.0F);
            } else {
                this.scrollTarget = this.getMinScrollOffset();
                this.stickToBottom = true;
            }
        } else {
            this.scrollTarget = this.getMinScrollOffset();
            this.scrollAnimation.setValue(this.scrollTarget);
            this.scrollOffset = this.scrollTarget;
            this.forceScrollToBottom = false;
            this.stickToBottom = true;
        }

        if (this.draggingScrollbar) {
            this.scrollTarget =
                    this.getScrollOffsetForThumbDrag(
                            mouseY,
                            this.thumbGrabOffset,
                            this.getY() + 70.0F,
                            this.getHeight() - 116.0F,
                            this.contentHeight);
            this.scrollAnimation.setValue(this.scrollTarget);
        }

        this.scrollTarget = Math.clamp(this.scrollTarget, this.getMinScrollOffset(), 0.0F);
        this.scrollAnimation.animateTo(this.scrollTarget);
        this.scrollOffset = (float) this.scrollAnimation.getValue();
        this.lastLayoutFriend = this.selectedFriend;
        this.lastMessageCount = this.messageBubbles.size();
        SocialTextField field = this.messageField;
        String placeholder;
        if (this.selectedFriend == null) {
            placeholder = "message";
        } else {
            placeholder = this.selectedFriend;
            placeholder = "message " + placeholder;
        }

        field.setPlaceholder(placeholder);
        this.messageField.setBounds(
                this.getX() + 12.0F,
                this.getY() + this.getHeight() - 35.0F,
                this.getWidth() - 92.0F,
                24.0F);
        this.sendButton.setBounds(
                this.getX() + this.getWidth() - 72.0F,
                this.getY() + this.getHeight() - 35.0F,
                60.0F,
                24.0F);
        this.removeButton.setBounds(
                this.getX() + this.getWidth() - 27.0F, this.getY() + 14.0F, 17.0F, 17.0F);
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {
        if (!(this.openProgress <= 0.01F)) {
            SizedFont titleFont = FontManager.getFont("Product Sans Bold", 9.0F);
            SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 8.0F);
            SizedFont smallFont = FontManager.getFont("Product Sans Regular", 7.0F);
            Color color = Astralis.getInstance().getPrimaryColor();
            Color color2 = Astralis.getInstance().getSecondaryColor();
            context.beginScissor(
                    this.getX(), this.getY(), 332.0F * this.openProgress, this.getHeight());
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setCornerRadii(0.0F, 0.0F, 8.0F, 0.0F)
                            .setColor(BACKGROUND_DIM));
            if (this.selectedFriend == null) {
                String heading = "Friends Hub";
                String subtitle = "Select a friend to open chat history.";
                float centerX = this.getX() + this.getWidth() / 2.0F;
                context.drawText(
                        TextDrawRequest.obtain()
                                .setText("Friends Hub")
                                .setPosition(
                                        centerX - titleFont.getStringWidth("Friends Hub") / 2.0F,
                                        this.getY() + 92.0F)
                                .setFont(titleFont.getMsdfFont())
                                .setFontSize(titleFont.getSize())
                                .enableShadow()
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setLinearGradient(
                                                        color.getRGB(),
                                                        color2.getRGB(),
                                                        centerX
                                                                - titleFont.getStringWidth(
                                                                                "Friends Hub")
                                                                        / 2.0F,
                                                        this.getY() + 92.0F,
                                                        centerX
                                                                + titleFont.getStringWidth(
                                                                                "Friends Hub")
                                                                        / 2.0F,
                                                        this.getY() + 92.0F)));
                context.drawText(
                        TextDrawRequest.obtain()
                                .setText("Select a friend to open chat history.")
                                .setPosition(
                                        centerX
                                                - bodyFont.getStringWidth(
                                                                "Select a friend to open chat history.")
                                                        / 2.0F,
                                        this.getY() + 112.0F)
                                .setFont(bodyFont.getMsdfFont())
                                .setFontSize(bodyFont.getSize())
                                .enableShadow()
                                .setColor(TEXT_SECONDARY.getRGB()));
                String[] statLabels = new String[] {"Friends", "Online"};
                int[] statValues =
                        new int[] {
                            Astralis.getInstance().getFriendManager().getFriends().size(),
                            Astralis.getInstance().getFriendManager().getOnlineFriends().size()
                        };

                for (int i = 0; i < statLabels.length; i++) {
                    float cardX = centerX + (i == 0 ? -82.0F : 8.0F);
                    context.drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(cardX, this.getY() + 145.0F, 74.0F, 46.0F)
                                    .setRadius(7.0F)
                                    .setColor(CONTROL));
                    context.drawText(
                            TextDrawRequest.obtain()
                                    .setText(String.valueOf(statValues[i]))
                                    .setPosition(cardX, this.getY() + 153.0F)
                                    .centerHorizontally(74.0F)
                                    .setFont(smallFont.getMsdfFont())
                                    .setFontSize(12.0F)
                                    .enableShadow()
                                    .setColor(TEXT.getRGB()));
                    context.drawText(
                            TextDrawRequest.obtain()
                                    .setText(statLabels[i])
                                    .setPosition(cardX, this.getY() + 172.0F)
                                    .centerHorizontally(74.0F)
                                    .setFont(smallFont.getMsdfFont())
                                    .setFontSize(smallFont.getSize())
                                    .enableShadow()
                                    .setColor(TEXT_MUTED.getRGB()));
                }

                context.endScissor();
            } else {
                boolean online =
                        this.containsIgnoreCase(
                                Astralis.getInstance().getFriendManager().getOnlineFriends(),
                                this.selectedFriend);
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(this.getX(), this.getY(), this.getWidth(), 58.0F)
                                .setColor(PANEL));
                this.drawAvatar(
                        context,
                        this.selectedFriend,
                        this.getX() + 12.0F,
                        this.getY() + 11.0F,
                        36.0F,
                        titleFont);
                context.drawText(
                        TextDrawRequest.obtain()
                                .setText(
                                        titleFont.truncateWithEllipsis(
                                                this.selectedFriend, this.getWidth() - 170.0F))
                                .setPosition(this.getX() + 58.0F, this.getY() + 12.0F)
                                .setFont(titleFont.getMsdfFont())
                                .setFontSize(titleFont.getSize())
                                .enableShadow()
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setLinearGradient(
                                                        color.getRGB(),
                                                        color2.getRGB(),
                                                        this.getX() + 58.0F,
                                                        this.getY() + 12.0F,
                                                        this.getX() + 150.0F,
                                                        this.getY() + 12.0F)));
                String statusText = online ? "Online" : "Offline";
                Color color3 = online ? SUCCESS : TEXT_MUTED;
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 58.0F,
                                        this.getY() + 31.0F,
                                        smallFont.getStringWidth(statusText) + 12.0F,
                                        14.0F)
                                .setRadius(7.0F)
                                .setColor(
                                        new Color(
                                                color3.getRed(),
                                                color3.getGreen(),
                                                color3.getBlue(),
                                                36)));
                context.drawText(
                        TextDrawRequest.obtain()
                                .setText(statusText)
                                .setPosition(this.getX() + 58.0F, this.getY() + 31.0F)
                                .centerInBox(smallFont.getStringWidth(statusText) + 12.0F, 14.0F)
                                .setFont(smallFont.getMsdfFont())
                                .setFontSize(smallFont.getSize())
                                .enableShadow()
                                .setColor(color3.getRGB()));
                this.removeButton.render(context, mouseX, mouseY);
                float listY = this.getY() + 66.0F;
                float listHeight = this.getHeight() - 108.0F;
                context.beginScissor(
                        this.getX() + 6.0F, listY, this.getWidth() - 12.0F, listHeight);
                float bubbleY = listY + 4.0F + this.scrollOffset;

                for (ChatMessageBubble bubble : this.messageBubbles) {
                    bubble.setBounds(
                            this.getX() + 12.0F,
                            bubbleY,
                            this.getWidth() - 24.0F,
                            bubble.getHeight());
                    bubble.render(context, mouseX, mouseY);
                    bubbleY += bubble.getHeight() + 8.0F;
                }

                if (this.messageBubbles.isEmpty()) {
                    this.drawPlaceholderText(
                            context,
                            "No messages yet",
                            this.getX(),
                            listY,
                            this.getWidth(),
                            listHeight,
                            smallFont);
                }

                context.endScissor();
                this.drawScrollbar(
                        context,
                        this.getX() + this.getWidth() - 5.0F,
                        listY + 4.0F,
                        listHeight - 8.0F,
                        this.contentHeight,
                        this.scrollOffset,
                        color);
                this.messageField.render(context, mouseX, mouseY);
                this.sendButton.render(context, mouseX, mouseY);
                context.endScissor();
            }
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (!(this.openProgress < 0.95F) && this.selectedFriend != null) {
            this.messageField.click(mouseButtonEvent);
            float scrollbarX = this.getX() + this.getWidth() - 5.0F;
            float trackY = this.getY() + 70.0F;
            float trackHeight = this.getHeight() - 116.0F;
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
                this.stickToBottom = false;
                this.forceScrollToBottom = false;
                this.scrollTarget =
                        this.getScrollOffsetForThumbDrag(
                                (float) mouseButtonEvent.y(),
                                this.thumbGrabOffset,
                                trackY,
                                trackHeight,
                                this.contentHeight);
                this.scrollAnimation.setValue(this.scrollTarget);
            } else {
                this.removeButton.click(mouseButtonEvent);
                this.sendButton.click(mouseButtonEvent);
            }
        } else {
            this.messageField.setFocused(false);
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingScrollbar = false;
        this.messageField.release(mouseButtonEvent);
        this.sendButton.release(mouseButtonEvent);
        this.removeButton.release(mouseButtonEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.open
                && RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY() + 66.0F,
                        this.getWidth(),
                        this.getHeight() - 108.0F)) {
            this.stickToBottom = false;
            this.forceScrollToBottom = false;
            this.scrollTarget =
                    Math.clamp(
                            this.scrollTarget + (float) scrollY * 22.0F,
                            this.getMinScrollOffset(),
                            0.0F);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.messageField.keyPressed(event)) {
            return true;
        } else if (event.key() == 257 && this.open && this.selectedFriend != null) {
            this.messageField.setFocused(true);
            return true;
        } else if (event.isEscape() && this.open) {
            this.closeChat();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        return this.messageField.charTyped(characterEvent);
    }

    public void toggleChat(String friendName) {
        if (this.open && friendName.equalsIgnoreCase(this.selectedFriend)) {
            this.closeChat();
        } else {
            this.selectedFriend = friendName;
            this.open = true;
            this.pendingRemovalFriend = null;
            this.messageField.setFocused(false);
            this.messageField.setText("");
            this.scrollTarget = 0.0F;
            this.forceScrollToBottom = true;
            this.stickToBottom = true;
            this.rawMessages = List.of();
            if (!friendName.equals(this.lastLoadedFriend)) {
                astralis.OfflineServices.unavailable("Astralis social services");
                this.lastLoadedFriend = friendName;
                this.socialScreen.setStatusMessage("Loaded chat with " + friendName);
            }
        }
    }

    public void closeChat() {
        this.open = false;
        this.draggingScrollbar = false;
        this.stickToBottom = false;
        this.messageField.setFocused(false);
    }

    private void sendMessage() {
        if (this.selectedFriend != null) {
            if (this.messageField.getText().isBlank()) {
                this.messageField.setFocused(true);
            } else {
                astralis.OfflineServices.unavailable("Astralis social services");
                String friendName = this.selectedFriend;
                this.socialScreen.setStatusMessage("Message sent to " + friendName);
                this.messageField.setText("");
                this.messageField.setFocused(true);
                this.stickToBottom = true;
            }
        }
    }

    private float getMinScrollOffset() {
        return Math.min(0.0F, this.getHeight() - 108.0F - this.contentHeight);
    }

    @Generated
    public String getSelectedFriend() {
        return this.selectedFriend;
    }

    @Generated
    public float getOpenProgress() {
        return this.openProgress;
    }
}
