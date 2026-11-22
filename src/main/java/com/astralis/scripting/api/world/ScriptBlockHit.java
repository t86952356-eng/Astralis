package com.astralis.scripting.api.world;

public record ScriptBlockHit(
        int supportX,
        int supportY,
        int supportZ,
        BlockFace face,
        double hitX,
        double hitY,
        double hitZ) {
    public ScriptBlockHit(
            int supportX,
            int supportY,
            int supportZ,
            BlockFace face,
            double hitX,
            double hitY,
            double hitZ) {
        if (face == null) {
            throw new IllegalArgumentException("Block face cannot be null.");
        }

        if (Double.isFinite(hitX) && Double.isFinite(hitY) && Double.isFinite(hitZ)) {
            this.supportX = supportX;
            this.supportY = supportY;
            this.supportZ = supportZ;
            this.face = face;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
        } else {
            throw new IllegalArgumentException("Hit coordinates must be finite.");
        }
    }

    public int targetX() {
        return this.supportX + this.face.getOffsetX();
    }

    public int targetY() {
        return this.supportY + this.face.getOffsetY();
    }

    public int targetZ() {
        return this.supportZ + this.face.getOffsetZ();
    }
}
