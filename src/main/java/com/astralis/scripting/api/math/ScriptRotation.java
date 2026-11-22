package com.astralis.scripting.api.math;

public record ScriptRotation(float yaw, float pitch) {
    public ScriptRotation(float yaw, float pitch) {
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            pitch = Math.clamp(pitch, -90.0F, 90.0F);
            this.yaw = yaw;
            this.pitch = pitch;
        } else {
            throw new IllegalArgumentException("Rotation coordinates must be finite.");
        }
    }

    public ScriptRotation add(float yawDelta, float pitchDelta) {
        return new ScriptRotation(this.yaw + yawDelta, this.pitch + pitchDelta);
    }

    public ScriptRotation interpolate(ScriptRotation other, double t) {
        double progress = Math.clamp(t, 0.0, 1.0);
        return new ScriptRotation(
                this.yaw + wrapDegrees(other.yaw - this.yaw) * (float) progress,
                this.pitch + (other.pitch - this.pitch) * (float) progress);
    }

    public float yawDifference(ScriptRotation other) {
        return wrapDegrees(other.yaw - this.yaw);
    }

    public float pitchDifference(ScriptRotation other) {
        return other.pitch - this.pitch;
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }

        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }

        return wrapped;
    }
}
