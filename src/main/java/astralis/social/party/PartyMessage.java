package astralis.social.party;

import lombok.Generated;

public final class PartyMessage {
    public final String senderName;
    public final String minecraftUuid;
    public final String message;
    public final long timestamp;

    @Generated
    public String getSenderName() {
        return this.senderName;
    }

    @Generated
    public String getMinecraftUuid() {
        return this.minecraftUuid;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public long getTimestamp() {
        return this.timestamp;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof PartyMessage other)) {
            return false;
        } else {
            if (this.getTimestamp() != other.getTimestamp()) {
                return false;
            }

            String thisSenderName = this.getSenderName();
            String otherSenderName = other.getSenderName();
            if (thisSenderName == null
                    ? otherSenderName == null
                    : thisSenderName.equals(otherSenderName)) {
                String thisMinecraftUuid = this.getMinecraftUuid();
                String otherMinecraftUuid = other.getMinecraftUuid();
                if (thisMinecraftUuid == null
                        ? otherMinecraftUuid == null
                        : thisMinecraftUuid.equals(otherMinecraftUuid)) {
                    String thisMessage = this.getMessage();
                    String otherMessage = other.getMessage();
                    return thisMessage == null
                            ? otherMessage == null
                            : thisMessage.equals(otherMessage);
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
        int result = 1;
        long timestampValue = this.getTimestamp();
        result = result * 59 + (int) (timestampValue >>> 32 ^ timestampValue);
        String senderNameValue = this.getSenderName();
        result = result * 59 + (senderNameValue == null ? 43 : senderNameValue.hashCode());
        String minecraftUuidValue = this.getMinecraftUuid();
        result = result * 59 + (minecraftUuidValue == null ? 43 : minecraftUuidValue.hashCode());
        String messageValue = this.getMessage();
        return result * 59 + (messageValue == null ? 43 : messageValue.hashCode());
    }

    @Generated
    @Override
    public String toString() {
        String senderNameValue = this.getSenderName();
        String minecraftUuidValue = this.getMinecraftUuid();
        long timestamp = this.getTimestamp();
        String message = this.getMessage();
        String minecraftUuid = minecraftUuidValue;
        String senderName = senderNameValue;
        return "PartyStatePacket.Message(senderName="
                + senderName
                + ", minecraftUuid="
                + minecraftUuid
                + ", message="
                + message
                + ", timestamp="
                + timestamp
                + ")";
    }

    @Generated
    public PartyMessage(String senderName, String minecraftUuid, String message, long timestamp) {
        this.senderName = senderName;
        this.minecraftUuid = minecraftUuid;
        this.message = message;
        this.timestamp = timestamp;
    }
}
