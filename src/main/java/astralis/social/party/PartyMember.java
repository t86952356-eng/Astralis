package astralis.social.party;

import lombok.Generated;

public final class PartyMember {
    public final int uid;
    public final int color;
    public final String backendName;
    public final String minecraftName;
    public final String minecraftUuid;
    public final boolean online;
    public final boolean leader;

    @Generated
    public int getUid() {
        return this.uid;
    }

    @Generated
    public int getColor() {
        return this.color;
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
    public boolean isOnline() {
        return this.online;
    }

    @Generated
    public boolean isLeader() {
        return this.leader;
    }

    @Generated
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof PartyMember other)) {
            return false;
        } else {
            if (this.getUid() != other.getUid()) {
                return false;
            }

            if (this.getColor() != other.getColor()) {
                return false;
            }

            if (this.isOnline() != other.isOnline()) {
                return false;
            }

            if (this.isLeader() != other.isLeader()) {
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
        result = result * 59 + this.getColor();
        result = result * 59 + (this.isOnline() ? 79 : 97);
        result = result * 59 + (this.isLeader() ? 79 : 97);
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
        int colorValue = this.getColor();
        String backendNameValue = this.getBackendName();
        String minecraftNameValue = this.getMinecraftName();
        String minecraftUuidValue = this.getMinecraftUuid();
        boolean leader = this.isLeader();
        boolean online = this.isOnline();
        String minecraftUuid = minecraftUuidValue;
        String minecraftName = minecraftNameValue;
        String backendName = backendNameValue;
        int color = colorValue;
        int uid = uidValue;
        return "PartyStatePacket.Member(uid="
                + uid
                + ", color="
                + color
                + ", backendName="
                + backendName
                + ", minecraftName="
                + minecraftName
                + ", minecraftUuid="
                + minecraftUuid
                + ", online="
                + online
                + ", leader="
                + leader
                + ")";
    }

    @Generated
    public PartyMember(
            int uid,
            int color,
            String backendName,
            String minecraftName,
            String minecraftUuid,
            boolean online,
            boolean leader) {
        this.uid = uid;
        this.color = color;
        this.backendName = backendName;
        this.minecraftName = minecraftName;
        this.minecraftUuid = minecraftUuid;
        this.online = online;
        this.leader = leader;
    }
}
