package astralis.social.presence;

import lombok.Generated;

public final class PresenceSnapshotEntry {
    public final int uid;
    public final String backendName;
    public final String minecraftName;
    public final String minecraftUuid;

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
    public String getMinecraftUuid() {
        return this.minecraftUuid;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof PresenceSnapshotEntry other)) {
            return false;
        } else {
            if (this.getUid() != other.getUid()) {
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
                    String thisMinecraftUuid = this.getMinecraftUuid();
                    String otherMinecraftUuid = other.getMinecraftUuid();
                    return thisMinecraftUuid == null
                            ? otherMinecraftUuid == null
                            : thisMinecraftUuid.equals(otherMinecraftUuid);
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
        String backendNameValue = this.getBackendName();
        result = result * 59 + (backendNameValue == null ? 43 : backendNameValue.hashCode());
        String minecraftNameValue = this.getMinecraftName();
        result = result * 59 + (minecraftNameValue == null ? 43 : minecraftNameValue.hashCode());
        String minecraftUuidValue = this.getMinecraftUuid();
        return result * 59 + (minecraftUuidValue == null ? 43 : minecraftUuidValue.hashCode());
    }

    @Generated
    @Override
    public String toString() {
        int uidValue = this.getUid();
        String backendNameValue = this.getBackendName();
        String minecraftUuid = this.getMinecraftUuid();
        String minecraftName = this.getMinecraftName();
        String backendName = backendNameValue;
        int uid = uidValue;
        return "PresenceSnapshotPacket.Entry(uid="
                + uid
                + ", backendName="
                + backendName
                + ", minecraftName="
                + minecraftName
                + ", minecraftUuid="
                + minecraftUuid
                + ")";
    }

    @Generated
    public PresenceSnapshotEntry(
            int uid, String backendName, String minecraftName, String minecraftUuid) {
        this.uid = uid;
        this.backendName = backendName;
        this.minecraftName = minecraftName;
        this.minecraftUuid = minecraftUuid;
    }
}
