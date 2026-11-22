package astralis.ui.hud;

public record PartyHeadRegion(String playerId, float x, float y, float size) {
    public String getPlayerId() {
        return this.playerId;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getSize() {
        return this.size;
    }
}
