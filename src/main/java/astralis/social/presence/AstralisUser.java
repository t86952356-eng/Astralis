package astralis.social.presence;

import lombok.Generated;

import java.util.UUID;

public final class AstralisUser {
    public final int uid;
    public final String backendName;
    public final String minecraftName;
    public final UUID minecraftUuid;
    public final long lastSeenAt;

    @Generated
    public AstralisUser(
            int uid, String backendName, String minecraftName, UUID uuid, long lastSeenAt) {
        this.uid = uid;
        this.backendName = backendName;
        this.minecraftName = minecraftName;
        this.minecraftUuid = uuid;
        this.lastSeenAt = lastSeenAt;
    }

    @Generated
    public int getUid() {
        return this.uid;
    }

    @Generated
    public String getBackendName() {
        return this.backendName;
    }

    @Generated
    public String getMinecraftName() {
        return this.minecraftName;
    }

    @Generated
    public UUID getMinecraftUuid() {
        return this.minecraftUuid;
    }

    @Generated
    public long getLastSeenAt() {
        return this.lastSeenAt;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof AstralisUser other)) {
            return false;
        } else {
            if (this.getUid() != other.getUid()) {
                return false;
            }

            if (this.getLastSeenAt() != other.getLastSeenAt()) {
                return false;
            }

            String thisBackendName = this.getBackendName();
            String otherBackendName = other.getBackendName();
            if (thisBackendName == null
                    ? otherBackendName == null
                    : thisBackendName.equals(otherBackendName)) {
                String thisMinecraftName = this.getMinecraftName();
                String otherMinecraftName = other.getMinecraftName();
                if (thisMinecraftName == null
                        ? otherMinecraftName == null
                        : thisMinecraftName.equals(otherMinecraftName)) {
                    UUID uuid = this.getMinecraftUuid();
                    UUID uuid2 = other.getMinecraftUuid();
                    return uuid == null ? uuid2 == null : uuid.equals(uuid2);
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

        result = 59 + this.getUid();
        long lastSeenAtValue = this.getLastSeenAt();

        int scaledForLastSeenAt = result * 59;

        result = scaledForLastSeenAt + (int) (lastSeenAtValue >>> 32 ^ lastSeenAtValue);
        String backendNameValue = this.getBackendName();

        int scaledForBackendName = result * 59;
        int backendNameHash;
        if (backendNameValue == null) {

            backendNameHash = 43;
        } else {
            backendNameHash = backendNameValue.hashCode();
        }

        result = scaledForBackendName + backendNameHash;
        String minecraftNameValue = this.getMinecraftName();

        int scaledForMinecraftName = result * 59;
        int minecraftNameHash;
        if (minecraftNameValue == null) {

            minecraftNameHash = 43;
        } else {
            minecraftNameHash = minecraftNameValue.hashCode();
        }

        result = scaledForMinecraftName + minecraftNameHash;
        UUID uuid = this.getMinecraftUuid();

        int scaledForMinecraftUuid = result * 59;
        int minecraftUuidHash;
        if (uuid == null) {

            minecraftUuidHash = 43;
        } else {
            minecraftUuidHash = uuid.hashCode();
        }

        return scaledForMinecraftUuid + minecraftUuidHash;
    }

    @Generated
    @Override
    public String toString() {

        int uidValue = this.getUid();
        String backendNameValue = this.getBackendName();
        String minecraftNameValue = this.getMinecraftName();
        String minecraftUuidValue = String.valueOf(this.getMinecraftUuid());
        long lastSeenAt = this.getLastSeenAt();
        String minecraftUuid = minecraftUuidValue;
        String minecraftName = minecraftNameValue;
        String backendName = backendNameValue;
        int uid = uidValue;

        return "PresenceManager.AstralisUser(uid="
                + uid
                + ", backendName="
                + backendName
                + ", minecraftName="
                + minecraftName
                + ", minecraftUuid="
                + minecraftUuid
                + ", lastSeenAt="
                + lastSeenAt
                + ")";
    }
}
