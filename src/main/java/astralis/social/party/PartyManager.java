package astralis.social.party;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PartyManager {
    private String partyId;
    private String leaderName;
    private final List<PartyMember> members;
    private final List<PartyMessage> messages;
    private final List<PartyInvite> invites;
    private final List<String> joinRequests;
    private final List<PartyWaypoint> waypoints;
    private final List<FriendParty> friendParties;
    private boolean leader;

    public PartyManager() {

        this.partyId = "";
        this.leaderName = "";
        this.members = new CopyOnWriteArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
        this.invites = new CopyOnWriteArrayList<>();
        this.joinRequests = new CopyOnWriteArrayList<>();
        this.waypoints = new CopyOnWriteArrayList<>();
        this.friendParties = new CopyOnWriteArrayList<>();
    }

    public void sendPartyAction(String action, String target, String payload) {
        astralis.OfflineServices.unavailable("Astralis social services");
    }

    public void addWaypoint(String name, double x, double y, double z) {

        astralis.OfflineServices.unavailable("Astralis social services");
    }

    public boolean hasMemberWithUuid(UUID uuid) {
        Iterator iterator = this.members.iterator();

        while (iterator.hasNext()) {
            PartyMember member = (PartyMember) iterator.next();
            if (!member.getMinecraftUuid().isBlank()) {
                try {
                    if (UUID.fromString(member.getMinecraftUuid()).equals(uuid)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return false;
    }

    public List<FriendParty> getFriendParties() {
        return this.friendParties;
    }

    public boolean isInParty() {
        return !this.partyId.isBlank();
    }

    public String getLeaderName() {
        return this.leaderName;
    }

    public List<PartyWaypoint> getWaypoints() {
        return this.waypoints;
    }

    public String getPartyId() {
        return this.partyId;
    }

    public boolean isLeader() {
        return this.leader;
    }

    public List<String> getJoinRequests() {
        return this.joinRequests;
    }

    public String getFriendPartyLeader(String friendName) {
        Iterator iterator = this.friendParties.iterator();

        while (iterator.hasNext()) {
            FriendParty friendParty = (FriendParty) iterator.next();
            if (friendParty.getFriendName().equalsIgnoreCase(friendName)) {
                return friendParty.getLeaderName();
            }
        }

        return null;
    }

    public List<PartyMember> getMembers() {
        return this.members;
    }

    public List<PartyInvite> getInvites() {
        return this.invites;
    }

    public List<PartyMessage> getMessages() {
        return this.messages;
    }
}
