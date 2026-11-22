package astralis.ui.hud;

public record PartyActionRegion(
        String action, String target, float x, float y, float width, float height) {
    public String getAction() {
        return this.action;
    }

    public String getTarget() {
        return this.target;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }
}
