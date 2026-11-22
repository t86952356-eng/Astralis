package com.astralis.scripting.api.math;

public record ScriptVector3(double x, double y, double z) {
    public ScriptVector3(double x, double y, double z) {
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            this.x = x;
            this.y = y;
            this.z = z;
        } else {
            throw new IllegalArgumentException("Vector coordinates must be finite.");
        }
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public ScriptVector3 scale(double factor) {
        return new ScriptVector3(this.x * factor, this.y * factor, this.z * factor);
    }

    public ScriptVector3 add(ScriptVector3 other) {
        return new ScriptVector3(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public ScriptVector3 offset(double offsetX, double offsetY, double offsetZ) {
        return new ScriptVector3(this.x + offsetX, this.y + offsetY, this.z + offsetZ);
    }

    public double dot(ScriptVector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public ScriptVector3 normalize() {
        double magnitude = this.length();
        return magnitude > 1.0E-9 ? this.scale(1.0 / magnitude) : new ScriptVector3(0.0, 0.0, 0.0);
    }

    public ScriptVector3 subtract(ScriptVector3 other) {
        return new ScriptVector3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public double distanceToSquared(ScriptVector3 other) {
        ScriptVector3 difference = this.subtract(other);
        return difference.x * difference.x
                + difference.y * difference.y
                + difference.z * difference.z;
    }

    public double distanceToSq(ScriptVector3 other) {
        return this.distanceToSquared(other);
    }

    public ScriptVector3 translate(double offsetX, double offsetY, double offsetZ) {
        return this.offset(offsetX, offsetY, offsetZ);
    }

    public double horizontalLength() {
        return Math.hypot(this.x, this.z);
    }

    public double distanceTo(ScriptVector3 other) {
        return this.subtract(other).length();
    }

    public ScriptVector3 cross(ScriptVector3 other) {
        return new ScriptVector3(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x);
    }
}
