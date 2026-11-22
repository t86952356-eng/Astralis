package astralis.ui.component;

import astralis.Astralis;
import astralis.social.friends.FriendManager;
import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FriendRequestsTab extends SocialTab {
    private final List<FriendEntry> incomingEntries;
    private final List<FriendEntry> sentEntries;

    public FriendRequestsTab(
            FriendsScreen screen, SocialTextField searchField, FriendChatPanel chatPanel) {
        super(screen, "Requests", true, searchField, chatPanel);
        this.incomingEntries = new ArrayList<>();
        this.sentEntries = new ArrayList<>();
    }

    protected int getBadgeCount() {
        FriendManager friendManager = Astralis.getInstance().getFriendManager();
        return friendManager.getIncomingRequests().size() + friendManager.getSentRequests().size();
    }

    protected void renderContent(Render2DContext context, float mouseX, float mouseY) {
        FriendManager friendManager = Astralis.getInstance().getFriendManager();
        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 9.0F);
        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 7.0F);
        List incomingNames = this.filterBySearch(friendManager.getIncomingRequests());
        List sentNames = this.filterBySearch(friendManager.getSentRequests());
        this.syncEntries(this.incomingEntries, incomingNames, true);
        this.syncEntries(this.sentEntries, sentNames, false);
        this.drawPaneHeader(context, "Requests", titleFont, mouseX, mouseY);
        float sectionHeight;
        if (incomingNames.isEmpty()) {
            sectionHeight = 39.0F;
        } else {
            sectionHeight = incomingNames.size() * 47.0F;
        }

        float incomingSectionHeight = 17.0F + sectionHeight + 25.0F;
        if (sentNames.isEmpty()) {
            sectionHeight = 39.0F;
        } else {
            sectionHeight = sentNames.size() * 47.0F;
        }

        this.setContentHeight(incomingSectionHeight + sectionHeight + 12.0F);
        context.beginScissor(
                this.paneX + 3.0F,
                this.paneY + 28.0F,
                this.getListWidth() - 6.0F,
                this.paneHeight - 32.0F);
        float incomingEndY =
                this.renderSection(
                        context,
                        "Incoming",
                        this.incomingEntries,
                        true,
                        this.paneY + 34.0F + this.scrollOffset,
                        mouseX,
                        mouseY,
                        titleFont,
                        bodyFont);
        this.renderSection(
                context,
                "Sent",
                this.sentEntries,
                false,
                incomingEndY + 8.0F,
                mouseX,
                mouseY,
                titleFont,
                bodyFont);
        context.endScissor();
        this.drawPaneScrollbar(context);
        this.drawStatusMessage(context);
    }

    protected void clickContent(MouseButtonEvent mouseButtonEvent) {
        if (!RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.paneX,
                this.paneY + 28.0F,
                this.getListWidth(),
                this.paneHeight - 32.0F)) {
            return;
        }

        this.incomingEntries.forEach(entry -> entry.click(mouseButtonEvent));
        this.sentEntries.forEach(entry -> entry.click(mouseButtonEvent));
    }

    protected void releaseContent(MouseButtonEvent mouseButtonEvent) {
        this.incomingEntries.forEach(entry -> entry.release(mouseButtonEvent));
        this.sentEntries.forEach(entry -> entry.release(mouseButtonEvent));
    }

    private void syncEntries(List<FriendEntry> entries, List<String> usernames, boolean incoming) {
        entries.removeIf(
                entry ->
                        usernames.stream()
                                .noneMatch(name -> name.equalsIgnoreCase(entry.getUsername())));
        Iterator iterator = usernames.iterator();

        while (iterator.hasNext()) {
            String username = (String) iterator.next();
            if (entries.stream()
                    .anyMatch(existing -> existing.getUsername().equalsIgnoreCase(username))) {
                continue;
            }

            FriendEntry friendEntry;
            label36:
            {
                label35:
                {
                    friendEntry =
                            new FriendEntry(
                                    this.socialScreen,
                                    username,
                                    incoming ? "Wants to add you" : "Request sent",
                                    FlatUiPaletteColors.TEXT_MUTED,
                                    () -> {});
                    if (!incoming) {
                        break label35;
                    }

                    friendEntry.addActionButton(
                            new SocialButton(
                                    this.socialScreen,
                                    "Accept",
                                    52.0F,
                                    12.0F,
                                    8.0F,
                                    SocialButtonStyle.TINTED,
                                    FlatUiPaletteColors.SUCCESS,
                                    () -> {
                                        this.respondToRequest("accept", username);
                                    }));
                    friendEntry.addActionButton(
                            new SocialButton(
                                    this.socialScreen,
                                    "Decline",
                                    50.0F,
                                    12.0F,
                                    8.0F,
                                    SocialButtonStyle.TINTED,
                                    FlatUiPaletteColors.ERROR,
                                    () -> {
                                        this.respondToRequest("decline", username);
                                    }));
                    break label36;
                }

                friendEntry.addActionButton(
                        new SocialButton(
                                this.socialScreen,
                                "Cancel",
                                52.0F,
                                12.0F,
                                8.0F,
                                SocialButtonStyle.TINTED,
                                FlatUiPaletteColors.WARNING,
                                () -> {
                                    this.respondToRequest("cancel", username);
                                }));
            }

            entries.add(friendEntry);
        }

        entries.sort(
                Comparator.comparingInt(
                        entry -> {
                            for (int index = 0; index < usernames.size(); index++)
                                if (usernames.get(index).equalsIgnoreCase(entry.getUsername()))
                                    return index;
                            return usernames.size();
                        }));
    }

    private float renderSection(
            Render2DContext context,
            String title,
            List<FriendEntry> entries,
            boolean incoming,
            float startY,
            float mouseX,
            float mouseY,
            SizedFont titleFont,
            SizedFont bodyFont) {
        float endY;
        float currentY = startY;
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(title)
                        .setPosition(this.paneX + 10.0F, currentY)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        currentY += 17.0F;
        List entryList = entries;
        if (entries.isEmpty()) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.paneX + 7.0F, currentY, this.getListWidth() - 14.0F, 34.0F)
                            .setRadius(7.0F)
                            .setColor(FlatUiPaletteColors.CONTROL));
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(incoming ? "No incoming requests" : "No sent requests")
                            .setPosition(this.paneX + 15.0F, currentY)
                            .centerVertically(34.0F)
                            .setFont(bodyFont.getMsdfFont())
                            .setFontSize(bodyFont.getSize())
                            .enableShadow()
                            .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
            return currentY + 39.0F;
        }

        entryList = entries;

        Iterator iterator = entryList.iterator();

        while (iterator.hasNext()) {
            FriendEntry entry = (FriendEntry) iterator.next();
            entry.setBounds(this.paneX + 7.0F, currentY, this.getListWidth() - 14.0F, 42.0F);
            entry.render(context, mouseX, mouseY);
            endY = currentY + 47.0F;
            currentY = endY;
        }

        endY = currentY;

        return endY;
    }

    private void respondToRequest(String action, String username) {
        FriendsScreen screen;
        int switchValue;
        astralis.OfflineServices.unavailable("Astralis social services");
        screen = this.socialScreen;
        String switchKey = action;
        byte matchedCase = -1;
        switchValue = switchKey.hashCode();
        switch (switchValue) {
            case -1423461112:
                switchValue = switchKey.equals("accept") ? 1 : 0;
                if (switchValue == 0) {
                    break;
                }

                matchedCase = 0;
                break;
            case 1542349558:
                switchValue = switchKey.equals("decline") ? 1 : 0;
                if (switchValue == 0) {
                    break;
                }

                matchedCase = 1;
                break;
            case -1367724422:
                switchValue = switchKey.equals("cancel") ? 1 : 0;
                if (switchValue != 0) {
                    matchedCase = 2;
                }
        }

        switchValue = matchedCase;
        screen.setStatusMessage(
                switch (switchValue) {
                    case 0 -> "Accepted " + username;
                    case 1 -> "Declined " + username;
                    case 2 -> "Canceled " + username;
                    default -> action + " " + username;
                });
        astralis.OfflineServices.unavailable("Astralis social services");
    }
}
