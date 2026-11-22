package astralis.ui.component;

import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class AddFriendTab extends SocialTab {
    private final SocialTextField usernameField;
    private final SocialButton sendButton;

    public AddFriendTab(
            FriendsScreen screen, SocialTextField searchField, FriendChatPanel chatPanel) {
        super(screen, "Add", false, searchField, chatPanel);
        this.usernameField =
                new SocialTextField(screen, "username", 32, "Product Sans Regular", 7.0F);
        this.usernameField.setOnSubmit(this::sendFriendRequest);
        this.sendButton =
                new SocialButton(
                        screen,
                        "Send Request",
                        256.0F,
                        23.0F,
                        8.0F,
                        SocialButtonStyle.PRIMARY,
                        FlatUiPaletteColors.TEXT,
                        this::sendFriendRequest);
    }

    protected int getBadgeCount() {
        return -1;
    }

    protected void renderContent(Render2DContext context, float mouseX, float mouseY) {
        SizedFont titleFont = FontManager.getFont("Product Sans Medium", 8.0F);
        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 7.0F);
        float cardX = this.paneX + (this.getListWidth() - 288.0F) / 2.0F;
        float cardY = this.paneY + (this.paneHeight - 116.0F) / 2.0F - 12.0F;
        this.drawPaneHeader(context, "Add Friend", titleFont, mouseX, mouseY);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(cardX, cardY, 288.0F, 116.0F)
                        .setRadius(8.0F)
                        .setColor(FlatUiPaletteColors.CONTROL));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText("Send a friend request")
                        .setPosition(cardX + 16.0F, cardY + 14.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(FlatUiPaletteColors.TEXT.getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText("Enter their exact username.")
                        .setPosition(cardX + 16.0F, cardY + 30.0F)
                        .setFont(bodyFont.getMsdfFont())
                        .setFontSize(bodyFont.getSize())
                        .enableShadow()
                        .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
        this.usernameField.setBounds(cardX + 16.0F, cardY + 52.0F, 256.0F, 24.0F);
        this.sendButton.setBounds(cardX + 16.0F, cardY + 84.0F, 256.0F, 23.0F);
        this.usernameField.render(context, mouseX, mouseY);
        this.sendButton.render(context, mouseX, mouseY);
        this.drawStatusMessage(context);
    }

    protected void clickContent(MouseButtonEvent mouseButtonEvent) {
        this.usernameField.click(mouseButtonEvent);
        this.sendButton.click(mouseButtonEvent);
    }

    protected void releaseContent(MouseButtonEvent mouseButtonEvent) {
        this.usernameField.release(mouseButtonEvent);
        this.sendButton.release(mouseButtonEvent);
    }

    protected boolean contentKeyPressed(KeyEvent keyEvent) {
        return this.usernameField.keyPressed(keyEvent);
    }

    protected boolean contentCharTyped(CharacterEvent characterEvent) {
        return this.usernameField.charTyped(characterEvent);
    }

    protected void onDeactivated() {
        this.usernameField.setFocused(false);
    }

    private void sendFriendRequest() {
        if (this.usernameField.getText().isBlank()) {
            this.socialScreen.setStatusMessage("Enter a username first");
            this.usernameField.setFocused(true);
            return;
        }

        String username = this.usernameField.getText().trim();
        astralis.OfflineServices.unavailable("Astralis social services");
        this.socialScreen.setStatusMessage("Sent request to " + username);
        this.usernameField.setText("");
        this.socialScreen.selectTab(FriendRequestsTab.class);
    }
}
