package astralis.social.chat;

public final class IrcHistoryEntry {
    private final int senderId;
    private final String senderName;
    private final String message;
    private final String sentAt;

    public IrcHistoryEntry(int senderId, String senderName, String message, String sentAt) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.sentAt = sentAt;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof IrcHistoryEntry other)) {
            return false;
        } else {
            if (this.getSenderId() != other.getSenderId()) {
                return false;
            }

            String thisSenderName = this.getSenderName();
            String otherSenderName = other.getSenderName();
            if (thisSenderName == null
                    ? otherSenderName == null
                    : thisSenderName.equals(otherSenderName)) {
                String thisMessage = this.getMessage();
                String otherMessage = other.getMessage();
                if (thisMessage == null ? otherMessage == null : thisMessage.equals(otherMessage)) {
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
        }
    }

    @Override
    public String toString() {

        int rawSenderId = this.getSenderId();
        String rawSenderName = this.getSenderName();
        String sentAtValue = this.getSentAt();
        String messageValue = this.getMessage();
        String senderNameValue = rawSenderName;
        int senderIdValue = rawSenderId;

        return "IrcHistoryPacket.Entry(senderId="
                + senderIdValue
                + ", senderName="
                + senderNameValue
                + ", message="
                + messageValue
                + ", sentAt="
                + sentAtValue
                + ")";
    }

    @Override
    public int hashCode() {

        int result;

        result = 59 + this.getSenderId();
        String senderNameValue = this.getSenderName();

        int nameBase = result * 59;
        int nameHash;
        if (senderNameValue == null) {

            nameHash = 43;
        } else {
            nameHash = senderNameValue.hashCode();
        }

        result = nameBase + nameHash;
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

    public String getMessage() {
        return this.message;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public String getSentAt() {
        return this.sentAt;
    }

    public int getSenderId() {
        return this.senderId;
    }
}
