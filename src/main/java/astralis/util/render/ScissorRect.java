package astralis.util.render;

public record ScissorRect(int x, int y, int width, int height) {
    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getX() {
        return this.x;
    }
}
