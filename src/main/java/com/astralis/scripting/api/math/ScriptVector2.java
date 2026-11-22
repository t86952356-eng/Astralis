package com.astralis.scripting.api.math;

public record ScriptVector2(double x, double y) {
    public ScriptVector2(double x, double y) {
        if (Double.isFinite(x) && Double.isFinite(y)) {
            this.x = x;
            this.y = y;
        } else {
            throw new IllegalArgumentException("Vector coordinates must be finite.");
        }
    }

    public ScriptVector2 add(ScriptVector2 other) {
        return new ScriptVector2(this.x + other.x, this.y + other.y);
    }

    public ScriptVector2 subtract(ScriptVector2 other) {
        return new ScriptVector2(this.x - other.x, this.y - other.y);
    }

    public ScriptVector2 scale(double factor) {
        return new ScriptVector2(this.x * factor, this.y * factor);
    }

    public double length() {
        return Math.hypot(this.x, this.y);
    }

    public double distanceTo(ScriptVector2 other) {
        return this.subtract(other).length();
    }

    public ScriptVector2 normalize() {
        double magnitude = this.length();
        return magnitude > 1.0E-9 ? this.scale(1.0 / magnitude) : new ScriptVector2(0.0, 0.0);
    }
}
