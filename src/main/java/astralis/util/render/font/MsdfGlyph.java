package astralis.util.render.font;

public final class MsdfGlyph {
    private final float advance;
    private final float planeLeft;
    private final float planeTop;
    private final float planeWidth;
    private final float planeHeight;
    private final float uLeft;
    private final float uRight;
    private final float vTop;
    private final float vBottom;
    private final boolean renderable;

    public MsdfGlyph(MsdfGlyphData data, float atlasWidth, float atlasHeight) {

        this.advance = data.getAdvance();
        MsdfBounds planeBounds = data.getPlaneBounds();
        if (planeBounds != null) {
            this.planeLeft = planeBounds.getLeft();
            this.planeTop = planeBounds.getTop();
            this.planeWidth = planeBounds.getRight() - planeBounds.getLeft();
            this.planeHeight = planeBounds.getTop() - planeBounds.getBottom();
            this.renderable = true;
        } else {
            this.planeLeft = this.planeTop = this.planeWidth = this.planeHeight = 0.0F;
            this.renderable = false;
        }

        MsdfBounds atlasBounds = data.getAtlasBounds();
        if (atlasBounds != null) {
            this.uLeft = atlasBounds.getLeft() / atlasWidth;
            this.uRight = atlasBounds.getRight() / atlasWidth;
            this.vTop = 1.0F - atlasBounds.getTop() / atlasHeight;
            this.vBottom = 1.0F - atlasBounds.getBottom() / atlasHeight;
        } else {
            this.uLeft = this.uRight = this.vTop = this.vBottom = 0.0F;
        }
    }

    public float getURight() {
        return this.uRight;
    }

    public float getVBottom() {
        return this.vBottom;
    }

    public float getPlaneHeight() {
        return this.planeHeight;
    }

    public float getULeft() {
        return this.uLeft;
    }

    public float getPlaneLeft() {
        return this.planeLeft;
    }

    public boolean isRenderable() {
        return this.renderable;
    }

    public float getPlaneWidth() {
        return this.planeWidth;
    }

    public float getPlaneTop() {
        return this.planeTop;
    }

    public float getVTop() {
        return this.vTop;
    }

    public float getAdvance() {
        return this.advance;
    }
}
