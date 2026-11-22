package astralis.social.party;

import lombok.Generated;

public final class FriendParty {
    public final String friendName;
    public final String leaderName;

    @Generated
    public String getFriendName() {
        return this.friendName;
    }

    @Generated
    public String getLeaderName() {
        return this.leaderName;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof FriendParty other)) {
            return false;
        } else {
            String thisFriendName = this.getFriendName();
            String otherFriendName = other.getFriendName();
            if (thisFriendName == null
                    ? otherFriendName == null
                    : thisFriendName.equals(otherFriendName)) {
                String thisLeaderName = this.getLeaderName();
                String otherLeaderName = other.getLeaderName();
                return thisLeaderName == null
                        ? otherLeaderName == null
                        : thisLeaderName.equals(otherLeaderName);
            } else {
                return false;
            }
        }
    }

    @Generated
    @Override
    public int hashCode() {
        int result = 1;
        String friendNameValue = this.getFriendName();
        result = result * 59 + (friendNameValue == null ? 43 : friendNameValue.hashCode());
        String leaderNameValue = this.getLeaderName();
        return result * 59 + (leaderNameValue == null ? 43 : leaderNameValue.hashCode());
    }

    @Generated
    @Override
    public String toString() {
        return "PartyStatePacket.FriendParty(friendName="
                + this.getFriendName()
                + ", leaderName="
                + this.getLeaderName()
                + ")";
    }

    @Generated
    public FriendParty(String friendName, String leaderName) {
        this.friendName = friendName;
        this.leaderName = leaderName;
    }
}
