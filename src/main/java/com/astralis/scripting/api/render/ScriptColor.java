package com.astralis.scripting.api.render;

public record ScriptColor(int argb) {
    public ScriptColor(int red, int green, int blue, int alpha) {
        this(
                Math.clamp(alpha, 0, 255) << 24
                        | Math.clamp(red, 0, 255) << 16
                        | Math.clamp(green, 0, 255) << 8
                        | Math.clamp(blue, 0, 255));
    }

    public int alpha() {
        return this.argb >>> 24;
    }

    public int red() {
        return this.argb >>> 16 & 0xFF;
    }

    public int green() {
        return this.argb >>> 8 & 0xFF;
    }

    public int blue() {
        return this.argb & 0xFF;
    }

    public ScriptColor withAlpha(int alpha) {
        return new ScriptColor(this.red(), this.green(), this.blue(), alpha);
    }

    public ScriptColor interpolate(ScriptColor other, double t) {
        double progress = Math.clamp(t, 0.0, 1.0);
        return new ScriptColor(
                (int) Math.round(this.red() + (other.red() - this.red()) * progress),
                (int) Math.round(this.green() + (other.green() - this.green()) * progress),
                (int) Math.round(this.blue() + (other.blue() - this.blue()) * progress),
                (int) Math.round(this.alpha() + (other.alpha() - this.alpha()) * progress));
    }
}
