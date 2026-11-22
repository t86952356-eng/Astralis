package com.astralis.scripting.api.math;

public record ScriptBoundingBox(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public ScriptBoundingBox(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!Double.isFinite(minX)
                || !Double.isFinite(minY)
                || !Double.isFinite(minZ)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)
                || !Double.isFinite(maxZ)) {
            throw new IllegalArgumentException("Bounding-box coordinates must be finite.");
        }

        if (!(minX > maxX) && !(minY > maxY) && !(minZ > maxZ)) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        } else {
            throw new IllegalArgumentException("Bounding-box minimums cannot exceed maximums.");
        }
    }

    public ScriptVector3 center() {
        return new ScriptVector3(
                (this.minX + this.maxX) * 0.5,
                (this.minY + this.maxY) * 0.5,
                (this.minZ + this.maxZ) * 0.5);
    }

    public ScriptVector3 size() {
        return new ScriptVector3(
                this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
    }

    public boolean contains(ScriptVector3 point) {
        return point.x() >= this.minX
                && point.x() <= this.maxX
                && point.y() >= this.minY
                && point.y() <= this.maxY
                && point.z() >= this.minZ
                && point.z() <= this.maxZ;
    }

    public boolean intersects(ScriptBoundingBox other) {
        return this.maxX >= other.minX
                && this.minX <= other.maxX
                && this.maxY >= other.minY
                && this.minY <= other.maxY
                && this.maxZ >= other.minZ
                && this.minZ <= other.maxZ;
    }

    public ScriptBoundingBox expand(double amount) {
        if (Double.isFinite(amount) && !(amount < 0.0)) {
            return new ScriptBoundingBox(
                    this.minX - amount,
                    this.minY - amount,
                    this.minZ - amount,
                    this.maxX + amount,
                    this.maxY + amount,
                    this.maxZ + amount);
        } else {
            throw new IllegalArgumentException("Expansion must be finite and non-negative.");
        }
    }
}
