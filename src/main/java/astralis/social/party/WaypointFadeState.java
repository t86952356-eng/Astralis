package astralis.social.party;

public class WaypointFadeState {
    public final PartyWaypoint waypoint;
    public float fade;
    public long lastUpdateTime = System.currentTimeMillis();
    public boolean active = true;

    public WaypointFadeState(PartyWaypoint waypoint) {
        this.waypoint = waypoint;
    }
}
