package astralis.ui.component;

import astralis.Astralis;
import astralis.social.friends.FriendManager;
import astralis.social.party.PartyManager;
import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.font.SizedFont;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FriendsTab extends SocialTab {
    private final List<FriendEntry> entries = new ArrayList<>();

    public FriendsTab(
            FriendsScreen screen,
            String title,
            SocialTextField searchField,
            FriendChatPanel chatPanel) {
        super(screen, title, true, searchField, chatPanel);
    }

    @Override
    protected int getBadgeCount() {
        return Astralis.getInstance().getFriendManager().getFriends().size();
    }

    @Override
    protected void renderContent(Render2DContext context, float mouseX, float mouseY) {

        FriendManager friendManager = Astralis.getInstance().getFriendManager();

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 9.0F);

        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 7.0F);
        List<String> friendNames = this.filterBySearch(this.getFriendNames(friendManager));
        this.entries.removeIf(
                entry ->
                        friendNames.stream()
                                .noneMatch(name -> name.equalsIgnoreCase(entry.getUsername())));

        for (String friendName : friendNames) {
            if (!this.entries.stream()
                    .anyMatch(existing -> existing.getUsername().equalsIgnoreCase(friendName))) {

                FriendEntry friendEntry =
                        new FriendEntry(
                                this.socialScreen,
                                friendName,
                                "Offline",
                                FlatUiPaletteColors.TEXT_MUTED,
                                () -> this.chatPanel.toggleChat(friendName));

                friendEntry.addActionButton(
                        new SocialButton(
                                this.socialScreen,
                                "Party",
                                42.0F,
                                22.0F,
                                7.0F,
                                SocialButtonStyle.TINTED,
                                FlatUiPaletteColors.SUCCESS,
                                () -> {
                                    String partyLeader =
                                            Astralis.getInstance()
                                                    .getPartyManager()
                                                    .getFriendPartyLeader(friendName);

                                    PartyManager partyManager =
                                            Astralis.getInstance().getPartyManager();
                                    String action;
                                    if (partyLeader == null) {

                                        action = "INVITE";
                                    } else {

                                        action = "REQUEST_JOIN";
                                    }

                                    partyManager.sendPartyAction(action, friendName, "");
                                    String message;
                                    if (partyLeader == null) {

                                        message = "Party invite sent to " + friendName;
                                    } else {

                                        message = "Requested to join " + partyLeader + "'s party";
                                    }

                                    this.socialScreen.setStatusMessage(message);
                                }));
                this.entries.add(friendEntry);
            }
        }

        this.entries.sort(
                Comparator.comparingInt(
                        entry -> {
                            int index = 0;

                            while (index < friendNames.size()) {
                                if (((String) friendNames.get(index))
                                        .equalsIgnoreCase(entry.getUsername())) {
                                    return index;
                                }

                                index++;
                            }

                            return friendNames.size();
                        }));
        this.drawPaneHeader(context, this.getHeaderTitle(), titleFont, mouseX, mouseY);
        this.setContentHeight(this.entries.size() * 47.0F + 8.0F);
        context.beginScissor(
                this.paneX + 3.0F,
                this.paneY + 28.0F,
                this.getListWidth() - 6.0F,
                this.paneHeight - 32.0F);
        float currentY = this.paneY + 34.0F + this.scrollOffset;

        for (FriendEntry entry : this.entries) {
            boolean online =
                    this.containsIgnoreCase(friendManager.getOnlineFriends(), entry.getUsername());
            String statusText;
            if (online) {

                statusText = "Online";
            } else {

                statusText = "Offline";
            }

            entry.setStatusText(statusText);
            entry.setStatusColor(
                    online ? FlatUiPaletteColors.SUCCESS : FlatUiPaletteColors.TEXT_MUTED);
            entry.setSelected(
                    entry.getUsername().equalsIgnoreCase(this.chatPanel.getSelectedFriend()));
            entry.setBounds(this.paneX + 7.0F, currentY, this.getListWidth() - 14.0F, 42.0F);
            entry.render(context, mouseX, mouseY);
            currentY += 47.0F;
        }

        if (this.entries.isEmpty()) {
            this.drawPlaceholderText(
                    context,
                    this.getEmptyMessage(),
                    this.paneX,
                    this.paneY + 28.0F,
                    this.getListWidth(),
                    this.paneHeight - 32.0F,
                    bodyFont);
        }

        context.endScissor();
        this.drawPaneScrollbar(context);
        this.drawStatusMessage(context);
    }

    @Override
    protected void clickContent(MouseButtonEvent mouseButtonEvent) {
        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.paneX,
                this.paneY + 28.0F,
                this.getListWidth(),
                this.paneHeight - 32.0F)) {
            for (FriendEntry entry : this.entries) {
                entry.click(mouseButtonEvent);
            }
        }
    }

    @Override
    protected void releaseContent(MouseButtonEvent mouseButtonEvent) {
        this.entries.forEach(entry -> entry.release(mouseButtonEvent));
    }

    protected List<String> getFriendNames(FriendManager friendManager) {
        return friendManager.getFriends();
    }

    protected String getHeaderTitle() {

        return "Friends";
    }

    protected String getEmptyMessage() {

        return "No friends found";
    }
}
