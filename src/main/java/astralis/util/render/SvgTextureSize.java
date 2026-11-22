package astralis.util.render;

public record SvgTextureSize(int width, int height) {
    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
