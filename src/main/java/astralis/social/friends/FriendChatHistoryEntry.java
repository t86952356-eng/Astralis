package astralis.social.friends;

import lombok.Generated;

public final class FriendChatHistoryEntry {
    private final String senderName;
    private final String receiverName;
    private final String message;
    private final String sentAt;

    @Generated
    public FriendChatHistoryEntry(
            String senderName, String receiverName, String message, String sentAt) {
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.message = message;
        this.sentAt = sentAt;
    }

    @Generated
    public String getSenderName() {
        return this.senderName;
    }

    @Generated
    public String getReceiverName() {
        return this.receiverName;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public String getSentAt() {
        return this.sentAt;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof FriendChatHistoryEntry other)) {
            return false;
        } else {
            String thisSenderName = this.getSenderName();
            String otherSenderName = other.getSenderName();
            if (thisSenderName == null
                    ? otherSenderName == null
                    : thisSenderName.equals(otherSenderName)) {
                String thisReceiverName = this.getReceiverName();
                String otherReceiverName = other.getReceiverName();
                if (thisReceiverName == null
                        ? otherReceiverName == null
                        : thisReceiverName.equals(otherReceiverName)) {
                    String thisMessage = this.getMessage();
                    String otherMessage = other.getMessage();
                    if (thisMessage == null
                            ? otherMessage == null
                            : thisMessage.equals(otherMessage)) {
                        String thisSentAt = this.getSentAt();
                        String otherSentAt = other.getSentAt();
                        return thisSentAt == null
                                ? otherSentAt == null
                                : thisSentAt.equals(otherSentAt);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Generated
    @Override
    public int hashCode() {

        int result;
        String senderNameValue = this.getSenderName();

        int senderHash;
        if (senderNameValue == null) {

            senderHash = 43;
        } else {
            senderHash = senderNameValue.hashCode();
        }

        result = 59 + senderHash;
        String receiverNameValue = this.getReceiverName();

        int receiverBase = result * 59;
        int receiverHash;
        if (receiverNameValue == null) {

            receiverHash = 43;
        } else {
            receiverHash = receiverNameValue.hashCode();
        }

        result = receiverBase + receiverHash;
        String messageValue = this.getMessage();

        int messageBase = result * 59;
        int messageHash;
        if (messageValue == null) {

            messageHash = 43;
        } else {
            messageHash = messageValue.hashCode();
        }

        result = messageBase + messageHash;
        String sentAtValue = this.getSentAt();

        int sentAtBase = result * 59;
        int sentAtHash;
        if (sentAtValue == null) {

            sentAtHash = 43;
        } else {
            sentAtHash = sentAtValue.hashCode();
        }

        return sentAtBase + sentAtHash;
    }

    @Generated
    @Override
    public String toString() {

        String rawSenderName = this.getSenderName();
        String rawReceiverName = this.getReceiverName();
        String sentAtValue = this.getSentAt();
        String messageValue = this.getMessage();
        String receiverNameValue = rawReceiverName;
        String senderNameValue = rawSenderName;

        return "FriendChatHistoryPacket.Entry(senderName="
                + senderNameValue
                + ", receiverName="
                + receiverNameValue
                + ", message="
                + messageValue
                + ", sentAt="
                + sentAtValue
                + ")";
    }
}
