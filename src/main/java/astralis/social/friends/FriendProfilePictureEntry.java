package astralis.social.friends;

import lombok.Generated;

import java.util.Arrays;

public final class FriendProfilePictureEntry {
    private final String friendName;
    private final byte[] profilePicture;

    @Generated
    public FriendProfilePictureEntry(String friendName, byte[] profilePicture) {
        this.friendName = friendName;
        this.profilePicture = profilePicture;
    }

    @Generated
    public String getFriendName() {
        return this.friendName;
    }

    @Generated
    public byte[] getProfilePicture() {
        return this.profilePicture;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof FriendProfilePictureEntry other)) {
            return false;
        } else {
            String thisFriendName = this.getFriendName();
            String otherFriendName = other.getFriendName();
            return (thisFriendName == null
                            ? otherFriendName == null
                            : thisFriendName.equals(otherFriendName))
                    ? Arrays.equals(this.getProfilePicture(), other.getProfilePicture())
                    : false;
        }
    }

    @Generated
    @Override
    public int hashCode() {

        int result;
        String friendNameValue = this.getFriendName();

        int nameHash;
        if (friendNameValue == null) {

            nameHash = 43;
        } else {
            nameHash = friendNameValue.hashCode();
        }

        result = 59 + nameHash;

        return result * 59 + Arrays.hashCode(this.getProfilePicture());
    }

    @Generated
    @Override
    public String toString() {
        String friendNameText = this.getFriendName();
        String pictureText = Arrays.toString(this.getProfilePicture());

        return "FriendProfilePicturesPacket.Entry(friendName="
                + friendNameText
                + ", pfp="
                + pictureText
                + ")";
    }
}
