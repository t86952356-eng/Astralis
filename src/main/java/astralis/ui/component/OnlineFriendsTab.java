package astralis.ui.component;

import astralis.Astralis;
import astralis.social.friends.FriendManager;
import astralis.ui.screen.FriendsScreen;

import java.util.List;

public class OnlineFriendsTab extends FriendsTab {
    public OnlineFriendsTab(
            FriendsScreen screen, SocialTextField searchField, FriendChatPanel chatPanel) {

        super(screen, "Online", searchField, chatPanel);
    }

    @Override
    protected int getBadgeCount() {
        return Astralis.getInstance().getFriendManager().getOnlineFriends().size();
    }

    @Override
    protected List<String> getFriendNames(FriendManager friendManager) {
        return friendManager.getOnlineFriends();
    }

    @Override
    protected String getHeaderTitle() {

        return "Online Friends";
    }

    @Override
    protected String getEmptyMessage() {

        return "No online friends";
    }
}
