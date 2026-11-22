package astralis.social.party;

import lombok.Generated;

public final class PartyInvite {
    public final String partyId;
    public final String leaderName;

    @Generated
    public String getPartyId() {
        return this.partyId;
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
        } else if (!(object instanceof PartyInvite other)) {
            return false;
        } else {
            String thisPartyId = this.getPartyId();
            String otherPartyId = other.getPartyId();
            if (thisPartyId == null ? otherPartyId == null : thisPartyId.equals(otherPartyId)) {
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

        int result;
        String partyIdValue = this.getPartyId();

        int partyIdHash;
        if (partyIdValue == null) {

            partyIdHash = 43;
        } else {
            partyIdHash = partyIdValue.hashCode();
        }

        result = 59 + partyIdHash;
        String leaderNameValue = this.getLeaderName();

        int scaledForLeaderName = result * 59;
        int leaderNameHash;
        if (leaderNameValue == null) {

            leaderNameHash = 43;
        } else {
            leaderNameHash = leaderNameValue.hashCode();
        }

        return scaledForLeaderName + leaderNameHash;
    }

    @Generated
    @Override
    public String toString() {
        String partyId = this.getPartyId();
        String leaderName = this.getLeaderName();

        return "PartyStatePacket.Invite(partyId=" + partyId + ", leaderName=" + leaderName + ")";
    }

    @Generated
    public PartyInvite(String partyId, String leaderName) {
        this.partyId = partyId;
        this.leaderName = leaderName;
    }
}
